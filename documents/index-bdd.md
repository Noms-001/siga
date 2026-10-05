# Index — à quoi ils servent dans ce projet

Ce document explique ce que font les index déclarés dans `schema.sql`, et
**pourquoi ils existent** plutôt que d'avoir été ajoutés par réflexe. Il
s'adresse à quelqu'un qui doit les maintenir, en ajouter, ou decides s'il est
légitime d'en supprimer un.

Fichiers concernés :

- `database/script/schema.sql` — les 15 index, chacun placé à la suite de sa table
- `backend/src/main/java/mg/bank/backend/repository/ActiviteRepository.java` — les requêtes qu'ils servent

---

## 1. Le principe directeur

> **Un index n'est pas un accélérateur générique. C'est la réponse à une
> requête précise, et il ne sert à rien si cette requête change de forme.**

Un index n'accélère pas « la base ». Il accélère **une** manière de trouver des
lignes. PostgreSQL l'utilise quand le plan qu'il envisage lui coûte moins cher
que le reste — et il a le droit de l'ignorer.

Conséquence directe, à garder en tête avant toute suppression :

> **Supprimer un index ne provoque jamais une erreur.** Le code continue de
> fonctionner, les tests passent, et la page met simplement plus de temps. Une
> régression de performance ne s'écrit dans aucun test.

C'est la différence entre cette optimisation et celles de `activites-liste.md` :
là, on peut écrire un test qui échoue ; ici, le seul test possible est une
mesure.

Chaque index de ce projet répond donc à une question précise, formulée en
requête. Le catalogue du §4 fait cette correspondance ligne à ligne.

---

## 2. Pourquoi le projet en avait besoin

Le projet n'avait qu'**un seul** index déclaré : `uq_plan_action_origine_principale`.
Aucune colonne de clé étrangère n'était indexée.

Or le cœur de l'application reconstruit trois informations qui **n'existent pas
comme colonnes** : le statut courant, la date de création et l'avancement. Voir
`activite-detail.md` §4 pour le pourquoi. Ces trois valeurs sont calculées par
trois `LEFT JOIN LATERAL`, dont deux utilisent la désignation de la dernière
ligne par comparaison de tuples :

```sql
LEFT JOIN LATERAL (
    SELECT h.id_statut
    FROM historique_activite h
    WHERE h.id_activite = act.id_activite
      AND NOT EXISTS (
          SELECT 1
          FROM historique_activite h2
          WHERE h2.id_activite = act.id_activite
            AND (h2.date_changement, h2.id_historique_activite)
                > (h.date_changement, h.id_historique_activite))
) hist ON TRUE
```

Cette écriture est **équivalente** à `ORDER BY ... LIMIT 1` — c'est un choix
délibéré, expliqué dans `activites-liste.md` §8.2 : un `ORDER BY` dans une
requête paginée fait échouer `hasOrderByClause()` de Spring Data, qui ne retire
qu'une occurrence de sous-requête et produit ensuite une erreur de syntaxe.

Le problème n'est pas cette écriture. C'est qu'**elle est quadratique sans
index** : PostgreSQL relit toute la table `historique_activite` pour chaque
ligne, puis autant de fois qu'il y a de lignes d'historique. Le plan le disait
avant tout correctif :

```
Seq Scan on historique_activite h  (actual rows=3.61 loops=100)  Rows Removed by Filter: 357
  Seq Scan on historique_activite h2 (actual rows=3.61 loops=361)  Rows Removed by Filter: 357
```

`Rows Removed by Filter: 357`, 100 fois, puis 361 fois. Environ 166 000 lignes
examinées pour afficher 100 activités.

### La mesure

Sur une base jetable, avec la requête de liste réelle, 5 000 activités /
15 061 lignes d'historique / 24 900 sous-activités :

| | Sans les index | Avec les index |
|---|---|---|
| Temps d'exécution | 612,8 ms | **1,4 ms** |
| Buffers lus | 45 269 | **810** |

Sur le jeu de données actuel (100 activités), l'écart est de 10,9 ms → 1,9 ms.
C'est peu, et c'est trompeur : le plan était quadratique, donc l'écart **croit
linéairement avec le volume**. 424× à 5 000 activités, contre 5,6× à 100.

C'est toute la justification : ces index ne sont pas un confort sur les
données actuelles, ils empêchent une page de mettre plusieurs secondes le jour
où le projet cesse d'être une démonstration.

---

## 3. La règle qui gouverne le catalogue

