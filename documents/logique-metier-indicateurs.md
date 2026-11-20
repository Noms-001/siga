# Logique métier de la gestion des indicateurs

**Périmètre :** référentiel des indicateurs, association aux activités, saisie des valeurs, calcul d’atteinte, calendrier, retards et alertes.

> Ce document décrit le comportement observé dans le code et le schéma de base. Il distingue les règles effectivement appliquées des conventions métier supposées par l’application. Les indicateurs ne sont pas eux-mêmes calculés à partir des activités : l’application stocke une valeur numérique saisie pour une période, puis la compare à la cible configurée.

## 1. Rôle d’un indicateur

Un indicateur sert à mesurer périodiquement un phénomène associé à une ou plusieurs activités. Sa fiche de référentiel contient :

- un **code** unique et une désignation ;
- un type, utilisé pour interpréter la cible (`KPI`, `KRI` ou autre valeur) ;
- une unité de mesure ;
- une fréquence de vérification et une fréquence d’agrégation ;
- une valeur cible et, facultativement, des seuils minimum et maximum ;
- une définition, une méthode de détermination et un objectif ;
- un état actif/inactif.

La fréquence de vérification fixe l’intervalle attendu entre deux relevés. La fréquence d’agrégation sert à regrouper les relevés pour une analyse sur une période. Ces deux fréquences ont donc des rôles différents.

## 2. Création et gestion du référentiel

La création, la modification et l’activation/désactivation se font dans le backoffice.

1. Le code et la désignation sont obligatoires ; le code est limité à 50 caractères et doit être unique.
2. À la création, un indicateur est actif par défaut si le statut n’est pas fourni.
3. Le type et les fréquences sont saisis comme des chaînes, et non sélectionnés dans une liste fermée au formulaire. Pour que les calculs prévus fonctionnent, les valeurs doivent néanmoins correspondre aux codes attendus, notamment `KPI`, `KRI` et les fréquences décrites plus bas.
4. La cohérence des seuils est vérifiée lorsque les trois bornes sont renseignées : `seuilMin ≤ valeurCible ≤ seuilMax`. Les champs peuvent rester partiellement ou totalement absents.
5. Un indicateur actif peut être modifié. Le code ne permet pas de modifier un indicateur désactivé ; il faut d’abord le réactiver.
6. La désactivation ne supprime ni l’indicateur ni son historique. Elle le retire de l’usage courant et interdit les nouvelles saisies ainsi que les nouveaux rattachements.

Le frontoffice consulte par défaut les seuls indicateurs actifs. Le backoffice peut filtrer les actifs/inactifs et par type.

## 3. Rattachement à une activité

La relation entre les activités et les indicateurs est plusieurs-à-plusieurs, portée par `activite_indicateur` : une activité peut suivre plusieurs indicateurs et un même indicateur peut être rattaché à plusieurs activités.

- Seuls les indicateurs actifs peuvent être liés à une activité.
- Un même indicateur ne peut pas être lié deux fois à la même activité.
- La liste des indicateurs disponibles exclut ceux déjà liés à l’activité.
- Le rattachement et le retrait sont refusés lorsque l’activité est terminée ou annulée. Les changements de liaison sont aussi soumis au périmètre de l’utilisateur.
- Le suivi des valeurs reste attaché à l’indicateur, et non à une copie de celui-ci par activité. Une valeur saisie est donc visible dans le suivi de toutes les activités liées à cet indicateur.

## 4. Saisie et conservation des valeurs

Une valeur est un relevé ponctuel associé à un indicateur et à une période. Le relevé conserve la valeur numérique, le début et la fin de période, un commentaire facultatif, la date de saisie, l’utilisateur qui a saisi la valeur et l’indicateur concerné.

### Règles de saisie

- La valeur, la date de début et la date de fin sont obligatoires.
- La date de fin doit être strictement postérieure à la date de début.
- Il ne peut pas exister deux valeurs pour le même indicateur avec exactement les mêmes dates de début et de fin.
- La saisie est **append-only** : une valeur existante n’est pas écrasée ; une nouvelle mesure ajoute une ligne et conserve l’historique.
- Une saisie est permise uniquement si l’indicateur est actif et accessible dans le périmètre de l’utilisateur.
- La modale propose par défaut la période du mois courant ; elle peut être préremplie depuis une cellule du calendrier. Le serveur valide les dates, mais ne force pas la période à suivre exactement la fréquence configurée.
- L’API d’historique est paginée (taille maximale 100). L’écran de détail demande une seule page de 100 valeurs ; les relevés plus anciens restent accessibles par l’API paginée, mais ne sont pas chargés dans ce tableau frontend.

Le code ne rejette pas une valeur parce qu’elle se trouve sous ou au-dessus de la cible ou des seuils. La comparaison sert à afficher l’état de l’indicateur, pas à interdire l’enregistrement.

