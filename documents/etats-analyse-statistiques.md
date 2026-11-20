# États d’analyse et statistiques de SAGA

**Audit du code et du jeu DEMO : 12 novembre 2026**<br>
**Périmètre :** états et sorties visibles du frontoffice, contrats API et calculs backend associés.<br>
**But :** préparer la partie « États d’analyse et statistiques » du mémoire, recenser les sorties identifiées sans limiter le catalogue à 3–5 états, les classer par priorité et signaler les écarts.

> **À retenir.** Les scénarios fictifs sont isolés dans [database/script/demo_etats_analyse.sql](database/script/demo_etats_analyse.sql). Les fixtures TACT/TIND ont été retirées de [database/script/data.sql](database/script/data.sql), qui ne charge plus que les référentiels, les permissions et les comptes de démonstration, sans aucune activité, sous-activité, valeur ni plan de test. Le jeu DEMO reconstruit contient **48 activités**, **144 sous-activités**, **18 plans d’action**, **6 indicateurs**, **19 rattachements indicateur-activité** et **13 relevés d’indicateur**. Sur une base fraîche, les exemples DEMO ne sont donc pas mélangés aux anciennes activités TACT ; toute autre donnée préexistante ou saisie dans l’application reste néanmoins dans le périmètre du dashboard.

## 1. Catalogue des états identifiés — trié par priorité

Les utilisateurs indiqués sont déduits des périmètres et libellés de l’application. Ils restent à confirmer avec les utilisateurs métier. Tous les états repérés sont accessibles à l’écran ; aucun export PDF ou Excel des états n’a été trouvé dans les sources examinées.

