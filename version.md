# 4.0.0
* https://redmine.okina.fr/issues/5097 : Ajout récupération Reflex mapping ZDEP/ZDLR
* Continuer le traitement sur une erreur de récupération ZDEP
* Fix problème encodage
* Remontée des erreurs SQL

# 4.0.1
* 5913 : Ajout d'un nouveau bouton "Modifier les lignes" pour pouvoir catégoriser en masse des lignes en IDFM.
* 5957 : Arrêt de l'import après la validation de niveau 1 (précédemment un bouton avait été rajouté pour permettre la validation de l'offre et la proposer dans un super espace de données.
* 5959 : Un petit flag IDFM est rajouté devant chaque ligne marquée IDFM
* 5977 : Amélioration de la sécurité du service de chargement des plages zdep
* 5872 : correctifs du NeTex Arrêt
* 5883 : A l'import la page de la liste des imports se rafraîchit automatiquement pour mettre à jour le statut du job d'import
* 5884 : Affichage dans une page des fichiers importés comme dans Ninkasi + option de réimport
* 5911 : Filtre de la liste des imports / exports selon une période
* 5954 : Doublon de la 5884 : les 2 tâches étaient proches et se sont recoupées
* 5354 : Génération NeTex Offre
* 6002 : Refactorisation / amélioration du code 5354
* 5208 : Une évolution a été prévue dans le sprint suivant afin d'alerter des "trous" dans une offre
* 5958 : Export concerto au format csv
* 6056 : Interface Okina avec export concerto

# 4.0.2
* 6177 : Correction des erreurs de syntaxe sur l'export Netex
* 6132 : Remonter une erreur si pas de Codifligne sur l'export Netex

# 4.0.3
* 6154 : Stockage des fichiers exportés sur le filesystem et/ou Google Cloud Storage
* 6258 : Réactivation de la validation offre Netex export IDFM
* 6314 : Correction retour Offre Netex IDFM
* Renommage des fichiers d'exports Netex offre
* Filtre INVALID_DATA sur le résultat des exports

# 4.0.4
* 6432 : Gestion du non-écrasement des données à l'import GTFS IDFM
* 6382 : Fix Netex Offre IDFM
* 6419 : Fusion des lignes versionnées dans routes.txt
* 6452 : Import des PA avec coordonnées différentes