## 5. Détermination de la valeur courante

Pour l’écran de valeur courante et les calculs d’atteinte, l’application choisit d’abord le relevé terminé dont `periode_fin` est la plus récente, c’est-à-dire la plus récente valeur dont la période est achevée à la date du traitement. En cas d’égalité, l’identifiant le plus élevé départage les relevés.

Si aucune période n’est encore terminée, le système utilise le relevé ayant la date de fin la plus récente, même si sa période est future. En l’absence de tout relevé, la valeur courante est absente : elle ne doit pas être interprétée comme zéro.

## 6. Calcul de l’atteinte de la cible

L’atteinte est déterminée par le type exact et la valeur cible, à partir de la valeur courante :

| Type | Règle appliquée | Interprétation |
|---|---|---|
| `KPI` | valeur ≥ cible | Atteint |
| `KPI` | valeur < cible | Non atteint |
| `KRI` | valeur ≤ cible | Atteint / risque maîtrisé |
| `KRI` | valeur > cible | Non atteint / seuil de risque dépassé |
| Type absent ou différent, cible absente, valeur absente | aucune comparaison possible | Indéterminé |

L’écart numérique affiché est `valeur courante − valeur cible`. Le signe de cet écart ne suffit pas à lui seul pour interpréter le résultat : un écart positif est favorable pour un KPI mais peut être défavorable pour un KRI.

Les champs `seuil_min` et `seuil_max` sont affichés dans les fiches, mais le calcul d’atteinte décrit ci-dessus ne les utilise pas. Seule la cible et la distinction KPI/KRI déterminent l’état atteint/non atteint.

## 7. Calendrier et agrégation

### Calendrier annuel

Le calendrier découpe l’année demandée selon la fréquence de vérification :

- mensuelle : 12 cellules ;
- trimestrielle : 4 cellules ;
- semestrielle : 2 cellules ;
- annuelle : 1 cellule.

Pour chaque cellule, l’écran affiche une valeur trouvée dans la période, son état d’atteinte, la date et l’auteur de la saisie. Sans valeur, une période future est « à venir », la période en cours est « en cours » et une période passée est « manquante ». Une cellule sans valeur passée constitue un manque de saisie, pas une mesure égale à zéro. La moyenne annuelle affichée ne prend en compte que les cellules qui ont une valeur ; si aucune cellule n’est renseignée, le service renvoie zéro.

La cellule est associée à un relevé dont la date de début tombe dans les bornes de la cellule. La saisie n’étant pas contrainte à la fréquence de vérification, un relevé dont la période chevauche une cellule sans commencer dans celle-ci peut ne pas être affecté à cette cellule.

### Agrégation sur une plage

Une route backend permet de demander une agrégation sur une plage de dates et une méthode parmi : moyenne, somme, minimum, maximum et dernière valeur. Les relevés dont les périodes chevauchent la plage sont retenus, puis regroupés selon la fréquence d’agrégation mensuelle, trimestrielle, semestrielle ou annuelle. Sans relevé dans un intervalle, la valeur agrégée de cet intervalle est absente (`null`). Cette fonction est exposée par l’API ; aucun parcours frontend de consultation de cette agrégation n’a été identifié dans les vues examinées.

**Point technique à vérifier :** la méthode `DERNIERE` consomme les valeurs triées par date de fin décroissante, mais sélectionne le dernier élément de cette liste. Cela correspond au relevé le plus ancien du groupe, et non au plus récent. La règle attendue pour « dernière valeur » doit être confirmée ou le choix de l’élément corrigé avant d’utiliser cette méthode dans un résultat métier.

## 8. Retard de saisie

Le calcul de retard s’applique aux indicateurs actifs, rattachés à au moins une activité du périmètre et dont la fréquence de vérification est renseignée.

- **Jamais saisi :** aucune valeur n’existe. Le statut est `JAMAIS_SAISI`; aucune date d’échéance ni durée de retard n’est calculée.
- **Avec historique :** l’application prend le relevé le plus récent, puis calcule la prochaine échéance en ajoutant une cadence à sa date de fin : un mois, trois mois, six mois ou un an.
- **En retard :** l’heure courante est strictement postérieure à cette échéance. Le nombre de jours est calculé entre l’échéance et l’heure du traitement.
- **À jour :** l’échéance n’est pas dépassée. Cette catégorie peut être incluse ou omise selon le paramètre de l’API.
- Une fréquence non reconnue ne produit pas de date d’échéance et l’indicateur est ignoré dans cette liste.

Le retard de saisie est distinct du non-atteinte de la cible : un indicateur peut avoir une valeur non atteinte mais être à jour, ou avoir une valeur atteinte tout en étant en retard sur le prochain relevé.

## 9. Tableau de bord et périmètre d’accès