| N° | Priorité | Nom de l’état / sortie | Utilisateur cible | Question métier | Type | Format constaté | Période / filtres | Sources principales |
|---|---|---|---|---|---|---|---|---|
| E1 | Très haute | Tableau de bord général des activités et plans d’action | Responsable de service ou de département | Sur l’exercice 2026, combien d’activités sont en retard et quel service doit recevoir les renforts en premier ? | Tableau de bord | Écran | Année, mois, trimestre ou dates personnalisées ; périmètre utilisateur | `frontend/src/views/frontoffice/Dashboard.vue`, `backend/.../DashboardController.java` |
| E2 | Très haute | Avancement global des activités | Responsable de suivi | L’ensemble des activités a-t-il atteint un niveau d’avancement suffisant pour maintenir le plan annuel, ou faut-il réaffecter des agents aux activités en retard ? | Synthèse | Écran, barre de progression et compteurs | Période du tableau de bord | `DashboardAvancement.vue`, `DashboardRepository.java` |
| E3 | Très haute | Activités en retard et échéances proches | Responsable opérationnel | Quelle équipe doit intervenir avant le 15 novembre sur DEMO-A-042, puis avant le 18 novembre sur DEMO-A-043 ? | Opérationnel | Écran, liste avec liens vers les fiches | Période du tableau de bord ; échéance proche par défaut dans les 7 jours | `DashboardEcheances.vue`, `DashboardService.java` |
| E4 | Haute | Suivi global des plans d’action | Responsable de programme ou de service | Parmi les 18 plans DEMO, lesquels sont déjà en retard et quel responsable doit remettre DEMO-PA-011 sur une trajectoire acceptable avant le 16 novembre ? | Synthèse + opérationnel | Écran, cartes et deux listes | Période du tableau de bord ; échéances dans les 30 jours | `DashboardPlansAction.vue`, `SuiviGlobalPlanActionService.java` |
| E5 | Haute | Activités par statut | Responsable de suivi | Le graphique explique-t-il bien les 48 activités DEMO, ou des statuts restent-ils absents avant la réunion de pilotage ? | Synthèse | Écran, graphique en anneau et légende | Période du tableau de bord | `DashboardStatuts.vue`, `DashboardRepository.java` |
| E6 | Haute | Activités par service | Chef de département | Faut-il renforcer les Risques et Procédures, où 13 activités DEMO restent non commencées, ou les Systèmes de Sécurité, où 11 activités DEMO sont en cours ? | Synthèse | Écran, tableau | Période du tableau de bord ; périmètre appliqué par le serveur | `DashboardServices.vue`, `DashboardRepository.java` |
| E7 | Haute | Comparaison PTA / activités courantes | Direction ou responsable de programme | Le faible avancement des activités courantes DEMO (7,5 %) justifie-t-il de réserver une équipe de rattrapage distincte des activités PTA (51,9 %) ? | Analyse comparative | Écran, deux cartes comparatives | Période du tableau de bord | `DashboardPta.vue`, `DashboardRepository.java` |
| E8 | Haute | Avancement par type d’activité | Responsable de programme | Quel type d’intervention — maintenance curative, contrôle, installation ou procédure — doit être priorisé au prochain comité au vu de son taux moyen et du nombre d’activités concernées ? | Analyse | Écran, graphique en barres | Période du tableau de bord ; filtre service visible au niveau département | `DashboardTypesActivite.vue`, `DashboardRepository.java` |
| E9 | Haute | Avancement des plans d’action par origine | Responsable des risques et plans d’action | Les plans issus d’incidents (82,5 %), de risques (50 %) ou d’audits/recommandations (22,5 %) nécessitent-ils des décisions d’escalade différentes ? | Analyse | Écran, graphique en barres | Période du tableau de bord ; agrégation globale, sans filtre service | `DashboardPlansActionOrigine.vue`, `DashboardRepository.java` |
| E10 | Haute | Indicateurs en retard, jamais saisis ou non atteints | Responsable du suivi des indicateurs | Qui doit saisir la mesure manquante DEMO-KRI-VIDE, et quel plan de rattrapage lancer pour DEMO-KPI-RETARD, bloqué à 86 % depuis août ? | Tableau de bord + opérationnel | Écran, cartes et tableau | Pas de filtre temporel d’ensemble visible ; s’appuie sur les dernières valeurs et retards calculés | `frontend/src/views/frontoffice/indicateurs/Suivi.vue`, `SuiviIndicateurService.java` |
| E11 | Haute | Liste des activités avec compteurs par statut | Agent ou responsable chargé de l’ensemble des activités | Après recherche `DEMO-A-` sur 2026, quelles 45 activités publiées restent visibles et lesquelles dois-je ouvrir pour traiter les 11 retards ? | Opérationnel + synthèse | Écran, cartes, tableau paginé | PTA/courantes, année, trimestre, dates, statut, service, priorité, type, site, objectif et recherche | `activites/Liste.vue`, `ActiviteController.java`, `ActiviteRepository.java` |
| E12 | Haute | Suivi des activités et de leur avancement | Responsable de suivi | Quelles sous-activités affectées à l’équipe ont atteint 100 %, et lesquelles restent à 0 % avant leur échéance ? | Synthèse + opérationnel | Écran, compteurs, cartes d’activité et progression | PTA/courantes, statut, recherche, service et priorité ; **pas de filtre de période dans l’interface actuelle** | `activites/Suivi.vue`, `ActiviteService.java` |
| E13 | Haute | Suivi collectif des membres | Chef de département | En filtrant le service Systèmes de Sécurité, Nantenaina a-t-elle besoin d’un renfort pour finir DEMO-A-042 avant le 15 novembre, tout en relançant DEMO-PA-006 et DEMO-PA-018 ? | Tableau de bord + analyse comparative | Écran, KPI, graphiques et tableau | Année, trimestre ou période personnalisée ; service et membre | `membres/SuiviMembresGlobal.vue`, `MembreSuiviController.java`, `MembreSuiviService.java` |
| E14 | Moyenne à haute | Avancement d’un utilisateur | Responsable hiérarchique | Le profil 2026 de Nantenaina confirme-t-il que DEMO-PA-006 et DEMO-PA-018 sont en retard, et faut-il replanifier leur démarrage ? | Synthèse + opérationnel | Écran, cartes et tableaux | Année sélectionnable | `utilisateurs/Avancement.vue`, `UtilisateurAvancementController.java` |
| E15 | Moyenne à haute | Liste et répartition des indicateurs KPI / KRI | Responsable du référentiel d’indicateurs | L’ensemble des indicateurs contient-il bien trois KPI et trois KRI actifs, et lesquels doivent être archivés ou complétés ? | Opérationnel + synthèse | Écran, tuiles et tableau | Type, fréquence de vérification, fréquence d’agrégation, actif/inactif ; pas de période | `indicateurs/List.vue`, `indicateurFront.ts` |
| E16 | Moyenne | Détail et historique d’un indicateur | Responsable de saisie ou de suivi | La mesure d’octobre des contrôles périodiques (90 %) reste 5 points sous la cible de 95 % : quels contrôles faut-il programmer avant le prochain relevé ? | Analyse opérationnelle | Écran, historique et calendrier | Calendrier par année ; valeurs affichées via une requête limitée à 100 lignes côté client | `indicateurs/Show.vue`, `CalendrierValeurs.vue`, `SuiviIndicateurController.java` |
| E17 | Moyenne | Répartition des signalements par type | Responsable de traitement des incidents et risques | Combien d’incidents, de risques et d’autres signalements de 2026 doivent être transformés en plans d’action ? | Synthèse + opérationnel | Écran, tuiles et tableau | Année, type, présence d’un plan d’action | `signalements/List.vue`, service de signalements ; **aucun signalement n’est généré par le script DEMO** |
| E18 | Moyenne | Activités à soumettre ou à valider | Auteur et responsables du circuit de validation | Parmi les activités ayant une vraie ligne dans `validation_activite`, lesquelles attendent ma décision à l’étape courante, et lesquelles puis-je valider en lot ? | Opérationnel | Écran, tableaux et actions de lot | Filtrage lié au poste, à l’étape et au périmètre ; pas de filtre de période visible | `activites/Brouillons.vue`, `activites/AValider.vue`, `ActiviteController.java` |
| E19 | Moyenne | Liste des plans d’action | Responsables des plans | Faut-il démarrer ou replanifier DEMO-PA-006 et DEMO-PA-018, deux plans de Nantenaina dont l’échéance prévue est passée ? | Opérationnel | Écran, tableau paginé et onglets | Statut, année, origine et recherche pour la liste générale ; vues dédiées pour brouillons, à valider et à démarrer | `planaction/List.vue`, `PlanActionController.java` |
| E20 | Moyenne | Fiche d’activité et fiche de sous-activité | Agent, responsable ou participant à l’activité | Sur DEMO-A-042, quel est l’avancement de chacune des trois sous-activités, qui en est responsable et quels fichiers ont été déposés pour les livrables ? | Opérationnel | Écran de détail | Une activité par identifiant ; pas de filtre temporel de rapport | `activites/Detail.vue`, `activites/SousActivite.vue`, `ActiviteDetailService.java` |
| E21 | Moyenne | Détail d’un plan d’action et historique de progression | Responsable du plan | DEMO-PA-011 est à 45 % pour une échéance au 16 novembre : quelle progression a été saisie et quelle action corrective le responsable doit-il consigner ? | Opérationnel | Écran de détail | Une fiche par plan ; historique de saisie | `planaction/Show.vue`, `PlanActionController.java` |

