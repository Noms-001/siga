# Liste des activités — fonctionnement et choix de conception

Ce document explique ce que fait la page `Liste.vue`, et **pourquoi** elle est
construite ainsi. Il s'adresse à quelqu'un qui doit la maintenir ou la
reprendre, pas à un utilisateur de l'application.

Fichier principal : `frontend/src/views/frontoffice/activites/Liste.vue`

---

## 1. Le principe directeur

> **Le backend filtre, trie, pagine et calcule. Le front affiche.**

La page ne fait aucun `slice()` sur les données, aucun tri local, aucun calcul
d'avancement. Chaque état de l'interface correspond à une requête, et la
réponse du backend fait autorité.

La raison est simple : une liste de 100 activités paginée par 10 ne contient
que 10 lignes. Si le front calculait un total ou un pourcentage d'avancement,
il ne le calculerait que sur ces 10 lignes. Le résultat serait faux, et faux
différemment selon la page affichée.

Conséquences directes :

| Responsabilité | Où |
|---|---|
| Filtrage, tri, pagination | `ActiviteRepository` |
| Statut courant, date de création, avancement | SQL (`SELECT_BASE`) |
| Compteurs des cards | `ActiviteRepository.statistiques` |
| Règles métier de périmètre | `ActiviteService` |
| Affichage, saisie, navigation | `Liste.vue` |

---

## 2. Le statut courant n'existe pas sur l'activité

La table `activite` ne porte **ni son statut, ni sa date de création**. Les
deux sont reconstruits depuis `historique_activite` : c'est la dernière ligne
d'historique qui fait foi.

La dernière ligne est désignée par un `NOT EXISTS` à comparaison de tuples :

```sql
AND NOT EXISTS (
    SELECT 1 FROM historique_activite h2
    WHERE h2.id_activite = act.id_activite
      AND (h2.date_changement, h2.id_historique_activite)
          > (h.date_changement, h.id_historique_activite))
```

C'est exactement équivalent à `ORDER BY ... LIMIT 1`, et le commentaire de
`ActiviteRepository` explique pourquoi la forme `NOT EXISTS` a été choisie :
voir §8.

### `EN_RETARD` n'est pas un statut

C'est un **pseudo-statut**, une règle métier : échéance dépassée **et** statut
non terminal. Il n'existe pas dans la table `statut`.

Conséquence : le filtre qui le reçoit est un `CASE` special dans le SQL, pas une
égalité. Il ne peut donc pas figurer dans la table des options, ni avoir de
libellé en base. La page le traite à part dans `libelleStatut()`.

---

## 3. Quelles activités sont visibles

Trois statuts sont **invisibles** dans le suivi :

- `EN_ATTENTE_VALIDATION`
- `VALIDEE`
- `REJETE`

Une activité encore dans le circuit de validation n'est pas publiée. La
montrer dans une liste de suivi exposerait un travail en cours de validation.

La règle est écrite **une seule fois**, dans `ActiviteRepository` :

```java
String SQL_NON_PUBLIEES = "'EN_ATTENTE_VALIDATION', 'VALIDEE', 'REJETE'";
```

et appliquée à trois endroits :

| Où | Pourquoi |
|---|---|
| `FILTRES` (liste **et** cards) | Les cards partagent le `WHERE` de la liste : sinon le total afficherait des activités absentes du tableau. |
| `autocomplete` | Sinon le menu proposerait des activités impossibles à ouvrir. |
| `StatutRepository.findOptionsActifs` | Sinon le sélecteur de statut offrirait un choix sans aucun résultat. |

La clause conserve les activités **sans historique** (`sta.code IS NULL`) :
elles ne sont pas « en attente de validation », elles sont simplement
inconnues, et les masquer serait une perte de données silencieuse.

---

## 4. Cards et liste : une seule source

`FILTRES` est utilisé tel quel par la requête paginée **et** par la requête de
statistiques. Les deux ne peuvent donc pas diverger.

Ce qui a été vérifié sur les données de démonstration (2026) :

| | Valeur |
|---|---|
| Total avant exclusion | 100 |
| `EN_ATTENTE_VALIDATION` masquées | 57 |
| **Total affiché** | **43** |
| Non commencées | 1 |
| En cours | 11 |
| Suspendues | 1 |
| Terminées | 28 |
| Annulées | 1 |
| Reportées | 1 |
| En retard | 8 |

Chacune des sept valeurs de card est reproductible en interrogeant la liste
avec le filtre de statut correspondant.

### Deux familles de compteurs, et leur ordre d'affichage

C'est la subtilité de cette rangée de cards, et la raison pour laquelle
`En retard` est placée **en dernier**.

| Famille | Cards | Relation |
|---|---|---|
| **Statuts courants** | Non commencées, En cours, Suspendues, Terminées, Annulées, Reportées | Mutuellement exclusifs : une activité visible a exactement un statut courant. |
| **Règle métier** | En retard | Recoupe les précédentes. |

