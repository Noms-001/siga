# Détail d'une activité — fonctionnement et choix de conception

Ce document explique ce que fait `GET /api/activites/{id}`, et **pourquoi** il
est construit ainsi. Il est le pendant de `activites-liste.md` : la liste et le
détail lisent la même donnée par des chemins différents, et c'est délibéré.

Il s'adresse à quelqu'un qui doit le maintenir ou le reprendre, pas à un
utilisateur de l'application.

Fichiers principaux :

- `backend/src/main/java/mg/bank/backend/repository/ActiviteRepository.java` (sections `Detail` et `Detail d une sous-activite`)
- `backend/src/main/java/mg/bank/backend/service/ActiviteDetailService.java`
- `backend/src/main/java/mg/bank/backend/dto/ActiviteDetailDTO.java`
- `backend/src/test/java/mg/bank/backend/ActiviteDetailIntegrationTests.java`
- `backend/src/test/java/mg/bank/backend/SousActiviteDetailIntegrationTests.java`

Côté front, les pages qui consomment ces contrats :

- `frontend/src/views/frontoffice/activites/Detail.vue`
- `frontend/src/views/frontoffice/activites/SousActivite.vue`
- `frontend/src/services/activite.ts` (`detailActivite`, `detailSousActivite`)
- `frontend/src/types/activite.ts` (section détail, miroir des DTO)

---

## 1. Le principe directeur

> **Une requête par type de donnée, jamais une requête par sous-activité.**

Le détail combine onze tables. Le nombre de requêtes qu'il exécute est
**fixe**, et ne dépend ni du nombre de sous-activités, ni du nombre de
livrables, ni du nombre de fichiers. Le regroupement se fait ensuite en
mémoire, par identifiant de parent.

C'est la seule propriété structurante de ce module. Tout le reste en découle.

---

## 2. Le modèle : les tables existent, les entités non

Le détail est la seule partie du projet qui lit des tables **sans mapping JPA**.
Il faut savoir pourquoi, car c'est contre-intuitif.

| Table | Entité JPA | Rôle dans le détail |
|---|---|---|
| `activite` | `Activite` | l'activité elle-même |
| `sous_activite` | aucune | sous-activité |
| `avancement_sous_activite` | aucune | relevés d'avancement |
| `affectation_sous_activite` | aucune | responsables |
| `livrable_sous_activite` | aucune | livrables |
| `fichier_sous_activite` | aucune | fichiers déposés |
| `activite_indicateur` | aucune | jointure indicateur ↔ activité |
| `indicateur` | aucune | définition d'un indicateur |
| `valeur_indicateur` | aucune | mesures d'un indicateur |
| `historique_activite` | aucune | statut courant **et** historique |
| `resultat_intermediaire` | aucune | résultats intermédiaires |

**Pourquoi ne pas créer les entités ?** Parce qu'aucune de ces tables n'est
modifiée par l'application, et que trois d'entre elles (`affectation_sous_activite`,
`activite_indicateur`, et dans une moindre mesure `resultat_intermediaire`) ne
sont que des tables de jointure ou ne portent que deux colonnes. Créer un modèle
d'écriture pour des tables qui ne sont que lues exposerait une surface
d'écriture sans usage, qu'il faudrait ensuite maintenir.

Des **projections** (`repository/projection/`) et des **requêtes natives**
suffisent, et produisent des DTO qui ne contiennent jamais plus que ce que la
page affiche.

> Cette décision est révisable. Le jour où une de ces tables doit être écrite,
> une entité JPA est le bon outil — mais elle n'a pas lieu d'exister tant que
> seule la lecture existe.

---

## 3. Le statut : une seule table à lire

`activite` ne porte aucune colonne de statut. L'état d'une activité se lit dans
`historique_activite`, et la ligne qui fait foi est la plus récente :

```sql
SELECT sta.code
FROM historique_activite h
JOIN statut sta ON sta.id_statut = h.id_statut
WHERE h.id_activite = :id
ORDER BY h.date_changement DESC, h.id_historique_activite DESC
LIMIT 1
```

Le second critère de tri n'est pas décoratif : deux changements peuvent
partager la même date au ticks près, et c'est alors la plus grande clé qui fixe
l'ordre. Sans lui, la ligne portant le statut courant pourrait changer de place
d'une requête à l'autre.