### Sorties repérées mais non disponibles comme état utilisateur complet

- **Suivi global des membres par catégories, statuts et évolution mensuelle :** l’API `GET /api/utilisateurs/suivi/global` et ses types existent, mais le composant frontoffice inspecté appelle `GET /api/utilisateurs/suivi/membres`. Je n’ai pas trouvé d’écran qui consomme l’endpoint `/global`. Ne pas le présenter comme un état réellement livré sans retrouver son consommateur dans le projet.
- **Risques et incidents sur le tableau de bord principal :** une route backend `/api/dashboard/risques-incidents` existe, mais elle n’est pas appelée par le tableau de bord frontend. Le service documente en outre l’absence de table de risques/incidents dans le modèle pris en charge par cet endpoint. Ne pas annoncer un état « risques et incidents » sur le dashboard principal.
- **Dashboard backoffice :** `frontend/src/views/backoffice/Dashboard.vue` ne contient qu’un titre et un commentaire de contenu. Il ne produit pas d’état statistique utilisable.
- **Récapitulatif du formulaire d’activité (`Ajout.vue`) :** il affiche les valeurs en cours de saisie (type, code, désignation, dates calculées, priorité, nombre de sous-activités). C’est une aide de saisie, pas un état agrégé destiné à la décision ; il n’est pas compté comme rapport.

## 2. Scénarios métier concrets à partir des données DEMO

### Périmètre et statut des chiffres

Les exemples ci-dessous s’appuient sur le jeu de démonstration de [database/script/demo_etats_analyse.sql](database/script/demo_etats_analyse.sql). Le script a été exécuté sur la base de développement et ses contrôles SQL confirment 48 activités, 144 sous-activités, 18 plans, 6 indicateurs, 19 rattachements indicateur-activité et 13 relevés. Les résultats détaillés restent à confronter à l’API et aux écrans du dashboard.

Les valeurs de l’ensemble des activités ci-dessous concernent uniquement les enregistrements dont le code commence par `DEMO-`. Le dashboard n’a pas de filtre de préfixe ; après initialisation fraîche, il agrège les activités présentes dans la base, dont les 48 activités DEMO. La page de liste permet de rechercher `DEMO-A-` et masque les activités `EN_ATTENTE_VALIDATION` conformément à sa règle de publication.

### Exemple A — Décider où affecter une équipe de rattrapage

Pour l’année 2026, le jeu DEMO compte **48 activités** : **36 PTA**, **12 activités courantes** et **24 par service**. Leur répartition de statut courant est calculée ainsi :

| Statut courant DEMO | Nombre | Lecture de gestion |
|---|---:|---|
| Terminées | 11 | Activités clôturées dans l’année |
| En cours | 11 | 5 ont dépassé leur échéance au 12/11 |
| Non commencées | 15 | 5 ont dépassé leur échéance ; 10 ont une échéance future |
| Suspendues | 2 | 1 a dépassé son échéance |
| Reportées | 3 | Statuts terminaux pour le calcul de retard du dashboard |
| Annulées | 3 | Statuts terminaux pour le calcul de retard du dashboard |
| En attente de validation | 3 | Une date passée et deux échéances futures ; statut masqué de la liste publiée |
| **Total** | **48** | Les catégories de statut courant ne comprennent pas le pseudo-statut de retard |

La règle « en retard » du dashboard est une **mesure qui recoupe les statuts**, et non un statut exclusif : échéance passée et statut non terminal. Elle donne **12 activités DEMO en retard** : 5 en cours + 5 non commencées + 1 suspendue + 1 en attente de validation.

