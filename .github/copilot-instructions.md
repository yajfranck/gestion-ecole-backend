1. Pour chaque demande se baser sur la documentation du projet situé dans le README.md

2. Mentionner que  la documentation a été consulté afin de répondre à la question

3. la version de java dans le projet est 21

4. La version de java installé en local pour lancé l'application est java version "25.0.4" 2026-07-21 LTS

5. Je suis sur Mac Apple Silicon (arm64). J’ai plusieurs JDK installés : 17, 21, 22, 23 et 25 ; Java 25 est actuellement celui par défaut. le projet utilise java 21.

6. Les packages, noms de classes, varibale et fonctions doivent être en anglais

7. Toutes les répoonses doivent être en français.

8. A chaque nouveay endpoint ou aprs une modification d'un endpoit, mets a jour le fichier doc-api.md a la racine du projet dans lequel tu mettra la documentation detaillé du endpoint

9. A chaqye code ajouté ou modifié rassures toi que ce sont les bonnes pratiques qui sont appliquées

10. lors de l'implémentation d'un endpoint avec hasAuthority,  la nomenclature des permissions doit suivre le format can_<entity>_<action>
exemple : can_user_create
exemple : can_user_update
exemple : can_user_delete
exemple : can_user_read

11. Lors des implémentationns des endpoints, il faut mettres les classes controller dans un package a part; les dto (record) dans un autre package; de même pour la gestion des exceptions.

12. Apres chaque implémentation, lance les tests unitaires pour vérifier que tout fonctionne correctement.

PS: une fois le readme consulté , affiche consultation README TERMINÉ