Il n'existe qu'une seule notion d'état, et une seule table qui l'exprime. La
table `validation_activite` et son enum `decision_validation` décrivent un
circuit d'examen qui n'est plus implémenté : aucun chemin de code n'écrit dans
cette table, et le détail ne la lit plus.

Le tableau des statuts reste nonetheless plus large que les statuts que le
backend écrit. Seul `BROUILLON` est produit aujourd'hui ; `EN_ATTENTE_VALIDATION`,
`VALIDEE` et `REJETE` subsistent dans la table `statut` parce que le jeu de
données de démonstration en contient — et parce que `SQL_NON_PUBLIEES` doit
continuer à les masquer dans la liste (voir `activites-liste.md`).

---

## 4. Ce qui est reconstruit, et comment

### Le statut courant

L'activité ne porte pas son statut. Il vient de la dernière ligne de
`historique_activite`, désignée par comparaison de tuples :

```sql
(date_changement, id_historique_activite)
```

Le **second** critère n'est pas décoratif : deux changements peuvent partager
la même `date_changement`, et c'est alors la plus grande clé qui tranche.

Le détail réutilise `SELECT_BASE`, le bloc de jointures de la liste, plutôt que
de le recopier. Les deux lectures partagent donc leur source : le statut affiché
dans le détail ne peut pas diverger de celui affiché dans la liste.

### La date de création

`activite` n'a pas de `date_creation`. Elle vaut `MIN(date_changement)` de
l'historique. Nullable : une activité sans historique n'est pas datable.

### L'avancement global

**Ce n'est pas** la moyenne des valeurs d'indicateurs, ni un champ stocké.
C'est la moyenne des **derniers** avancements de chaque sous-activité :

```sql
AVG(dernier.valeur_pourcentage)
```

C'est la même formule que la liste, et pour la même raison : le détail doit
afficher le même pourcentage que la ligne dont il est issu.

### `avancementCourant` est nullable, et c'est nécessaire

Sur le jeu de données actuel, **seules 10 sous-activités sur 100** ont un
relevé dans `avancement_sous_activite`. Une sous-activité sans relevé n'a pas
un avancement de `0` : elle n'a **pas d'avancement constaté**. Sur une page de
suivi, `0` signifie « lancé à 0 % » et `null` signifie « jamais évalué » — la
distinction est visible par l'utilisateur, donc elle est conservée.

---

## 5. Le nombre de requêtes

| # | Requête | Portée |
|---|---|---|
| 1 | activité + statut + objectif + avancement | 1 ligne |
| 2 | sous-activités | toutes |
| 3 | avancements | toutes, groupés par sous-activité |
| 4 | affectations | toutes, groupées par sous-activité |
| 5 | livrables | tous, groupés par sous-activité |
| 6 | fichiers | tous, groupés par livrable |
| 7 | indicateurs | tous |
| 8 | valeurs d'indicateurs | toutes, groupées par indicateur |
| 9 | résultats intermédiaires | tous |
| 10 | historique | toutes |

**10 requêtes, quel que soit le contenu de l'activité.**

Chaque requête enfant est un `JOIN` depuis `activite`, jamais une requête par
identifiant parent. C'est ce qui rend la propriété vraie.

> Revenir à une boucle — `for (sa : sousActivites) findLivrables(sa.getId())` —
> casserait cela **silencieusement** : le code resterait correct, et l'impact ne
> se verrait qu'à l'échelle. C'est pourquoi le test `pasDeNPlusUn` compare le
> nombre de requêtes d'une activité à un livrable et d'une activité à deux
> livrables : il échouerait immédiatement si la propriété était perdue.

### L'ordre de lecture porte du sens

Les requêtes qui alimentent un historique sont triées de façon que **la première
ligne soit l'élément courant** :

```sql
ORDER BY id_sous_activite, date_changement DESC, id_historique_sous_activite DESC
```

Le tri commence par le parent, ce qui permet le regroupement en mémoire, puis
classe chaque groupe du plus récent au plus ancien. Le service désigne donc
l'avancement courant par `getFirst()` du groupe, sans recherche supplémentaire.