La moyenne d’avancement DEMO calculée depuis les relevés courants est **40,83 %** : les 48 activités ont chacune trois sous-activités avec relevé, et la requête calcule la moyenne des moyennes d’activité. Cette moyenne donne le même poids à chaque activité ; ce jeu ne permet pas de tester des activités ayant des nombres de sous-activités différents.

Le graphique en anneau ne représente pas les 48 activités DEMO de façon exhaustive. Sa requête affiche **39** en comptant 11 terminées, 12 en retard, 6 en cours non échues et 10 non commencées non échues. Les 3 annulées, les 3 reportées, l’unique suspendue qui n’est pas en retard et les 2 activités en attente de validation non échues n’apparaissent pas dans ces quatre parts (soit 9 activités hors anneau). Les KPI comptent 11 activités « en cours », alors que la part « en cours » du graphique ne compte que les 6 non échues. Le responsable ne doit donc pas utiliser l’anneau seul pour répartir les ressources par statut.

**Décision concrète.** Le tableau « Activités par service » doit inciter le chef de département à examiner le service **Gestion des Risques et Procédures** : parmi ses 24 activités DEMO, 13 sont non commencées et 11 terminées, pour **45,8 %** d’avancement moyen. Le service **Gestion des Systèmes de Sécurité** compte 24 activités, dont 11 en cours, 2 non commencées, 2 suspendues, 3 reportées, 3 annulées et 3 en attente de validation, pour **35,8 %** d’avancement moyen. Il convient donc de suivre les activités non commencées du premier service et les échéances en cours du second.

**Décision PTA / courant.** Les 36 PTA DEMO ont un avancement moyen de **51,94 %** ; les 12 activités courantes, **7,50 %**. Le responsable de programme peut décider d’un point de rattrapage dédié aux activités courantes, mais doit d’abord vérifier que leur faible moyenne correspond à des retards opérationnels et non à des tâches volontairement non commencées.

### Exemple B — Arbitrer les deux prochaines échéances

Au **12/11/2026**, le panneau « Échéances » du jeu DEMO contient **12 retards** et **8 échéances dans les sept jours**. Les deux plus proches illustrent une décision immédiate (les autres échéances de la fenêtre sont portées par `DEMO-A-022`, `023`, `026`, `027`, `028` et `030`) :

| Activité | Responsable affecté dans le jeu | Échéance | Statut / avancement calculé | Décision attendue |
|---|---|---|---|---|
| `DEMO-A-042` — maintenance planifiée des équipements | Nantenaina Rakotomalala | 15/11/2026 | En cours, **70 %**, priorité haute ; sous-activités à 55 %, 70 % et 85 % | Maintenir une équipe d’intervention sur le dossier et contrôler la clôture des trois sous-activités avant le 15/11. |
| `DEMO-A-043` — mise à jour des consignes opérationnelles | Miora Ravelomanantsoa | 18/11/2026 | Non commencée, **0 %**, échéance dans six jours | Affecter un intervenant immédiatement ou faire arbitrer le report avant que l’échéance soit dépassée. |

La première décision relève d’un suivi de fin de travaux ; la seconde demande un arbitrage de capacité ou de calendrier. Ces deux cas sont plus utiles dans le mémoire qu’une capture du seul graphique sans lecture métier.

### Exemple C — Choisir les plans d’action à escalader

Le jeu DEMO comprend **18 plans** : 6 terminés, 6 en cours et 6 non commencés. Au 12/11/2026, **10 plans sont en retard** et **2 plans non terminés** ont une échéance dans les 30 jours.

`DEMO-PA-011`, associé à une recommandation d’audit, est porté par **Miora Ravelomanantsoa**. Son avancement final est **45 %** et son échéance est le **16/11/2026**. Le chef de service peut demander un plan de clôture des actions restantes avant la date prévue. Le suivi par origine donne, dans le jeu DEMO, **82,5 %** pour les incidents, **50 %** pour les risques et **22,5 %** pour les audits/recommandations (6 plans par catégorie). Il serait donc pertinent d’escalader en premier les plans issus des audits/recommandations, puis de vérifier les retards individuels avant d’interpréter ces moyennes.

### Exemple D — Décider quelles mesures d’indicateurs relancer

Le périmètre départemental DEMO compte six indicateurs : trois KPI et trois KRI. Les quatre séries régulières comportent trois relevés mensuels d’août à octobre ; `DEMO-KPI-RETARD` comporte un relevé mensuel d’août, et `DEMO-KRI-VIDE` n’a volontairement aucune valeur. Les relevés d’octobre et les cibles donnent cette lecture :

