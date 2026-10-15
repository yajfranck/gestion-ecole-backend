# Architecture

## Style architectural

L’application est un monolithe modulaire : une application Spring Boot, un seul artefact déployable et PostgreSQL comme base de données principale. Les fonctionnalités métier sont séparées en modules dans le package racine existant `com.galactique.gestionecole`.

## Modules métier

```text
com.galactique.gestionecole
├── students/
├── enrollments/
├── guardians/
├── schoolclasses/
├── employees/
├── positions/
├── attendance/
├── assessments/
├── tuition/
├── security/
├── shared/
└── GestionEcoleApplication.java
```

Ces modules constituent un premier découpage indicatif, et non un engagement à implémenter toutes ces fonctionnalités. Un module ne sera développé que lorsqu’un besoin concret le justifie.

## Structure interne d’un module métier

Créer ces sous-packages dans un module au moment d’implémenter son premier cas d’usage :

```text
<module>/
├── api/             # contrôleurs Spring MVC et DTO de requête/réponse
├── application/     # cas d’usage et frontières transactionnelles
├── domain/          # règles métier et modèle du domaine
└── infrastructure/  # repositories JPA et adaptateurs externes
```

`security` porte la configuration de l’authentification et de l’autorisation. `shared` est réservé aux éléments techniques réellement transverses; les concepts métier restent dans leur module propriétaire.

Le domaine de sécurité contient `UserAccount`, `Role` et `Permission`. Les entités JPA et les associations entre comptes, rôles et permissions sont isolées dans `security.infrastructure.persistence` et créées par Flyway. `POST /api/v1/login` reçoit `login` et `password`; il renvoie un JWT d'accès Bearer et un refresh token opaque, ou `401` si l'authentification échoue. `POST /api/v1/refresh` fait tourner le refresh token sans exiger de JWT d'accès. Les refresh tokens ont une durée de vie de 30 jours, sont stockés hachés en base et ne sont utilisables qu'une fois; le rejeu révoque leur famille. Les routes protégées sont sans état et attendent `Authorization: Bearer <token>`. BCrypt protège les mots de passe; les JWT HS256 expirent après 15 minutes et leur émetteur est validé. Le secret Base64 d’au moins 32 octets est fourni par la variable d’environnement `JWT_SECRET`.

Les rôles actifs exposent des autorités `ROLE_<CODE>` et leurs permissions exposent leur code directement. La création de comptes et leur attribution de rôles ne sont pas exposées par une API. V2 crée le rôle `ADMIN` et sa permission. V3 supprime le compte historique introduit par V2; le compte `admin-tech` de développement est recréé uniquement par la migration locale V4. En production, provisionner le compte séparément avec `scripts/provision-admin-tech.sql` après Flyway et avant d’ouvrir le trafic; fournir `ADMIN_PASSWORD_HASH` depuis le gestionnaire de secrets de la plateforme.

### Provisionnement manuel de `admin-tech` en production

Cette procédure est réservée au provisionnement initial ou aux interventions d’urgence. Exécuter les migrations Flyway communes, puis lancer le script une seule fois avant d’ouvrir le trafic applicatif.

Générer un hash avec le même encodeur BCrypt que l’application. Depuis la racine du projet, préparer le classpath et ouvrir JShell dans un terminal interactif :

```bash
./mvnw -Plocal dependency:build-classpath -Dmdep.outputFile=/tmp/gestion-ecole-classpath
jshell --class-path "$(cat /tmp/gestion-ecole-classpath)"
```

Dans JShell, saisir le mot de passe à l’invite. Il ne sera pas affiché :

```java
var console = System.console();
if (console == null) throw new IllegalStateException("Lancer JShell depuis un terminal interactif");
var password = console.readPassword("Mot de passe initial: ");
try {
		System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
				.encode(new String(password)));
} finally {
		java.util.Arrays.fill(password, '\0');
}
```

Transmettre uniquement le hash BCrypt au script, de préférence depuis le gestionnaire de secrets de la plateforme. Avec une variable d’environnement temporaire :

```bash
export ADMIN_PASSWORD_HASH='<hash BCrypt obtenu>'
psql "service=gestion-ecole-prod" --no-psqlrc --set=ON_ERROR_STOP=1 \
	--file=scripts/provision-admin-tech.sql
unset ADMIN_PASSWORD_HASH
```

Le script vérifie le format BCrypt, la présence du rôle `ADMIN` actif et l’absence du compte, puis crée `admin-tech` et son association au rôle dans une transaction. Il refuse de remplacer un compte existant. Ne jamais inscrire le mot de passe ou son hash dans une migration, les arguments d’une commande ou les logs. Conserver le mot de passe initial dans le gestionnaire de secrets afin de pouvoir se connecter.

## Base locale

Le profil Spring `local` utilise une base H2 en mémoire en mode de compatibilité PostgreSQL et applique les migrations communes (`db/migration`) ainsi que les migrations de développement (`db/migration-local`). Les autres profils n’appliquent que `db/migration`. Les données H2 sont perdues à l’arrêt de l’application. Le pilote H2 est limité au profil Maven `local`. Pour démarrer avec les deux profils : `./mvnw -Plocal spring-boot:run -Dspring-boot.run.profiles=local`. La clé JWT de repli de ce profil est réservée au développement local; utiliser `JWT_SECRET` pour la remplacer et ne jamais réutiliser cette valeur en production.

## Personnel et établissements