Conséquence : la somme des six cartes de la première famille **vaut** le total
(1 + 11 + 1 + 28 + 1 + 1 = 43). C'est une vraie property de coherence, et elle
ne tient que parce que les six cartes couvrent tous les statuts courants
visibles. Ajouter une card pour un statut manquant la romprait.

`En retard` s'ajoute par-dessus, et aucune somme ne doit l'inclure. Le placer au
milieu de la rangée ferait croire qu'il s'additionne aux autres. Il compte à
la fois des activités `EN_COURS`, `SUSPENDUE` et `NON_COMMENCEE` dont
l'échéance est dépassée.

> Ce décompte a une contrepartie côté métier : `SUSPENDUE` et `NON_COMMENCEE`
> sont volontairement absents de `StatutActiviteEnum`, qui ne liste que les
> statuts **terminaux**. Une activité suspendue n'est pas finie, elle peut
> encore être en retard.


### Un statut est posé par le clic sur une card

Il n'y a **pas** de champ « statut » dans la zone de filtres. Seul le clic sur
une card le pose, et un second clic sur la même card le retire. Le badge
« filtre actif » permet de le retirer sans connaître la card concernée.

La table `CODES_CARTES` associe chaque card à son code, et sert **à la fois**
à l'affichage et au clic. Avec deux listes séparées, une card finirait par
filtrer sur un code absent du badge correspondant.

`SUSPENDUE` est le seul statut dont la couleur n'existe pas dans Bootstrap
(variante `purple`, déclarée dans le `<style scoped>`). Lui emprunter celle de
`REPORTEE` ou de `NON_COMMENCEE` rendrait les deux badges indistinguables dans
le tableau.

### `EN_RETARD` n'est pas un compteur de la même famille

Voir « Deux familles de compteurs » ci-dessus : `enRetard` recoupe
`EN_COURS`, `NON_COMMENCEE` et `SUSPENDUE`, et ne s'additionne à aucun.

---

## 5. Autocomplete

Les deux champs texte (activité, objectif spécifique) partagent
`frontend/src/composables/useAutocomplete.ts`.

### Le problème réel : l'ordre des réponses

C'est la difficulté d'un autocomplete, et elle n'a rien à voir avec l'affichage.

Une requête HTTP déjà envoyée ne s'annule pas. Taper `abc` puis `ab` vite peut
faire arriver la réponse de `ab` **après** celle de `abc` : le menu afficherait
des résultats qui ne correspondent plus au texte tapé.

Le composable incrémente un jeton à chaque frappe. Une réponse dont le jeton
n'est plus le jeton courant est **écartée**. On ne tente pas d'annuler la
requête, on ignore la réponse devenue obsolète.

### Le reste du comportement

- **Debounce de 350 ms.** Une requête par frappe inonderait l'API, et les
  réponses arriveraient dans le désordre.
- **Saisie trop courte → invalidation.** Le jeton est incrémenté pour annuler
  aussi la requête en vol, sinon elle rouvrirait le menu sur une saisie devenue
  vide.
- **Fermeture au clic extérieur**, avec une `racine` qui englobe le champ *et*
  la liste : sans elle, le clic qui choisit une suggestion fermerait le menu
  avant d'être traité.
- **Clavier** : `↑` / `↓` (cyclique), `Entrée` (valide et empêche la
  soumission du formulaire), `Échap` et `Tab` (ferment).

### Ce que fait la sélection

| Champ | Valeur affichée | Filtre appliqué | Pourquoi |
|---|---|---|---|
| Activité | le libellé | `filtres.search = code` | Le code est le seul identifiant stable ; un libellé contient des espaces, des accents et peut évoluer. |
| Objectif | le libellé | `filtres.objectifSpecifiqueId = id` | Un objectif a un identifiant, donc le filtre est exact. Le texte est vidé pour ne pas resserrer la requête. |

À la frappe, le texte alimente le filtre correspondant. La liste et les
compteurs se rechargent avec **le même** délai de 350 ms. `appliquerFiltres()`
annule le minuteur en attente quand elle est appelée directement : une
sélection de suggestion ne déclenche donc pas une seconde requête.

---

## 6. La pagination : deux indexations, un seul endroit qui traduit

L'interface est en base 1, l'API en base 0.

| | Indexation |
|---|---|
| `BasePagination` (affichage, libellé « 41–43 sur 43 ») | 1 |
| `Pageable` / `Page` de Spring, donc l'API | 0 |

La conversion est faite **dans `listerActivites`**, et nulle part ailleurs.

### Pourquoi cette règle a un effet de bord bien plus grave

Sans la conversion, cliquer sur la dernière page (N) envoie `page=N`. L'API
indexe à partir de 0, donc N est hors bornes et la réponse revient **vide**.

La page vide déclenchait alors la récupération prévue pour un filtre devenu
trop restrictif :

```js
page.value = Math.max(1, totalPages.value)   // totalPages vaut N
```