| Indicateur | Dernière valeur DEMO | Cible | Règle de lecture appliquée | Décision concrète |
|---|---:|---:|---|---|
| Disponibilité des systèmes (`DEMO-KPI-01`) | 98 % en octobre | 90 % | KPI atteint | Maintenir le dispositif ; ne pas déclencher de correction sur ce résultat seul. |
| Contrôles périodiques (`DEMO-KPI-02`) | 90 % en octobre | 95 % | KPI non atteint de 5 points | Programmer les contrôles manquants et vérifier leur clôture au prochain relevé. |
| Incidents de sécurité (`DEMO-KRI-01`) | 9 en octobre | 10 maximum | KRI atteint car une valeur plus faible est favorable | Suivre la tendance sans déclarer la cible dépassée. |
| Équipements critiques indisponibles (`DEMO-KRI-02`) | 4 en octobre | 5 maximum | KRI atteint | Maintenir le suivi de maintenance préventive. |
| Recommandations clôturées (`DEMO-KPI-RETARD`) | 86 %, dernière période en août | 90 % | KPI non atteint et saisie en retard | Relancer le responsable : la prochaine échéance mensuelle est le 30/09, soit environ 42 jours de retard au 12/11 selon l’heure du calcul. |
| Alertes non qualifiées (`DEMO-KRI-VIDE`) | Aucune valeur saisie | 3 maximum | Jamais saisi | Demander une première saisie ; l’absence de valeur ne signifie pas zéro alerte. |

Dans la synthèse, le résultat attendu pour les six DEMO est **3 atteints, 2 non atteints, 0 indéterminé et 1 jamais saisi**. Le compteur « en retard de saisie » est attendu à **2** parce qu’il rassemble le relevé en retard et l’indicateur jamais saisi. La capture doit porter la mention « Données fictives de démonstration ».

Chaque indicateur DEMO est désormais rattaché à plusieurs activités cohérentes (relation plusieurs-à-plusieurs, **19 liens** au total). Une même valeur d’indicateur est donc consultable depuis chacune des activités liées ; les relevés sont saisis par un utilisateur unique, rattaché à l’activité marquée auteur dans le script :

| Indicateur | Activités rattachées | Relevés |
|---|---|---:|
| `DEMO-KPI-01` | `DEMO-A-002`, `DEMO-A-006`, `DEMO-A-042`, `DEMO-A-046` | 3 |
| `DEMO-KPI-02` | `DEMO-A-005`, `DEMO-A-013`, `DEMO-A-025` | 3 |
| `DEMO-KRI-01` | `DEMO-A-004`, `DEMO-A-024`, `DEMO-A-044` | 3 |
| `DEMO-KRI-02` | `DEMO-A-002`, `DEMO-A-006`, `DEMO-A-042` | 3 |
| `DEMO-KPI-RETARD` | `DEMO-A-007`, `DEMO-A-027`, `DEMO-A-047` | 1 |
| `DEMO-KRI-VIDE` | `DEMO-A-008`, `DEMO-A-028`, `DEMO-A-048` | 0 |

Les rattachements croisés `DEMO-KPI-01`/`DEMO-KRI-02` sur `DEMO-A-002`, `006` et `042` montrent qu’un même dossier peut suivre à la fois un indicateur de performance et un indicateur de risque. Le script reste idempotent : il supprime uniquement les liens DEMO↔DEMO avant de les recréer.

### Exemple E — Utiliser la liste pour établir une file de travail

En recherchant `DEMO-A-` et en gardant l’année 2026, la liste statistique masque les trois DEMO en attente de validation et affiche **45 activités publiées**. Les compteurs de statut courant visibles sont : 11 terminées, 11 en cours, 15 non commencées, 2 suspendues, 3 reportées et 3 annulées. Le compteur de retard vaut **11** : il recoupe 5 activités en cours, 5 non commencées et 1 suspendue ; il ne s’ajoute pas au total des statuts.

**Décision concrète.** Le responsable peut exporter ou recopier cette file de travail (l’application n’offre pas d’export automatique repéré), commencer par les activités non commencées en retard, puis vérifier les échéances et priorités dans leurs fiches. Les activités en attente de validation ne doivent pas être considérées comme absentes du système : elles sont volontairement retirées de cette liste publiée.

### Exemple F — Suivre un membre sur une tâche précise

En filtrant le suivi collectif sur **Gestion des Systèmes de Sécurité**, puis sur **Nantenaina Rakotomalala**, le responsable retrouve son affectation à `DEMO-A-042`, ainsi qu’à six plans DEMO dont `DEMO-PA-006` et `DEMO-PA-018`. La fiche activité montre 70 % d’avancement et une échéance au 15/11/2026 ; ces deux plans sont non commencés et leurs échéances sont respectivement le 10/11 et le 08/11/2026 dans le jeu DEMO recalé. La décision est de confirmer la disponibilité de l’intervenante sur l’activité proche, puis de relancer ces deux plans en retard. Les totaux globaux par membre doivent être relevés dans l’application.

> **Précaution sur les chiffres affichés à l’écran.** Les comptes `DEMO-` ci-dessus sont des sous-totaux du script. Le dashboard peut aussi inclure d’autres activités déjà présentes dans la base. Les valeurs exactes d’une capture complète doivent donc être relevées dans l’application, et non remplacées par les seuls sous-totaux DEMO.

## 3. Fiches détaillées : calculs, décisions et vérifications

### E1 à E9 — Tableau de bord principal