Le module `establishments` possède les établissements; `students` gère les dossiers élèves et leur archivage logique par établissement. Le numéro d’élève est unique dans son établissement. Les endpoints `/api/v1/establishments/{establishmentId}/students` utilisent les permissions `can_student_read`, `can_student_create`, `can_student_update` et `can_student_delete`. `schoolclasses` possède le calendrier configurable ainsi que le catalogue de niveaux, classes et matières; les codes et noms de niveaux sont uniques par établissement, tandis que les classes appartiennent à une année et un niveau du même établissement, avec code unique par année et nom unique par niveau. `SchoolClassSubject` associe les matières aux classes sans doublon. Les affectations d’enseignants et emplois du temps sont hors de cette tranche. `employees` gère les fiches de personnel et les invitations, tandis que `positions` gère le catalogue des postes par établissement et expose ses endpoints via `PositionController`. Chaque `Position` appartient à un établissement. `POST /api/v1/employees` crée une fiche rattachée à un poste par `positionId` et envoie une invitation SMTP. L’acceptation crée le compte de sécurité avec BCrypt. Les tokens d’invitation sont aléatoires, stockés uniquement sous forme SHA-256, valables 48 heures, à usage unique et transmis depuis le fragment de l’URL d’invitation.

Les rôles effectifs d’un membre sont l’union de ses rôles directs et des rôles attribués à son poste. Modifier les rôles d’un poste synchronise les comptes actifs de ses titulaires sans perdre leurs rôles directs. Les routes positions vérifient les permissions `can_position_read`, `can_position_create`, `can_position_update`, `can_position_assign_roles` et `can_position_delete`, attribuées à `ADMIN` et `ESTABLISHMENT_ADMIN`; les autres routes utilisent les permissions `ADMIN_ACCESS`, `ESTABLISHMENT_STAFF_MANAGEMENT` ou `STAFF_ACCESS`. Les administrateurs d’établissement sont limités à leur établissement; ils peuvent attribuer `STAFF` et `ESTABLISHMENT_ADMIN` à un poste, mais ne peuvent pas attribuer directement `ESTABLISHMENT_ADMIN` à un membre. Le rôle global `ADMIN` n’est jamais attribuable par les API Employee. `GET`, `POST`, `PATCH`, `PUT .../roles` et `DELETE` sous `/api/v1/establishments/{establishmentId}/positions` gèrent les postes; un poste rattaché à des membres ne peut pas être désactivé. `PATCH /api/v1/employees/{id}` peut changer le poste dans le même établissement; `DELETE` désactive la fiche et le compte sans supprimer l’historique. Désactiver un établissement désactive aussi ses membres et leurs comptes et révoque leurs refresh tokens.

Le module `enrollments` conserve les inscriptions par établissement, élève, année et classe. Une contrainte unique garantit une classe par élève et par année; les inscriptions restent en base pour préserver l’historique. La condition de paiement est configurable par établissement (`NONE`, `PARTIAL_PAYMENT`, `FULL_PAYMENT`); une inscription peut rester `PENDING_PAYMENT` jusqu’à ce que le pont de facturation confirme le paiement requis.

Le module `guardians` gère les comptes parent et les liens tuteur-élève. Une adresse e-mail correspond à un compte parent, qui peut être associé à plusieurs élèves. L’activation passe par une invitation à usage unique; le portail parent ne retourne que les élèves associés au compte authentifié et rattachés à un établissement actif.

Le module `tuition` définit des frais par établissement et année, éventuellement ciblés sur un niveau ou un élève. La définition est appliquée aux inscriptions actives pour créer des créances immuables avec snapshots de l’élève, du montant, de la devise et de l’échéance. Les paiements partiels sont immuables, enregistrés sous verrou de créance pour empêcher le surpaiement concurrent, et génèrent un numéro de reçu unique. Les soldes sont dérivés de l’historique des paiements; les méthodes acceptées sont configurables par établissement.

Les migrations V7 à V10 normalisent les anciens intitulés, renomment le schéma vers `positions` et `employees`, puis attribuent les permissions spécifiques à `positions`; V11 crée le registre élèves et ses permissions; V12 crée les années scolaires et périodes; V13 crée le catalogue de niveaux, classes, matières et leurs permissions; V14 crée les inscriptions et leurs contraintes de périmètre et d’unicité. La configuration SMTP accepte `EMPLOYEE_INVITATION_FROM` et `EMPLOYEE_INVITATION_ACCEPT_URL`; les noms historiques `STAFF_INVITATION_FROM` et `STAFF_INVITATION_ACCEPT_URL` restent des valeurs de repli.

V2 crée le rôle global `ADMIN` et sa permission. V3 supprime le compte historique introduit par V2; le compte `admin-tech` de développement est recréé uniquement par la migration locale V4. En production, provisionner le compte séparément avec `scripts/provision-admin-tech.sql` après Flyway et avant d’ouvrir le trafic; fournir `ADMIN_PASSWORD_HASH` depuis le gestionnaire de secrets de la plateforme.

## Règles de dépendance

- `api` délègue à `application` et ne contient pas de règles métier.
- `application` orchestre le domaine et définit les frontières transactionnelles.
- `domain` ne dépend ni de Spring MVC, ni de la persistance, ni de l’infrastructure d’un autre module.
- `infrastructure` implémente la persistance et les intégrations propres au module.
- Un module n’accède pas directement aux repositories ou aux tables d’un autre module. La collaboration intermodule passe par une interface explicite côté application; les événements internes sont réservés aux besoins de découplage asynchrone.
- Les évolutions du schéma de base de données sont gérées par des migrations Flyway.

## Tests

Les tests restent sous la racine existante `src/test/java/com/galactique/gestionecole` et reprennent les noms des modules de production. Tester séparément les règles du domaine, les cas d’usage avec des tests ciblés, et les frontières HTTP ou de persistance par des tests d’intégration lorsque nécessaire.