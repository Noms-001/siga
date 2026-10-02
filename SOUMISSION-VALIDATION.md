# Soumission d'une activité à la validation

Ce document explique comment une activité passe de `BROUILLON` à `EN_ATTENTE_VALIDATION`,
ce qui se passe dans la base à cet instant, et ce que l'interface en fait.

Il décrit la chaîne complète, du clic sur « Soumettre » dans `Brouillons.vue` jusqu'aux
deux lignes écrites en base.

---

## 1. Le point de départ : l'activité n'a pas de statut

La table `activite` ne porte **aucune colonne de statut**. Ce qui existe, c'est une table
`historique_activite` qui enregistre chaque changement d'état.

Le statut courant d'une activité est donc, par définition :

```sql
SELECT sta.code
FROM historique_activite h
JOIN statut sta ON sta.id_statut = h.id_statut
WHERE h.id_activite = :id
ORDER BY h.date_changement DESC, h.id_historique_activite DESC
LIMIT 1
```

Deux conséquences directes :

- **Changer le statut, c'est ajouter une ligne.** Pas un `UPDATE` sur `activite`.
- L'identifiant de la ligne d'historique sert de second critère de tri, parce que deux
  changements peuvent partager la même date au ticks près.

Cette requête est celle de `ActiviteRepository.findStatutCourant`, relue par la page des
brouillons, celle des validateurs, et le contrôle de la soumission.

---

## 2. Le circuit de validation : la seconde table

L'historique dit dans quel état l'activité est. Il ne dit pas *qui* doit la valider.

Cette information vit dans `validation_activite` :

| colonne | rôle |
| --- | --- |
| `decision` | enum `EN_ATTENTE_VALIDATION`, `VALIDE`, `REJETE`, `RETOUR_MODIFICATION` |
| `id_activite` | l'activité examinée |
| `id_etape_validation` | l'étape du circuit atteinte |
| `id_demandeur` | qui a soumis |
| `id_decideur` | qui a tranché |
| `date_demande` / `date_decision` | les deux instants |

Une contrainte de table garantit la cohérence temporelle :

```sql
CONSTRAINT chk_validation_activite_decision_date CHECK (
    (decision = 'EN_ATTENTE_VALIDATION' AND date_decision IS NULL)
    OR
    (decision IN ('VALIDE', 'REJETE', 'RETOUR_MODIFICATION') AND date_decision IS NOT NULL)
)
```

Autrement dit : **une décision sans date est impossible, et une date sans décision
n'existe pas.** C'est cette contrainte qui permet de laisser `date_decision` à sa valeur
par défaut à l'ouverture, sans avoir à gérer le cas dans le code.

### Le point qui a demandé une décision

Ces deux tables peuvent diverger : une activité peut être `EN_ATTENTE_VALIDATION` dans
l'historique sans aucune demande correspondante. Conséquence concrète : la page des
validateurs ne la verrait pas, et une décision prise dessus échouerait faute de demande.

La soumission écrit donc dans **les deux tables**, dans la même transaction. C'est la ligne
`ouvrirDemandeValidation` qui rend les deux modèles cohérents.

---

## 3. Le déclenchement depuis l'interface

Dans `frontend/src/views/frontoffice/activites/Brouillons.vue`, la soumission passe par
trois étapes, et non par un appel direct :

1. **Confirmation** — `useConfirmation.demander()` pose une modale qui explique la conséquence.
2. **Appel** — seulement après confirmation, `soumettreReellement()` appelle le service.
3. **Retrait local de la card** — pas de rechargement de la page.

```ts
confirmation.demander({
    titre: 'Soumettre à la validation',
    message: `L'activité ${item.code} et son contenu seront figés et transmis à validation.`,
    consequence: "Une fois soumise, elle ne sera plus modifiable depuis cette page.",
    libelleConfirmer: 'Soumettre',
    ton: 'warning',
    action: () => soumettreReellement(item),
})
```

Le service côté interface est délibérément minimal :

```ts
export async function soumettreValidation(id: number) {
    return post<SoumissionResultat>(`/activites/${id}/soumettre-validation`, null)
}
```

**Pas de corps.** Ni l'identifiant ni le statut ne viennent du client : l'un est dans le
chemin, l'autre est décidé par le serveur. Un statut fourni par l'appelant rendrait la
transition arbitraire.

---

## 4. L'endpoint

```java
@PostMapping("/{idActivite}/soumettre-validation")
public ResponseEntity<ApiResponse<ActiviteSoumissionDTO>> soumettreValidation(
        @PathVariable Integer idActivite) { ... }