Le drapeau `etatActuel` / `valeurCourante` n'est **pas** calculé en base : il
est posé par le service en comparant chaque ligne à la première. Dupliquer dans
le SQL un critère de sélection qui doit rester identique à celui du tri serait
une seconde source de vérité à garder synchronisée.

`Collectors.groupingBy` est utilisé avec un `LinkedHashMap` explicite, pour que
le regroupement conserve l'ordre de lecture au lieu de le perdre.

---

## 6. Sécurité

### Le périmètre, pas les permissions

Le détail applique **exactement** le périmètre de la liste, délégué à
`ActiviteService.getPerimetre()` — jamais recalculé, pour que les deux endpoints
ne puissent pas diverger sur ce qui autorise un accès :

| Situation de l'appelant | Effet |
|---|---|
| Rattaché à un service | voit ce service uniquement |
| Niveau département | voit tous les services du département |
| Sans rattachement | aucune restriction |

Le filtre est appliqué **dans le SQL de l'activité**, pas après coup sur une
liste déjà chargée. C'est ce qui permet de distinguer un identifiant absent
d'un identifiant interdit.

> Le projet possède des tables `permission`, `poste_permission` et `role`
> peuplées, mais **aucun code Java ne les lit** : ni `@PreAuthorize`, ni
> `hasAuthority`, ni repository de permissions. Le seul contrôle réel est le
> périmètre ci-dessus. Le détail ne s'appuie pas sur un mécanisme de permissions
> qui n'existe pas, et n'en invente pas un. Voir §9.

### 404 ou 403 : la distinction est volontaire