**Besoin.** Le chef de département compare les services sur une période pour décider où mobiliser des agents et quelles échéances escalader. Sur les données DEMO, il doit distinguer les 12 retards des statuts terminaux et ne pas traiter le graphique en anneau comme une répartition exhaustive.

**Données et règles visibles dans le code.** Le frontend envoie une période commune aux blocs : année, mois, trimestre ou dates personnalisées. Le backend sélectionne les activités dont la période prévue **chevauche** la période choisie. Pour les statuts et avancements historiques, la date de référence est la fin de période, plafonnée à aujourd’hui. Le périmètre service/département est résolu côté serveur à partir de l’utilisateur connecté.

Les blocs distincts du tableau de bord sont :

1. KPI : total, en cours, terminées, en retard, non commencées, suspendues, annulées, reportées, en attente de validation et avancement global.
2. Avancement global : moyenne des avancements calculés pour les activités ; nombre d’activités et compteurs de sous-activités.
3. Répartition par statut : quatre catégories sont renvoyées dans le graphique : non commencées, en cours, en retard et terminées.
4. Par service : total, terminées, en cours, en retard, non commencées et avancement moyen.
5. PTA / non-PTA : deux agrégats séparés ; la classification PTA s’appuie sur la présence d’un objectif spécifique.
6. Par type : volume d’activités et avancement moyen.
7. Échéances : en retard et à échéance proche ; le tableau de bord demande par défaut une fenêtre de 7 jours.
8. Plans d’action : résumé de l’ensemble des plans non brouillon/non annulé, retards et échéances proches dans les 30 jours.
9. Plans par origine : volume et avancement moyen par incident, risque ou autre origine.

**Restitution.** Capture recommandée du tableau de bord avec période clairement indiquée, données cohérentes et plusieurs services/types visibles. Les graphiques doivent être accompagnés d’une légende et d’un commentaire interprétatif.

**Vérification effectuée sur la base de développement.** Le script confirme 48 activités et 144 sous-activités DEMO. Une requête directe sur les historiques donne 12 activités en retard, dont **8 échéances à moins de sept jours** au 12/11/2026 ; la moyenne des moyennes d’activité est 40,83 %. Ces résultats restent à comparer à l’API du dashboard sur la période choisie dans l’écran.

### E11 — Liste des activités et statistiques par statut

**Besoin.** Retrouver les activités d’un périmètre et analyser leur état selon plusieurs critères.

**Données et calculs.** Le backend sert une liste paginée et un endpoint statistiques séparé. Les mêmes critères de recherche et périmètre sont utilisés pour les deux. Les compteurs de statuts et la règle métier « en retard » sont calculés dans la requête statistique, non sur la page courante. L’interface sépare PTA et activités courantes.

**Point de période important.** Les filtres de la liste d’activités appliquent des bornes de dates prévues qui sont plus restrictives qu’un chevauchement simple : date de début prévue après/égale au début filtré et date de fin prévue avant/égale à la fin filtrée. Ce n’est pas la même définition que le tableau de bord ; il faut l’expliquer pour éviter de comparer directement les totaux.

**Vérification.** Rechercher `DEMO-A-` et sélectionner 2026 : la base contient 48 activités DEMO, dont 3 en attente de validation que la liste publiée masque ; le total visible attendu est donc 45. Parmi celles-ci, le jeu recalé comporte 11 retards visibles (5 en cours, 5 non commencées, 1 suspendue). Le document existant `documents/activites-liste.md` décrit également les contrôles historiques à reprendre.

### E12 — Suivi des activités

**Besoin.** Suivre les activités et modifier l’avancement de leurs sous-activités.

**Calculs visibles.** Les compteurs de statut proviennent de l’API de statistiques. En revanche, la valeur affichée comme « Avancement global » est calculée côté navigateur avec la moyenne des objets `activites` reçus sur la page courante. La page est paginée par 20 éléments : ce pourcentage est donc celui de la page, et non de toutes les activités qui correspondent aux filtres. Le total de sous-activités affiché est également celui des lignes chargées.

**Lecture / vérification.** Ne pas présenter cette valeur comme l’avancement global du département. Corriger le calcul pour obtenir l’agrégat serveur sur l’ensemble filtré, ou renommer clairement la valeur comme avancement moyen des activités affichées.

### E13 — Suivi collectif des membres

**Besoin.** Comparer la charge et la progression des membres, repérer ceux qui ont des retards et filtrer par période ou service.

**Restitution.** KPI de membres, graphiques de participation et de progression, tableau récapitulatif. Le graphique limite volontairement l’affichage aux 15 membres les plus chargés ; le tableau présente les membres filtrés. Les compteurs de la page respectent l’année/trimestre/période personnalisée et le service.

**Vérification.** Filtrer le service Systèmes de Sécurité et Nantenaina Rakotomalala, puis recouper l’affectation à `DEMO-A-042`, son échéance et ses trois relevés d’avancement avec la fiche activité. Les compteurs globaux du tableau par membre doivent être relus sur la base après exécution ; ils ne sont pas inventés dans ce rapport.