> **Un index par couple (table, motif d'accès), pas un index par table.**

Un index sur une colonne de clé étrangère qui n'est jamais cherchée seule est
du bruit : il coûte une écriture à chaque `INSERT` et ne sert personne.

L'argument « ça servira à supprimer le parent » ne tient pas ici. Le schéma ne
déclare **aucun** `ON DELETE` : toutes les clés étrangères sont en `NO ACTION`,
et la seule suppression du projet — celle d'une sous-activité — passe par un
contrôle explicite des dépendances (`compterDependances`, voir
`activite-detail.md`). Aucune clé étrangère n'est donc jamais supprimée en
cascade, et l'index qui l'accompagnerait ne servirait à rien.

Et l'inverse est vrai : `id_activite` est la colonne la plus consultée du
projet, sur `historique_activite`, `sous_activite` et `resultat_intermediaire`.
**Ce sont trois index différents**, sur trois tables différentes. Un seul
n'aurait servi que le premier cas rencontré.

---

## 4. Le catalogue

Chaque ligne existe pour une requête nommée. La colonne « Requête » indique ce
qu'il faut aller vérifier avant de supprimer l'index.

| Index | Requête servie |
|---|---|
| `ix_historique_activite_activite`<br>`(id_activite, date_changement DESC, id_historique_activite DESC)` | `SELECT_BASE` : statut courant **et** date de création (`MIN`) · `findHistorique` · `findStatutCourant` · 1 index pour 4 requêtes |
| `ix_sous_activite_activite`<br>`(id_activite, date_debut_prevue, code)` | LATERAL d'avancement · `findSousActivites` et sa jumelle dérivée : le filtre **et** le `ORDER BY` |
| `ix_avancement_sous_activite_sous`<br>`(id_sous_activite, date_changement DESC, id_historique_sous_activite DESC)` | Dernier avancement courant par sous-activité · `findAvancements` · `findAvancementsSousActivite` |
| `ix_affectation_sous_activite_sous`<br>`(id_sous_activite, date_affectation DESC)` | `findAffectations` · `findAffectationsSousActivite` · `compterDependances` |
| `ix_livrable_sous_activite_sous`<br>`(id_sous_activite, designation)` | `findLivrables` · `findLivrablesSousActivite` · `compterDependances` |
| `ix_fichier_sous_activite_livrable`<br>`(id_livrable_sous_activite)` | `findFichiers` · `findFichiersSousActivite` |
| `ix_resultat_intermediaire_activite`<br>`(id_activite)` | `findResultatsIntermediaires` |
| `ix_valeur_indicateur_indicateur`<br>`(id_indicateur)` | `findValeursIndicateurs`, qui passe par `activite_indicateur` |
| `ix_utilisateur_email`<br>`(email)` | `findByEmail` — **appelé à chaque requête authentifiée**, via `CustomUserDetailsService` puis `ActiviteService.utilisateurCourant()` |
| `ix_token_auth_token`<br>`(token)` | `findByToken` — même fréquence |
| `ix_service_departement`<br>`(id_departement, nom)` | `findOptionsActivesParDepartement` |
| `ix_activite_service`<br>`(id_service)` | Filtre de périmètre, présent sur **toutes** les listes |
| `ix_activite_objectif`<br>`(id_objectif_specifique)` | Filtre PTA / objectif, présent sur **toutes** les listes |
| `ix_objectif_specifique_annee`<br>`(annee DESC, code)` | `autocomplete` : `ORDER BY annee DESC, code LIMIT` |

### Un piège à connaître : `ix_service_departement`

`service` porte déjà `UNIQUE(id_service, id_departement)`. Cet index **ne sert
pas** la recherche par département : en PostgreSQL, un index composite ne peut
être utilisé sur sa deuxième colonne que si les colonnes précédentes sont
égalités dans la requête. Ici `id_service` ne l'est pas. D'où un index
supplémentaire, avec `nom` en deuxième colonne pour couvrir le `ORDER BY nom`.

Le même raisonnement vaut pour `parametre.findByCodeAndActifTrue`, qui **est**
couvert : `UNIQUE(code)` commence par la colonne cherchée.

---

## 5. Les deux index en `DESC` : ne pas les repasser en `ASC`

`ix_historique_activite_activite` et `ix_avancement_sous_activite_sous` sont
déclarés avec `DESC` sur leurs colonnes de date. C'est délibéré, et c'est
surchargé d'un commentaire dans `schema.sql` pour une raison précise.

L'index sert deux choses :

1. le `NOT EXISTS (date, id) > (date, id)`, qui compare dans un sens ;
2. les `ORDER BY date_changement DESC, id DESC`, qui réclament cet ordre.

Repasser les colonnes en `ASC` **ne casserait rien** : la comparaison de tuples
resterait correcte, les résultats identiques, les tests verts. Simplement,
l'index ne fournirait plus l'ordre, et PostgreSQL ajouterait un tri sur chaque
lecture d'historique. La dégradation serait invisible — d'où le commentaire.

---

## 6. Ce qui n'est volontairement pas indexé

| Absence | Raison |
|---|---|
| `activite_indicateur` | Sa clé primaire est **déjà** `(id_activite, id_indicateur)`. Le `WHERE id_activite = :id` de `findIndicateurs` et `findValeursIndicateurs` est donc déjà couvert. Un index serait redondant. |
| `site`, `priorite`, `type_activite`, `role`, `statut`, `poste`, `permission`, `departement` | Tables de référence jointes par clé primaire, de quelques lignes à quelques dizaines. `statut.code`, `role.code` et `priorite.code` sont déjà `UNIQUE`. Un seq scan sur 10 lignes est le plan le moins cher qui soit — un index ne serait jamais utilisé. |
| `plan_action` et ses 6 tables filles, `notification`, `origine` | **Aucun repository du backend ne les interroge.** Elles sont dans le schéma parce qu'elles sont prévues, pas parce qu'elles sont lues. Indexer reviendrait à deviner un usage futur. |
| `validation_activite`, `procedure`, `etape_validation` | Le circuit de validation n'est pas implémenté : aucun repository ne lit ni n'écrit dans ces trois tables. L'index `ix_validation_activite_activite` reste dans le schéma, mais plus aucune requête ne s'en sert. Comme `plan_action`, elles sont prévues, pas lues. |
| `poste_utilisateur`, `poste_permission`, `type_activite_service` | Clés primaires composites déjà indexées, et aucune requête ne les parcourt en dehors d'un chargement JPA par entité. |
| `utilisateur.email` en `UNIQUE` | Le jeu de données respecte l'unicité, mais elle encode une règle métier qui n'est pas écrite dans le schéma. Un index simple a été posé ; passer en `UNIQUE` est une décision, pas une optimisation. |

---

## 7. Ce que les index ne règlent pas

Deux limites subsistent. Les connaître évite de croire qu'un index règle un
problème de forme de requête.

### Le tri par défaut de la liste n'est pas indexable

La liste est paginée sur `dateCreationTri`, qui vaut
`COALESCE(crea.date_creation, TIMESTAMP '-infinity')`, c'est-à-dire
`MIN(historique.date_changement)`. C'est une **valeur calculée** : aucun index
sur `activite` ne peut la fournir. PostgreSQL trie donc la page après avoir
calculé la date des lignes candidates.

Le correctif serait une colonne `date_creation` dénormalisée sur `activite`,
alimentée à l'écriture. Ce n'est pas fait ici : cela duplique une donnée
dérivée, donc introduit une source de désynchronisation, et cela n'a de sens
qu'au-delà de quelques milliers d'activités. À traiter le jour où la liste
passe en timeout, pas avant.

### Les filtres `EXTRACT` et `ILIKE` restent non sargables

Dans `FILTRES` :

```sql
AND (:annee IS NULL OR EXTRACT(YEAR FROM act.date_debut_prevue) = :annee)
AND (:recherche IS NULL OR act.code ILIKE :recherche ...)
```

Une fonction appliquée à une colonne rend l'index inutilisable : PostgreSQL ne
peut pas utiliser un index sur `date_debut_prevue` si la colonne est enveloppée
dans `EXTRACT`. Et `ILIKE '%motif%'` commence par un jocker, donc aucun index
ne peut servir. Ce sont les deux formes de filtre les plus coûteuses du projet,
et elles ne sont pas corrigeables par un index.

`string_to_array(:codesTerminaux, ',')` mérite le même constat : la fonction est
évaluée **par ligne** au lieu d'une fois. Elle devrait être un vrai tableau
(`:codes::text[]` avec `= ANY(...)`). Inerte à 100 activités, réel à 50 000.

---

## 8. Vérifications

Les index ne se testent pas : ils se mesurent. La commande ci-dessous reproduit
la mesure du §2 sans toucher à `sagadb`.

```bash
# La base de travail ne doit jamais être modifiée pour une mesure.
# On en crée une jetable à partir des scripts du dépôt.
docker exec saga-postgres psql -U saga_user -d postgres \
  -c "CREATE DATABASE saga_bench OWNER saga_user;"

docker cp database/script/schema.sql saga-postgres:/tmp/schema.sql
docker cp database/script/data.sql   saga-postgres:/tmp/data.sql
docker exec saga-postgres psql -U saga_user -d saga_bench \
  -v ON_ERROR_STOP=1 -f /tmp/schema.sql -f /tmp/data.sql

# Le plan d'exécution est la vraie vérification : un index ajouté sans être
# utilisé se voit ici, et nulle part ailleurs.
# La requête à expliquer est SELECT_BASE de ActiviteRepository.findActivites
# (statut courant + date de création + avancement), avec LIMIT 20.
docker exec saga-postgres psql -U saga_user -d saga_bench \
  -f /tmp/explain.sql
```

Ce qu'il faut regarder dans le plan, dans cet ordre :

| Indice | Plan | Diagnostic |
|---|---|---|
| 1 | `Seq Scan on historique_activite` avec `loops=` > 1 | index absent, non ajouté, ou colonne non couverte |
| 2 | `Rows Removed by Filter` élevé sur une table indexée | index présent mais inutilisé : vérifier la colonne de tête |
| 3 | `Index Only Scan` sur `historique_activite` | le cas attendu |
| 4 | index déclaré mais absent du plan | normal sur 100 lignes : PostgreSQL préfère le seq scan. Se becomes significatif au-delà de quelques milliers de lignes. |

Le point 4 est la seule chose qui distingue « index inutile » de « index
prématuré », et il se tranche à l'échelle, pas sur le jeu de données actuel.

En fin de manipulation :

```bash
docker exec saga-postgres psql -U saga_user -d postgres \
  -c "DROP DATABASE saga_bench;"
```