Le clamp redonnait le **même** numéro de page. La même requête partait, la
même réponse vide revenait, et `charger()` s'appelait lui-même à l'infini.
Chaque tour déclenchait deux requêtes (liste + statistiques) : d'où la rafale
de requêtes 1000, 1200, 1300… et l'abandon de la connexion.

Le garde qui rend la boucle terminable est explicite :

```js
if (derniere === page.value) {
    activites.value = []      // la page n'a pas bougé : on s'arrête
} else {
    page.value = derniere
    return charger()          // au plus un second tour
}
```

**Clamper n'est pas une garantie de terminaison.** C'est la comparaison
avant de rappeler qui l'est. Un `return charger()` sans ce garde est un bug
latent, quelle que soit la cause de la page vide.

### Le rejet de promesse non géré

`get()` transforme une réponse HTTP en échec métier, mais laisse remonter une
**panne réseau** : `fetch` rejette sur un abort ou un serveur injoignable.
Cette rejection traversait `Promise.all`, remontait de `charger()`, que les
appelants lancent avec `void charger()` — personne n'attend la promesse.

Résultat : `Uncaught (in promise) TypeError: NetworkError when attempting to
fetch resource`.

`charger()` et `chargerOptions()` rattrapent désormais, et les deux callers de
`void charger()` sont réputés ne jamais rejeter. C'est le contrat implicite de
toute fonction asynchrone appelée en `void` : **soit elle gère ses erreurs,
soit elle ne peut pas être appelée ainsi.**

---

## 7. Le périmètre

Le backend n'applique jamais à l'utilisateur plus de droits qu'il n'en a.
`servicePerimetre` prime sur `serviceDemande` : un utilisateur rattaché à un
service ne peut pas élargir son périmètre en changeant un filtre.

Le filtre « Service » n'est rendu que si le backend a renvoyé des services.
Un utilisateur de niveau service n'en reçoit pas : le filtre n'est pas
*masqué*, il n'a simplement rien à offrir.

---

## 8. Pièges SQL à ne pas réintroduire

Trois règles s'appliquent à toute requête native de `ActiviteRepository`.
Elles ont été apprises en corrigeant des 500, et un commentaire dans le fichier
les rappelle à chaque lecture.

### 1. Aucune apostrophe, pas même dans un commentaire

Spring Data analyse la chaîne pour distinguer le SQL des littéraux et bascule
un booléen à chaque apostrophe. La première fait basculer tout le reste en
« chaîne », et les paramètres nommés qui suivent cessent d'être enregistrés.
Symptôme : `QueryParameterException` sur un paramètre pourtant présent.

### 2. Aucun `ORDER BY` dans une requête **paginée**

Le tri vient du `Pageable`, et Spring Data ne l'injecte que si
`hasOrderByClause()` renvoie `false`. Cette méthode compte les `order by` du
texte mais n'en retire qu'une occurrence de sous-requête (regex gloutonne) : deux
`ORDER BY` dans des sous-requêtes suffisent à la tromper. Elle concatène alors
le tri sans le mot-clé, et PostgreSQL répond `syntax error at or near ','`.

D'où le `NOT EXISTS` à comparaison de tuples pour « dernière ligne ».

> `autocomplete` fait exception : elle ne reçoit pas de `Pageable`, donc
> `hasOrderByClause()` n'est jamais appelé. Son `ORDER BY act.code` est sûr.

### 3. Les parenthèses d'un `IN` sont posées par l'appelant

`SQL_NON_PUBLIEES` ne contient **pas** de parenthèses. Un appelant qui ajoute
les siennes écrit `NOT IN ((...))`, que PostgreSQL lit comme une comparaison
`varchar = record` et refuse.

Corollaire pour les blocs de texte : la concaténation d'une constante se fait
**hors** du `"""..."""`, sinon le `+` reste un caractère littéral.

---

## 9. Ce qui n'est pas fait, et pourquoi

| Absence | Raison |
|---|---|
| Bouton « Ajouter une activité » | La page de création n'existe pas. Bouton désactivé plutôt que route fictive. |
| `BaseTable` | Ce composant pagine et recherche côté client, ce qui est explicitement exclu ici. Le tableau est écrit directement, avec `BasePagination` pour la pagination serveur. |
| Tri dans les en-têtes de colonnes | Le tri est géré par le `Pageable`. brancher les en-têtes demanderait un état de tri dans `Liste.vue`, à faire avec la même exigence de cohérence. |

---

## 10. Vérifications

```bash
# Backend — le port 5433 correspond au conteneur Docker.
# Sans DB_PORT, le test tente 5432 et échoue sur une autre base locale.
cd backend && DB_PORT=5433 ./mvnw -o test

# Front
cd frontend && npx vue-tsc --noEmit -p tsconfig.app.json
```

`npm run lint` signale des erreurs préexistantes dans `BaseInput`,
`BaseToast` et `BaseTable`, ainsi que `Liste` en nom de composant mono-mot.
Elles ne sont pas liées à cette page.