### E14 à E17 — Utilisateurs, indicateurs et signalements

- **Avancement utilisateur :** période choisie par année ; affiche nombre et statuts d’activités/plans, moyenne d’avancement, échéances et détails. Exemple : contrôler dans le profil de Nantenaina l’affectation à `DEMO-A-042` et son échéance du 15/11/2026.
- **Dashboard indicateurs :** la valeur courante est le **dernier relevé dont la période est terminée**, avec repli sur le relevé le plus récent si aucune période n’est clôturée ; le statut d’atteinte compare ensuite cette valeur à la cible selon le sens de l’indicateur : pour un KPI, valeur supérieure ou égale à la cible = atteint ; pour un KRI, valeur inférieure ou égale à la cible = atteint. Les seuils d’alerte sont informatifs et ne changent pas le statut d’atteinte. L’absence de valeur donne `JAMAIS_SAISI`, jamais zéro. Les six indicateurs DEMO sont liés à 19 activités ; `DEMO-KPI-01` et `DEMO-KRI-02` partagent notamment `DEMO-A-002`, `006` et `042`. Exemple DEMO : 98 % atteint pour `DEMO-KPI-01`, 90 % non atteint pour `DEMO-KPI-02`, 86 % en retard pour `DEMO-KPI-RETARD`, et `DEMO-KRI-VIDE` jamais saisi.
- **Liste des indicateurs :** la synthèse distingue total, KPI, KRI et autres ; filtres par type, fréquences et état. Pas de filtre de période.
- **Détail d’indicateur :** valeur courante, historique et calendrier de saisie. Le wrapper frontend de l’historique demande une seule page de 100 valeurs : les historiques plus longs peuvent être tronqués dans cet écran.
- **Signalements :** la page prévoit une synthèse par année, type et présence d’un plan d’action, mais le jeu DEMO ne contient pas de signalements et les scripts SQL inspectés ne définissent pas de table correspondante. Ne pas illustrer cet état avec des chiffres inventés.

## 4. Écarts et risques de conformité relevés

### Écarts par rapport aux instructions du mémoire

| Instruction | Constat dans le projet | État |
|---|---|---|
| Recenser tous les états, leurs utilisateurs et leurs questions | Plusieurs sorties sont identifiées ci-dessus, mais il faut confirmer les utilisateurs métier et les vues accessibles avec les droits réels. | À compléter dans le mémoire |
| Présenter au moins deux types d’états | Le projet contient des sorties de type tableau de bord, synthèse/analyse et opérationnel. | Couvert par le code ; à démontrer avec les fiches/captures |
| Fiche besoin, règles de calcul, restitution, interprétation, vérification | Des questions métier et exemples DEMO chiffrés sont maintenant rédigés ci-dessus. Il manque encore les captures réelles après chargement et le recoupement des chiffres dans la base. | Partiel ; compléter les preuves |
| Filtre de période sur les états | Présent au dashboard, à la liste d’activités et au suivi collectif. Absent de l’écran de suivi d’activités et du suivi des indicateurs ; certains autres états n’ont qu’un filtre annuel. | Partiel |
| Données réalistes en volume suffisant | Le script DEMO prévoit 48 activités, 144 sous-activités, 18 plans et 6 indicateurs, avec 13 relevés répartis sur plusieurs mois et 19 rattachements indicateur-activité ; ces volumes correspondent au jeu de test validé pour les démonstrations. | Conforme au jeu de test validé |
| Export ou format de restitution | Aucun export PDF/Excel de rapport n’a été trouvé dans les fichiers frontend/backend recherchés. Les références XLSX concernent l’import d’activités, pas un export d’état. | Écran uniquement pour les états inspectés |
| Vérification des chiffres | Tests identifiés pour l’avancement des plans par origine et pour le service de suivi membres ; tests d’intégration de détails d’activités. Pas de suite de tests de calcul couvrant l’ensemble des statistiques/dashboard repérée. | Partiel / à renforcer |

**Cohérence de l’agrégation d’indicateurs.** L’endpoint de synthèse par plage est exposé par l’API mais aucun écran du frontend ne le consomme. La méthode `DERNIERE` doit être corrigée : les relevés sont triés par période décroissante et `calculer()` renvoie le dernier élément de la liste, soit le plus ancien, alors que le commentaire attend le plus récent. Ce point ne modifie pas les exemples E14–E17, qui utilisent la valeur courante et non l’agrégation.

## 5. Fiches prioritaires à intégrer au mémoire

Pour exploiter le projet au mieux, rédiger d’abord des fiches complètes pour **E1 (dashboard, en isolant ses indicateurs lisibles), E3 (échéances), E11 (liste/statistiques), E13 (suivi collectif) et E10 (suivi des indicateurs)**. Ce choix couvre les quatre types d’états au lieu de multiplier uniquement les listes. Le catalogue ci-dessus conserve les autres sorties identifiées, conformément à la demande de ne pas limiter le recensement à 3–5.