| Situation | Réponse |
|---|---|
| L'activité n'existe pas | `404 Activite introuvable` |
| L'activité existe, hors périmètre | `403 Vous n'êtes pas autorisé à consulter cette activité` |
| Identifiant nul ou négatif | `400` |
| Sans authentification | `403` (voir §9) |

Confondre les deux premiers cas en renvoyant `404` partout donnerait un message
trompeur — une activité réellement interdite paraîtrait inexistante — et
interdirait à l'appelant de distinguer une faute de frappe d'un accès refusé.

Le coût de la vérification supplémentaire est ** nul en fonctionnement normal** :
elle n'est exécutée qu'après l'échec de la lecture principale, donc uniquement
lorsqu'il y a un refus à signaler.

Le message du `403` ne nomme ni l'activité ni son service : il confirme l'accès
refusé, pas la position de la donnée.

### L'identité ne vient pas du client

L'appelant est lu dans le `SecurityContext`, jamais transmis en paramètre. Il
ne peut donc pas être forgé, et un utilisateur ne peut pas élargir son périmètre
en modifiant une requête.

---

## 7. Ce que la réponse ne contient pas

### `chemin_fichier` n'est pas exposé

C'est un **chemin de stockage interne**. L'exposer ne rend aucun service à une
page qui affiche une liste de livrables, et cela laisserait fuir l'arborescence
du serveur.

Seul `nomFichier` est renvoyé, qui identifie le dépôt sans dire où il se trouve.
Le contenu binaire ne transite pas par cette API **dans le détail** : il passe
par l'endpoint de téléchargement dédié (voir §13), qui renvoie le contenu et
**jamais** le chemin de stockage. Le chemin ne sert qu'à retrouver le dépôt sur
le disque du serveur.

### Aucune collection n'est `null`

Toutes les listes — `sousActivites`, `indicateurs`,
`resultatsIntermediaires`, `historique`, et les listes imbriquées — sont jamais
nulles. Une activité sans indicateur expose `[]`, ce qui permet au front de
distinguer « rien à afficher » d'« information manquante » sans tester `null`.

Le test `collectionsVidesNonNulles` vérifie l'absence de `"indicateurs":null`
dans la réponse brute, pas seulement le statut HTTP.

### L'email n'est pas répété

`UtilisateurResumeDTO` ne porte que `id`, `nom` et `prenom`. L'email et le
téléphone ne sont pas repris : le détail n'a pas besoin de coordonnées, et les
exposer multiplierait les occurrences de données personnelles pour un affichage
qui se limite au nom.

---

## 8. Contrats de forme

### Ordre des listes

| Liste | Ordre | Raison |
|---|---|---|
| `historique` | décroissant | le plus récent d'abord |
| `historiqueAvancement` | décroissant par sous-activité | idem |
| `valeurs` (indicateur) | `periode_fin` décroissante | la période la plus récente d'abord |
| `sousActivites` | `date_debut_prevue`, puis `code` | ordre de planning, stable |
| `affectations` | `date_affectation` décroissante | le responsable actuel d'abord |
| `livrables` | `designation` | ordre de lecture stable |
| `fichiers` | `nom_original`, puis `version` décroissante | versions de la plus récente à la plus ancienne |
| `indicateurs` | `code` | `code` est unique, donc l'ordre est total |
| `resultatsIntermediaires` | `designation` | ordre de lecture stable |

Aucun de ces ordres n'est fourni par le client, et aucun ne dépend de l'ordre
physique des lignes en base. Les seconds critères de tri existent pour les
listes où le premier n'est pas total.

### Le statut est nullable

`ActiviteDetailDTO.statut` est `null` si l'historique est vide. La liste renvoie
un statut `null` dans ce cas, et le détail fait de même plutôt que de produire
une référence à moitié vide.

---

## 9. Ce qui n'est pas fait, et pourquoi

| Point | Décision |
|---|---|
| `Detail.vue` et sa route | **Créés.** Route `activites/:id(\\d+)` (nommée `activite-detail`), page `frontend/src/views/frontoffice/activites/Detail.vue` consommant `/api/activites/{id}`. Le regex `(\\d+)` exclut les liens morts de la sidebar (`/activites/nouvelle`, `/activites/a-valider`). La navigation part du bouton œil de `Liste.vue`. |
| `SousActivite.vue` et sa route | **Créés.** Voir §12 : endpoint dédié, page dédiée, section sous-activités du détail devenue tableau. |
| Entités JPA pour les tables de détail | **Non créées.** Voir §2. |
| Téléchargement de fichier | **Implémenté.** Voir §13 : endpoint dédié `GET .../fichiers/{idFichier}` renvoyant le contenu binaire, jamais le chemin de stockage. |
| Contrôle par permissions | **Non ajouté.** Les tables existent mais aucun code ne les lit. Ajouter un mécanisme de permissions parallèlement au périmètre serait une architecture concurrente ; c'est une décision à prendre à part. |
| Pagination du détail | **Non appliquée.** Un détail n'est pas paginé : il doit être rendu en entier. Si une activité finit par avoir des centaines de livrables, c'est le seul point qui devra être rediscuté. |
| Modification des données | **Non implémentée.** Le détail est en lecture seule. |

### Deux écarts à connaître

1. **L'anonyme reçoit `403`, pas `401`.** C'est le comportement général du
   projet : `SecurityConfig` ne déclare aucun point d'entrée d'authentification
   (`formLogin` et `httpBasic` sont désactivés). Une requête anonyme se heurte
   donc à un accès refusé. Corriger cela modifierait **tous** les endpoints
   existants : c'est une décision propre à `SecurityConfig`, pas à cet
   endpoint. Le test fixe le comportement réel pour qu'un changement soit
   visible.

2. **Les indicateurs sont partagés entre activités.** `activite_indicateur` ne
   porte aucune donnée propre, et `valeur_indicateur` ne dépend que de
   l'indicateur. Un indicateur rattaché à deux activités affiche donc **les mêmes
   mesures chez les deux**. C'est le modèle existant ; le détail le restitue
   sans l'interpréter. Corriger cela demanderait de rattacher la valeur à
   l'activité, ce qui est un changement de modèle.

### Une question à trancher, volontairement laissée ouverte

**Le détail n'applique pas la règle des activités « non publiées ».**

La liste masque les activités dont le statut courant est `EN_ATTENTE_VALIDATION`,
`VALIDEE` ou `REJETE` (`SQL_NON_PUBLIEES` dans `ActiviteRepository`). Le détail,
lui, renvoie le contenu complet de toute activité située dans le périmètre de
l'appelant, y compris une activité à l'un de ces statuts, **si son identifiant
est connu**.

Ces trois statuts sont aujourd'hui des données de démonstration : aucun chemin
de code ne les produit (voir §3). La question se posera de nouveau le jour où
une activité pourra à nouveau quitter le brouillon.

Ce n'est pas une fuite au sens du périmètre : l'activité reste dans le service
de l'appelant. Mais c'est une divergence de visibilité entre la liste et le
détail, et la règle d'origine est formulée en termes d'**exposition** d'un
travail non publié, ce qui s'applique aussi à une page de détail.

Aucune des deux options n'a été retenue ici, parce qu'elles ont des
conséquences opposées :

- **l'appliquer** interdirait d'ouvrir le détail d'une activité que la liste
  vient de cacher, pour un utilisateur autorisé à la voir ;
- **ne pas l'appliquer** laisse accessible en direct une activité que la liste
  cache.

C'est une question d'intention produit, pas une décision technique. Le point à
confirmer est donc : *le détail doit-il suivre la règle de publication de la
liste, ou est-il volontairement plus permissif ?* Si la réponse est « il doit
suivre », le changement tient en une ligne dans `findActiviteDetail` et ne
touche à rien d'autre.

---

## 10. Tests

`ActiviteDetailIntegrationTests` — 12 tests, exécutés sur la base de
développement, pas sur une base simulée.

> **Pourquoi la vraie base ?** Le comportement à vérifier est justement ce qu'une
> base en mémoire ne reproduirait pas : les tuples de comparaison, le `LEFT JOIN
> LATERAL` du statut courant, les contraintes de périmètre. Un test sur H2
> passerait puis le code échouerait en production.

L'authentification passe par `@WithMockUser` : le service lit l'identité dans le
contexte de sécurité, et c'est exactement ce que le test doit reproduire. Aucun
mot de passe de test n'est nécessaire, et aucun utilisateur de test n'est créé
en base.

**Aucun identifiant n'est figé** : les activités sont recherchées par leurs
propriétés (service, nombre de livrables, absence d'indicateur), et une
recherche qui ne trouve rien est une erreur explicite. Un test qui passerait à
vide parce que le jeu de données a changé est un test qui ne teste plus rien.

| Test | Ce qu'il protège |
|---|---|
| `detailAccessible` | cas nominal complet |
| `activiteHorsPerimetreRefusee` | `403` sur une activité d'un autre service |
| `horsPerimetreDistingueDeAbsente` | l'activité existe bien, le `403` porte sur l'accès |
| `activiteInexistante` | `404` sur identifiant absent |
| `accesAnonymeRefuse` | comportement anonyme réel (voir §9) |
| `identifiantInvalide` | `400` sur identifiant `0` |
| `niveauDepartementVoitLesTousServices` | le niveau département voit les deux services |
| `collectionsVidesNonNulles` | pas de `null` dans les collections |
| `historiqueDecroissantEtStatutCoherent` | tri décroissant, un seul `etatActuel`, égal au statut de l'activité |
| `avancementGlobalCoherent` | l'avancement calculé hors Java vaut celui renvoyé |
| `sousActiviteSansReleve` | `avancementCourant` est `null`, pas `0` |
| `pasDeNPlusUn` | même nombre de requêtes avec 1 ou 2 livrables |

---

## 11. Vérifications

```bash
# Backend : le port 5433 correspond au conteneur Docker.
# Sans DB_PORT, le test tente 5432 et échoue sur une autre base locale.
cd backend
DB_PORT=5433 ./mvnw -o test

