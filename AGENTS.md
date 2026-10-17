# Instructions du projet

## Documentation du projet

- Pour toute demande concernant ce projet, consulte d’abord le `README.md` à la racine et appuie-toi sur sa documentation.
- Après avoir consulté le README, affiche exactement :
  `CONSULTATION README TERMINÉ`
- Indique dans ta réponse que tu as consulté la documentation du projet.
- Si le README est absent ou ne contient pas l’information nécessaire, signale-le clairement et vérifie les fichiers pertinents du projet.

## Java et environnement local

- La version de Java utilisée par le projet est **Java 21**.
- L’environnement local est un **Mac Apple Silicon (arm64)**.
- Plusieurs JDK sont installés : 17, 21, 22, 23 et 25. Java 25 est actuellement le JDK par défaut.
- Pour compiler, tester ou lancer le projet, vérifie et utilise **Java 21**. Ne suppose pas que le JDK par défaut convient au projet.

## Langue et conventions de nommage

- Rédige toutes les réponses en français.
- Écris en anglais les noms de packages, classes, variables, méthodes et fonctions.
- Respecte les conventions de nommage déjà présentes dans le projet.

## Endpoints et documentation API

- Lors de la création ou de la modification d’un endpoint, mets à jour `doc-api.md`, situé à la racine du projet.
- Décris chaque endpoint de façon détaillée : méthode HTTP, URL, paramètres, corps de requête, réponse, codes d’erreur, règles de sécurité et exemples utiles.
- Pour les permissions utilisées avec `hasAuthority`, respecte le format :
  `can_<entity>_<action>`
- Exemples : `can_user_create`, `can_user_update`, `can_user_delete`, `can_user_read`.

## Organisation du code et qualité

- Pour les endpoints, place les contrôleurs, les DTO (records) et la gestion des exceptions dans des packages distincts.
- Respecte l’architecture et les conventions existantes du projet.
- Pour chaque ajout ou modification de code, applique les bonnes pratiques adaptées au projet : validation des entrées, gestion des erreurs, sécurité et tests pertinents.
- Limite les changements au besoin demandé et évite d’ajouter une dépendance sans nécessité.