Pour chaque fiche retenue :

1. nommer précisément l’utilisateur et la décision qu’il prend ;
2. indiquer tables/DTO, filtres, périmètre et règle de période ;
3. joindre une capture lisible avec période et mentionner si les données sont fictives ;
4. interpréter les chiffres observés sur cette capture, sans inventer de résultats ;
5. expliquer le contrôle effectué (recalcul manuel, comparaison à une liste ou à une autre synthèse).

## 6. Conclusion à adapter au mémoire

SAGA restitue des informations opérationnelles et de pilotage sur les activités, les échéances, les plans d’action, les membres et les indicateurs. Le jeu DEMO illustre des décisions concrètes : démarrer les activités en retard, sécuriser l’échéance de `DEMO-A-042`, relancer les mesures manquantes et escalader les plans issus d’audits. Les indicateurs sont partagés entre plusieurs activités cohérentes (19 liens), ce qui montre qu’une même mesure peut servir plusieurs dossiers sans dupliquer les relevés. Avant le dépôt du mémoire, il reste à relever les valeurs réellement affichées et à ajouter les captures annotées. Le donut des statuts et les différences entre avancement global, liste et sous-totaux DEMO doivent être explicités pour ne pas présenter des chiffres incomparables comme s’ils concordaient.

## 7. Fichiers principaux vérifiés

### Frontend

- `frontend/src/views/frontoffice/Dashboard.vue`
- `frontend/src/components/dashboard/DashboardKpiCards.vue`
- `frontend/src/components/dashboard/DashboardAvancement.vue`
- `frontend/src/components/dashboard/DashboardStatuts.vue`
- `frontend/src/components/dashboard/DashboardServices.vue`
- `frontend/src/components/dashboard/DashboardPta.vue`
- `frontend/src/components/dashboard/DashboardEcheances.vue`
- `frontend/src/components/dashboard/DashboardTypesActivite.vue`
- `frontend/src/components/dashboard/DashboardPlansAction.vue`
- `frontend/src/components/dashboard/DashboardPlansActionOrigine.vue`
- `frontend/src/composables/useDashboardPeriode.ts`, `frontend/src/services/dashboard.ts`, `frontend/src/types/dashboard.ts`
- `frontend/src/views/frontoffice/activites/Liste.vue`, `Suivi.vue`, `Detail.vue`, `SousActivite.vue`, `AValider.vue`, `Brouillons.vue`, `Ajout.vue`
- `frontend/src/views/frontoffice/membres/Suivi.vue`, `SuiviMembresGlobal.vue`, `frontend/src/views/frontoffice/utilisateurs/Avancement.vue`
- `frontend/src/views/frontoffice/indicateurs/List.vue`, `Suivi.vue`, `Show.vue`, `frontend/src/components/CalendrierValeurs.vue`
- `frontend/src/views/frontoffice/signalements/List.vue`
- `frontend/src/views/frontoffice/planaction/List.vue`, `Show.vue`
- Services/types associés, notamment `activite.ts`, `suivi.ts`, `membreSuivi.ts`, `indicateurFront.ts`, `planaction.ts` et `types/*`.

### Backend et documentation existante

- `backend/src/main/java/mg/bank/backend/controller/dashboard/DashboardController.java`
- `backend/src/main/java/mg/bank/backend/service/dashboard/DashboardService.java`
- `backend/src/main/java/mg/bank/backend/repository/dashboard/DashboardRepository.java`
- `backend/src/main/java/mg/bank/backend/repository/SqlFragments.java`
- `backend/src/main/java/mg/bank/backend/service/common/PeriodeSupport.java`
- `backend/src/main/java/mg/bank/backend/controller/activite/ActiviteController.java`
- `backend/src/main/java/mg/bank/backend/repository/activite/ActiviteRepository.java`
- `backend/src/main/java/mg/bank/backend/service/activite/ActiviteService.java`
- `backend/src/main/java/mg/bank/backend/controller/utilisateur/MembreSuiviController.java`
- `backend/src/main/java/mg/bank/backend/service/utilisateur/MembreSuiviService.java`
- `backend/src/main/java/mg/bank/backend/service/indicateur/SuiviIndicateurService.java`
- `backend/src/main/java/mg/bank/backend/service/planaction/SuiviGlobalPlanActionService.java`
- Tests trouvés : `DashboardServicePlanActionOrigineTests.java`, `MembreSuiviServiceTests.java`, `ActiviteDetailIntegrationTests.java`, `SousActiviteDetailIntegrationTests.java`.
- Documentation déjà présente : `documents/activites-liste.md` et `documents/activite-detail.md`.

> **Limite de vérification :** le script de démonstration a été exécuté sur la base de développement et ses volumes ainsi que plusieurs indicateurs de retard ont été contrôlés par SQL. Les calculs complets du dashboard n’ont pas été rapprochés d’une capture frontend ; les chiffres affichés restent à confirmer dans l’application avant leur insertion définitive dans le mémoire.