# Front : vérification de types et de conventions.
cd frontend
npx vue-tsc --noEmit -p tsconfig.app.json
```

Pour un aperçu de la réponse réelle, ouvrir `/api/activites/1` avec un
utilisateur du service concerné. La réponse respecte l'enveloppe habituelle :

```json
{ "success": true, "data": { "...": "..." } }
```

et, en cas d'erreur, `{ "success": false, "data": null, "error": "..." }` —
le champ d'erreur s'appelle **`error`**, pas `message`.

---

## 12. Le détail d'une sous-activité

Le détail d'activité s'est révélé trop riche pour une sous-activité isolée :
affectations, historique des relevés et livrables n'y sont pas lisibles à
l'étage d'une sous-activité. Deux changements en découlent.

### Un endpoint dédié

`GET /api/activites/{idActivite}/sous-activites/{idSousActivite}`.

Il **réutilise** le DTO existant `SousActiviteDetailDTO` et la méthode
d'assemblage `sousActivite(...)` du détail d'activité : même définition de
l'avancement courant (première ligne du groupe trié), mêmes règles de tri,
mêmes regroupements. Aucun DTO nouveau : une sous-activité isolée est exactement
la même structure que la sous-activité imbriquée dans une activité.

### Toujours un nombre de requêtes fixe

| # | Requête | Portée |
|---|---|---|
| 1 | la sous-activité, jointe à son activité **et au périmètre** | 1 ligne |
| 2 | relevés d'avancement de la sous-activité | tous |
| 3 | affectations de la sous-activité | toutes |
| 4 | livrables de la sous-activité | tous |
| 5 | fichiers des livrables de la sous-activité | tous |

Cinq requêtes, quel que soit le nombre de relevés, de responsables ou de
livrables, puis la lecture de l'utilisateur courant. Aucune boucle n'appelle
le repository par identifiant enfant. Le test `pasDeNPlusUn` de
`SousActiviteDetailIntegrationTests` le garantit, exactement comme pour
l'activité.

### Deux identifiants, trois façons de se tromper

| Situation | Réponse |
|---|---|
| `idActivite` ou `idSousActivite` nul ou négatif | `400` |
| La sous-activité n'existe pas | `404 Sous-activite introuvable` |
| La sous-activité existe mais est rattachée à **une autre** activité | `404 Sous-activite introuvable` |
| L'activité n'existe pas (avec une sous-activité qui, elle, existe) | `404 Sous-activite introuvable` |
| L'activité existe mais sort du périmètre | `403` |
| Sans authentification | `403` (même règle §9) |

Une sous-activité d'une autre activité n'est pas une donnée interdite : c'est
une ressource inexistante **dans le parent demandé**, donc un `404` — pas un
`403`. Les trois comptages de l'erreur (`compterSousActivite`,
`compterSousActiviteDeActivite`) ne sont payés qu'après l'échec de la lecture
principale, comme pour l'activité.

### Côté front : un tableau et une page

La section « Sous-activités » de `Detail.vue` est devenue un **tableau**
(Code, Désignation, Période prévue, Responsables, Livrables, Avancement,
Actions) : chaque ligne donne une vue d'ensemble, et le bouton œil ouvre la
page dédiée. Les cartes — riches, mais empilées au point d'enlever toute vue
comparative — ont disparu.

`SousActivite.vue` (route `activites/:id(\\d+)/sous-activites/:sousActiviteId(\\d+)`,
nommée `sous-activite-detail`) affiche le détail complet : informations,
avancement courant, historique des relevés, affectations (actuelles et
passées), livrables et fichiers. Le regex `(\\d+)` est appliqué aux deux
niveaux : une sous-activité ne se construit que sous une activité numérique
réelle.

### Tests

`SousActiviteDetailIntegrationTests` — 11 tests, mêmes principes que
`ActiviteDetailIntegrationTests` (vraie base, `@WithMockUser`, aucun
identifiant figé) :

| Test | Ce qu'il protège |
|---|---|
| `detailAccessible` | cas nominal complet, pas de collection `null` |
| `sousActiviteHorsPerimetreRefusee` | `403` sur une sous-activité d'une activité d'un autre service |
| `sousActiviteInexistante` | `404` sur identifiant absent |
| `sousActiviteDAutreActivite` | `404` sur une sous-activité d'une autre activité, même service |
| `activiteInexistante` | `404` sur une activité absente avec sous-activité existante |
| `accesAnonymeRefuse` | comportement anonyme réel |
| `identifiantInvalide` | `400` sur `0` à chacun des deux niveaux |
| `sousActiviteSansReleve` | `avancementCourant` est `null`, historique vide |
| `historiqueDecroissantEtAvancementCourantCoherent` | tri décroissant, un seul courant, égal à l'avancement exposé |
| `avancementCourantCoherent` | l'avancement courant vaut le dernier relevé, calculé hors Java |
| `pasDeNPlusUn` | même nombre de requêtes avec 1 ou 2 livrables |

---

## 13. Le téléchargement d'un fichier

`GET /api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}`.

### Pourquoi un endpoint dédié

Le détail (§7) n'expose pas `chemin_fichier`, et le front ne doit jamais le
connaître. Le contenu binaire transite donc par un endpoint à part, qui
**n'appartient pas à l'enveloppe JSON** : un JSON ne peut pas porter des octets.

- Le **succès** renvoie le fichier brut : `Content-Type` du dépôt
  (`type_mime`, fallback `application/octet-stream` si vide) et disposition
  `inline` pour que le navigateur puisse l'afficher en aperçu.
- Les **erreurs** restent au format JSON habituel
  (`{ success: false, error }`) via `GlobalExceptionHandler` : le front arrive à
  les lire sans jamais parser le blob.

### Le même contrôle que le détail, descendu d'un niveau

La logique de `telechargerFichier` (`ActiviteDetailService`) applique d'abord
les identifiants invalides, puis le périmètre **dans le SQL** (la requête
`findFichierTelechargement` joint le fichier à sa sous-activité, son activité
et le périmètre de l'appelant). En cas d'échec, `refusOuAbsenceFichier`
départage les `404` des `403`, avec les mêmes comptages que le détail :

| Situation | Réponse |
|---|---|
| `idActivite`, `idSousActivite` ou `idFichier` nul ou négatif | `400` |
| Le fichier n'existe pas | `404 Fichier introuvable` |
| Le fichier est celui d'une **autre** sous-activité | `404 Fichier introuvable` |
| La sous-activité est celle d'une **autre** activité | `404 Fichier introuvable` |
| L'activité existe mais sort du périmètre | `403` |
| **Le fichier est en base mais absent du disque** | `404 Le fichier n est plus disponible sur le serveur` |
| Sans authentification | `403` (même règle §9) |

Comme pour le détail, ces comptages ne sont payés **que sur le chemin de
l'erreur** : le cas nominal coûte une seule requête.

Un fichier encore référencé en base mais dont le dépôt a disparu du disque est
un `404` : ce n'est pas une donnée interdite, c'est une ressource qui n'existe
plus. La vérification se fait avec `Files.isRegularFile` sur le chemin
normalisé issu de la base, puis `Files.readAllBytes` ; la réponse ne contient
que le contenu, jamais ce chemin.

Le nom d'origine vient de la base (`nom_original`, pas du client) et est
transmis en UTF-8 dans `Content-Disposition` au format RFC 5987
(`filename*=`), pour que « mise à jour.pdf » survive au transit en en-tête.
`Cache-Control: no-store` empêche la mise en cache d'un document qui peut être
confidentiel.

### Côté front

Un bouton **œil** (aperçu) et un bouton **téléchargement** sur chaque fichier
de `SousActivite.vue`, avec `telechargerFichier()` dans `services/activite.ts` :
`getBlob()` (`services/api-client.ts`) fait la requête avec l'en-tête
`Authorization` et renvoie un `Blob` en succès, ou l'erreur JSON habituelle.
Le contenu n'est donc jamais accessible sans le jeton.

L'aperçu ouvre une `BaseModal` : `<img>` si le type MIME commence par
`image/`, `<iframe>` si `application/pdf`, message sinon. Les `objectURL`
créés pour l'aperçu sont révoqués à la fermeture et au démontage du composant.
Le téléchargement passe par un lien `a[download]` temporaire qui porte le
`nom_original` ; son `objectURL` est révoqué aussitôt.

### Tests

`FichierTelechargementIntegrationTests` — 8 tests. Les fichiers de `data.sql`
pointent vers `/uploads`, qui n'existe pas sur la machine de développement :
chaque test crée donc son propre fichier dans un `@TempDir` et insère une ligne
`fichier_sous_activite` qui pointe dessus (transaction annulée à la fin).
Les fichiers se trouvent sur le disque réel du test, pas sur un volume monté.

| Test | Ce qu'il protège |
|---|---|
| `fichierAccessible` | contenu exact, `Content-Type`, `filename*` RFC 5987 |
| `fichierHorsPerimetreRefuse` | `403` sur un fichier d'une activité d'un autre service |
| `fichierInexistant` | `404` sur un identifiant absent |
| `fichierDAutreSousActivite` | `404` sur un fichier d'une autre sous-activité, même service |
| `activiteInexistante` | `404` sur une activité absente |
| `fichierAbsentDuDisque` | `404` si le dépôt n'existe plus sur le serveur |
| `accesAnonymeRefuse` | comportement anonyme réel |
| `identifiantInvalide` | `400` sur identifiant `0` |