```

`200` et non `201` : aucune ressource n'est créée, une activité existante change d'état.

| code | situation |
| --- | --- |
| `200` | soumission enregistrée |
| `400` | identifiant mal formé |
| `403` | activité hors périmètre |
| `404` | activité inexistante |
| `409` | l'activité n'est plus un brouillon |

Le `409` n'est pas une panne : c'est le cas de deux onglets ouverts sur la même activité.
La page retire alors la card, puisqu'elle a quitté les brouillons de toute façon.

---

## 5. Le service : cinq règles, dans cet ordre

`ActiviteSoumissionService.soumettre()` est la seule autorité sur cette transition.

**a. Identifiant valide.** Un `null` ou un négatif est une erreur du client, pas un conflit.

**b. Périmètre.** `activiteService.getPerimetre()` déduit ce que l'utilisateur a le droit de
voir. Un utilisateur rattaché à un service ne voit que son service ; un chef de département
voit tout son département. La vérification passe par les données, **jamais par un paramètre
HTTP**.

**c. Verrou de ligne, pris avant la relecture du statut.**

```java
Activite verrouillee = activiteRepository.verrouillerActivite(idActivite)
        .orElseThrow(() -> activiteService.refusOuAbsence(idActivite));
```

L'ordre compte. Relire le statut *puis* verrouiller laisserait une fenêtre où deux
soumissions simultanées lisent toutes deux `BROUILLON` et déposent chacune leur ligne
`EN_ATTENTE_VALIDATION`. En verrouillant d'abord, la seconde attend, relit, et tombe sur le
conflit.

**d. Contrôle de l'état.** Seul `BROUILLON` devient `EN_ATTENTE_VALIDATION`. Toute autre
valeur donne le `409`.

**e. Aucune validation métier.** Le service ne vérifie ni le contenu, ni la présence de
sous-activités, ni les dates. Sa seule règle est la transition d'état. Les règles de
complétude éventuelles apparaîtront ici, et **nowhere else** — une règle écrite à deux
endroits diverge toujours un jour.

---

## 6. Les deux écritures

### 6.1 L'état

```sql
INSERT INTO historique_activite
    (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
VALUES (
    :commentaire,
    GREATEST(
        CURRENT_TIMESTAMP,
        COALESCE(
            (SELECT max(h.date_changement) FROM historique_activite h
             WHERE h.id_activite = :idActivite),
            CURRENT_TIMESTAMP) + make_interval(secs => 1)),
    :idUtilisateur,
    (SELECT id_statut FROM statut WHERE code = :codeStatut),
    :idActivite)
```

Le `GREATEST` mérite un mot. La soumission d'une activité qui n'a jamais changé d'état
reviendrait à écrire une date antérieure à celle de sa création — et l'historique
s'afficherait alors dans le désordre. On garantit donc que **tout changement est
strictement postérieur au précédent**, en repoussant d'une seconde si nécessaire.

### 6.2 La demande d'examen

```sql
INSERT INTO validation_activite
    (commentaire, date_demande, id_activite, id_etape_validation,
     id_demandeur, id_decideur)
SELECT ...
WHERE NOT EXISTS (
    SELECT 1 FROM validation_activite v
    WHERE v.id_activite = :idActivite
      AND v.decision = 'EN_ATTENTE_VALIDATION')
```

Trois détails ne sont pas visibles dans la requête :

**L'étape est la première étape active.** `findPremiereEtapeActive()` retourne
`min(id_etape_validation)` parmi les étapes actives de la procédure — le début du circuit,
donc le seul choix qui ne suppose pas qu'une autre activité ait déjà été examinée. Si aucune
étape n'est active, la méthode renvoie `null` et **la soumission n'est pas annulée pour
autant** : le statut reste correct, et il n'y a simplement personne à qui demander.

**Une seule demande ouverte à la fois.** Le `WHERE NOT EXISTS` empêche d'empiler deux
examens en attente sur la même activité. Ce n'était pas décoratif : les tests ont montré
qu'une activité déjà examinée et resoumise en recevait une seconde. Le statut peut changer
au passage — c'est la demande existante qui est reprise, pas dupliquée.

**`id_decideur` vaut l'auteur, provisoirement.** La colonne est `NOT NULL` dès la création,
alors qu'une demande en attente n'a par définition personne. Le schéma l'impose ; ce n'est
pas un choix fonctionnel. `id_decideur` est écrasé au moment de la décision.

---

## 7. L'idempotence par le conflit

Une seconde soumission du même brouillon ne renvoie pas un succès : elle échoue en `409`.

C'est délibéré. Rendre l'appel idempotent — répondre `200` la deuxième fois — masquerait
à l'appelant que l'activité a déjà quitté les brouillons, et la page afficherait « soumise »
pour une opération qui n'a rien fait.

---

## 8. Après la soumission

L'activité apparaît alors dans la page `AValider.vue` via
`GET /api/activites?statut=EN_ATTENTE_VALIDATION`, et peut être tranchée par
`POST /api/activites/{id}/decision`.

| décision | statut obtenu |
| --- | --- |
| `VALIDE` | `VALIDEE` — publiée au suivi |
| `REJETE` | `REJETE` — sortie du circuit |
| `RETOUR_MODIFICATION` | `BROUILLON` — reprise par l'auteur |

Le refus et le retour exigent un motif. En frontend, il est tronqué à 1000 caractères au
moment de la saisie, parce que `BaseInput` ne sait pas poser de `maxlength` sur sa
textarea ; côté serveur, `@Size(max = 1000)` le refuse.

Un détail propre au type `decision` : c'est un **enum** PostgreSQL, pas une colonne texte.
Un paramètre Java arrive en `varchar` et PostgreSQL refuse l'affectation. L'`UPDATE` porte
donc un `CAST(:decision AS decision_validation)` — sans lui, la première décision renvoyait
un `500`.

---

## 9. Ce que prouvent les tests

`ActiviteSoumissionIntegrationTests` (12 tests) travaille sur une vraie base, dans une
transaction annulée en fin de test. L'état de départ est fabriqué par le test lui-même,
plutôt que supposé.

| ce qui est vérifié | pourquoi |
| --- | --- |
| la ligne d'historique porte le statut et l'auteur | le changement d'état est bien *tracé* |
| la demande d'examen est ouverte en même temps | les deux tables restent cohérentes |
| une seconde soumission donne `409` | pas de double ligne pour un même brouillon |
| hors périmètre donne `403` | le périmètre n'est pas contournable |
| un lot de trois donne trois insertions | le comportement de lot est bien N requêtes |
| un échec au milieu n'annule pas les précédentes | chaque requête est sa propre transaction |

Ce que ces tests **ne** prouvent pas : que la page appelle l'endpoint. Une page qui
n'appellerait rien passerait la totalité de cette suite.

---

## 10. Fichiers concernés

**Backend**
- `backend/src/main/java/mg/bank/backend/controller/ActiviteController.java`
- `backend/src/main/java/mg/bank/backend/service/ActiviteSoumissionService.java`
- `backend/src/main/java/mg/bank/backend/service/ActiviteDecisionService.java`
- `backend/src/main/java/mg/bank/backend/repository/ActiviteRepository.java`
- `backend/src/main/java/mg/bank/backend/dto/ActiviteSoumissionDTO.java`
- `backend/src/test/java/mg/bank/backend/ActiviteSoumissionIntegrationTests.java`
- `backend/src/test/java/mg/bank/backend/ActiviteDecisionIntegrationTests.java`

**Base de données**
- `database/script/schema.sql` — `historique_activite`, `validation_activite`
- `database/script/data.sql` — jeu de démonstration

**Frontend**
- `frontend/src/views/frontoffice/activites/Brouillons.vue`
- `frontend/src/views/frontoffice/activites/AValider.vue`
- `frontend/src/components/activite/RejetActiviteModal.vue`
- `frontend/src/services/activite.ts`
- `frontend/src/composables/useConfirmation.ts`