Le tableau de bord des indicateurs présente les indicateurs actifs du périmètre, leur valeur courante et leur état d’atteinte, puis une synthèse des atteints, non atteints, indéterminés, jamais saisis et en retard de saisie. Les non-atteints sont affichés avant les indéterminés, les jamais saisis et les atteints.

Pour le calcul de retard, le périmètre est résolu via les activités liées à l’indicateur et le service/département de l’utilisateur. Les opérations de consultation d’un indicateur vérifient également qu’il est rattaché à une activité visible dans le périmètre.

**Cas limite observé dans le dashboard :** si la requête ne trouve aucun indicateur actif avec fréquence de vérification dans le périmètre, le tableau de bord remplace cette liste par tous les indicateurs actifs, sans filtre de périmètre dans ce fallback. À l’inverse, la liste des retards continue d’utiliser le périmètre. Ce comportement peut donner des ensembles différents ; il mérite une vérification métier et de sécurité.

## 10. Notifications de retard

Un traitement planifié s’exécute tous les jours à 08:00, selon l’heure du serveur. Il recherche les indicateurs en retard, y compris ceux jamais saisis, et crée des notifications internes :

- pour les indicateurs déjà notifiés dans les sept derniers jours, aucune nouvelle notification n’est créée ;
- les destinataires sont les utilisateurs ayant une affectation non désaffectée comme responsable ou suppléant sur une activité liée à l’indicateur ;
- si aucune priorité de notification active n’est configurée ou si aucun destinataire n’est trouvé, aucune notification n’est créée ;
- le message distingue l’absence totale de saisie du retard après une dernière période connue.

Le job ajoute des notifications dans l’application ; l’envoi éventuel d’un courriel dépend du traitement général des notifications et de sa configuration.

## 11. Parcours utilisateur résumé

1. Le backoffice crée le référentiel : code, désignation, type, cible, seuils, unités, fréquences et méthode de définition.
2. Un responsable associe l’indicateur actif à une ou plusieurs activités encore modifiables.
3. Un utilisateur autorisé saisit une valeur pour une période et ajoute éventuellement un commentaire.
4. Le système conserve la saisie dans l’historique, choisit la valeur courante et calcule l’atteinte selon KPI/KRI.
5. Le calendrier matérialise les périodes renseignées et manquantes ; les agrégations regroupent les relevés pour analyse.
6. Le suivi détecte les échéances dépassées et le job quotidien prévient les responsables/suppléants concernés.

## 12. Points métier à confirmer

- Les types devraient-ils être limités aux valeurs `KPI` et `KRI`, ou les types libres sont-ils voulus ? Le calcul automatique n’interprète que les codes exacts `KPI` et `KRI`.
- Les seuils min/max doivent-ils modifier le statut d’atteinte ou rester des repères descriptifs ? À ce jour, seul le champ cible est utilisé.
- La fréquence doit-elle imposer une période de saisie conforme (par exemple un relevé par mois) ? Le serveur vérifie l’unicité des bornes exactes, mais pas la cadence.
- La prochaine échéance doit-elle être calculée à partir de la fin de la dernière période saisie, ou d’un calendrier fixe ? L’implémentation ajoute simplement la cadence à cette fin de période.
- Pour `DERNIERE`, la valeur attendue est-elle la plus récente de la période ? Le comportement actuel semble prendre la plus ancienne après tri décroissant.
- Le fallback du dashboard aux indicateurs actifs hors périmètre doit-il être supprimé ou limité au périmètre courant ?
- Confirmer l’heure du job de notification dans l’environnement de déploiement, car le cron ne définit pas explicitement de fuseau horaire.

## Sources principales dans le projet

- [Contrôleur du référentiel backoffice](../backend/src/main/java/mg/bank/backend/controller/backoffice/IndicateurController.java)
- [Service de gestion du référentiel](../backend/src/main/java/mg/bank/backend/service/indicateur/IndicateurService.java)
- [Service de suivi, calcul d’atteinte, calendrier et retards](../backend/src/main/java/mg/bank/backend/service/indicateur/SuiviIndicateurService.java)
- [Service de rattachement aux activités](../backend/src/main/java/mg/bank/backend/service/indicateur/LiaisonIndicateurService.java)
- [Service des notifications de retard](../backend/src/main/java/mg/bank/backend/service/indicateur/IndicateurRetardNotificationService.java)
- [Job quotidien de notification](../backend/src/main/java/mg/bank/backend/scheduler/IndicateurRetardScheduler.java)
- [Schéma SQL des indicateurs et des valeurs](../database/script/schema.sql)
- [Écran backoffice de création/modification](../frontend/src/views/backoffice/indicateurs/Form.vue)
- [Suivi frontoffice des indicateurs](../frontend/src/views/frontoffice/indicateurs/Suivi.vue)
- [Fiche et historique d’un indicateur](../frontend/src/views/frontoffice/indicateurs/Show.vue)
- [Calendrier des saisies](../frontend/src/components/CalendrierValeurs.vue)
