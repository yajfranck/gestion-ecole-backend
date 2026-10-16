# Plateforme de gestion des établissements scolaires privés

> **Document de cadrage fonctionnel et technique**  
> Version 1.0 — 14 octobre 2026  
> État : spécifications de référence; l'implémentation du backend est en cours.

## 1. Vision du produit

Construire une plateforme web et mobile permettant aux établissements scolaires privés de gérer leurs activités administratives et pédagogiques, de communiquer avec les familles et de proposer des cours à distance.

La solution est conçue comme un **SaaS multi-établissements**. Chaque école dispose de son espace, de ses utilisateurs, de ses élèves, de ses réglages et de ses données. L’éditeur de la plateforme gère les offres commerciales et les paramètres de référence.

### Objectifs

- Centraliser les informations de l’école, des élèves, des familles et du personnel.
- Simplifier les inscriptions, le suivi des présences et les paiements scolaires.
- Donner aux parents un accès mobile aux informations et services auxquels ils ont souscrit.
- Permettre l’organisation de cours à distance.
- Adapter la solution aux différents pays : programmes, règles de notation, devises et moyens de paiement.
- Fournir aux directions une vue claire sur les effectifs, les impayés et l’activité scolaire.

### Public cible

- Écoles maternelles, primaires et secondaires privées.
- Directions, administrateurs, secrétariats, comptables et enseignants.
- Parents ou tuteurs légaux et élèves, selon leur âge et les règles de l’établissement.
- Super administrateurs de la plateforme.

## 2. Principes de fonctionnement

### 2.1 Multi-établissements

Chaque établissement est un tenant logique. Les données métier doivent toujours être rattachées à un établissement et les accès doivent être contrôlés côté serveur. Un utilisateur peut être membre de plusieurs établissements avec des rôles différents.

Un établissement peut éventuellement gérer plusieurs sites ou campus. Cette option peut être activée selon l’offre souscrite.

### 2.2 Paramètres configurables

Le super administrateur définit les paramètres de référence et les valeurs disponibles. L’administrateur d’école choisit et ajuste les paramètres autorisés pour son établissement.

| Paramètre | Référence plateforme | Réglage établissement |
|---|---|---|
| Pays et programmes scolaires | Catalogue de programmes | Programme applicable |
| Notation | Barèmes et règles de calcul disponibles | Barème, coefficients et règles autorisés |
| Devise | Devises prises en charge | Devise de facturation de l’école |
| Paiements | Fournisseurs et méthodes disponibles | Méthodes acceptées par l’école |
| Année et calendrier | Modèles éventuellement proposés | Années, périodes, dates et jours fériés |
| Frais scolaires | Aucune grille imposée | Frais, échéances, remises et pénalités |
| Modules et capacités | Catalogue de fonctionnalités | Modules actifs selon le forfait |

Les paramètres sont versionnés lorsque leur modification pourrait changer le calcul ou l’interprétation de données historiques. Les notes et paiements déjà enregistrés doivent conserver leur contexte d’origine.

### 2.3 Deux types de paiement à séparer

1. **Abonnement logiciel** : paiement de l’école à l’éditeur de la plateforme pour l’utilisation du SaaS.
2. **Frais scolaires** : paiement du parent à l’école pour la scolarité, l’inscription, le transport ou d’autres services de l’établissement.

Ces flux ont des factures, des reçus, des comptes de suivi et des rapprochements distincts. Un paiement scolaire ne règle pas l’abonnement logiciel, et inversement.

## 3. Rôles et droits

Les permissions sont attribuées par rôle et limitées au périmètre de l’établissement concerné.

## Configuration SMTP Yahoo

Les invitations du personnel utilisent Yahoo SMTP (`smtp.mail.yahoo.com`, port `587`, STARTTLS). L’identifiant par défaut est `franckyaj@yahoo.com`. Le mot de passe n’est pas stocké dans le dépôt : fournir un mot de passe d’application Yahoo dans `SMTP_PASSWORD`.

Avant de lancer l’application, définir le secret dans le shell :

```sh
export SMTP_PASSWORD='<mot-de-passe-application-yahoo>'
```

Les paramètres `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_AUTH`, `SMTP_STARTTLS` et `EMPLOYEE_INVITATION_FROM` peuvent être surchargés par l’environnement. Ne pas inscrire de mot de passe dans les fichiers versionnés.

| Rôle | Principales responsabilités |
|---|---|
| Administrateur plateforme (`ADMIN`) | Consulter la liste des établissements, créer un établissement et inviter son premier administrateur d’école |
| Administrateur d’école | Configurer l’école, gérer les rôles et permissions locaux, les années, classes, utilisateurs, modules et paramètres |
| Direction | Consulter les tableaux de bord et rapports, superviser les opérations |
| Secrétariat | Gérer les dossiers, inscriptions, classes et communications administratives |
| Comptable / caissier | Gérer les frais scolaires, encaissements, reçus, échéanciers et impayés |
| Enseignant | Consulter ses classes, gérer les présences et, selon les modules activés, les cours et évaluations |
| Parent / tuteur | Consulter les informations de ses enfants et utiliser les services activés |
| Élève | Accéder aux contenus ou cours autorisés, si l’établissement active cet accès |

Le modèle doit permettre de préciser les droits fins sans multiplier inutilement les rôles prédéfinis. Les actions sensibles (remboursement, changement de droits, clôture d’année, modification d’un paiement) doivent être journalisées.

## 4. Périmètre fonctionnel

### 4.1 Gestion de l’établissement

- Création et paramétrage d’un établissement.
- Informations légales, contacts et logo.
- Sites ou campus si l’offre le permet.
- Années scolaires, périodes, classes et matières.
- Calendrier scolaire et jours non ouvrés.
- Emplois du temps quotidiens et hebdomadaires par classe, avec modèles réutilisables.
- Gestion des utilisateurs, invitations, rôles et désactivation d’accès.

Un emploi du temps regroupe des créneaux récurrents associés à une classe, une matière, un enseignant, une salle, un jour de la semaine et des heures de début et de fin. Il est consultable à la journée ou à la semaine. Un modèle réutilisable peut être appliqué à une classe puis ajusté. L’application refuse les chevauchements pour une même classe, un même enseignant ou une même salle. Les jours non ouvrés du calendrier scolaire sont pris en compte. Ces emplois du temps récurrents sont distincts des séances ponctuelles de cours à distance.

### 4.2 Élèves, parents et inscriptions

- Fiche élève avec numéro interne généré automatiquement et informations utiles à l’établissement. Le numéro est unique et immuable.
- Un ou plusieurs parents/tuteurs par élève.
- Un parent/tuteur peut être lié à plusieurs enfants.
- Inscription à une année et une classe.
- Historique des inscriptions et changements de classe.
- Statut du dossier : préinscrit, actif, transféré, sorti ou archivé.
- Import initial de données depuis un fichier tabulaire et export autorisé.

### 4.3 Frais scolaires et encaissements

- Catalogue de types de frais configurable par établissement; chaque frais est associé à un type actif.
- Définition des frais par année, classe ou élève.
- Échéances, remises et ajustements autorisés.
- Enregistrement des paiements et paiements partiels.
- Modes de paiement configurables par l’école, avec libellés modifiables et désactivation logique sans effacer l’historique.
- Reçu numéroté, historique des opérations et solde restant dû.
- Liste des impayés et relances.
- Exports de suivi comptable.
- Annulation ou correction par opération tracée, sans effacer l’historique.

L’intégration directe à un opérateur de mobile money ou à une banque dépend du pays et du fournisseur choisi. Pour le MVP, une saisie manuelle contrôlée suffit ; l’intégration pourra venir ensuite.

### 4.4 Présences et absences

- Appel des élèves par classe et séance.
- Statuts : présent, absent, retard et absence justifiée.
- Commentaire ou motif selon les droits.
- Historique par élève et synthèse par classe.
- Notification au parent selon la configuration de l’école et les canaux disponibles.

### 4.5 Cours à distance

Le module doit permettre aux enseignants de planifier et de communiquer des cours à distance. Pour le MVP, les visioconférences reposent sur un lien externe (par exemple un outil de visioconférence choisi par l’école), afin de ne pas construire immédiatement une infrastructure vidéo.

- Planification d’une séance liée à une classe, une matière et un enseignant.
- Date, heure, durée, instructions et lien de visioconférence.
- Partage de documents ou ressources pédagogiques.
- Affichage des cours à venir et passés dans l’espace parent/élève autorisé.
- Suivi simple de participation : présence saisie par l’enseignant ou déclaration contrôlée.
- Accès au lien réservé aux personnes autorisées.
- Enregistrement du lien de replay ou d’une ressource après le cours, si l’école en fournit une.
- Notifications de création ou de modification d’une séance.

Les enregistrements vidéo, la visioconférence intégrée, le suivi automatique de connexion et les fonctions avancées de classe virtuelle sont hors MVP. Les liens et les documents doivent respecter les droits d’accès et les règles de conservation de l’établissement.

### 4.6 Communication

- Annonces de l’établissement, d’une classe ou d’un groupe ciblé.
- Notifications dans l’application.
- Préparation à l’envoi par SMS, courriel ou messagerie via des fournisseurs externes.
- Historique minimal des notifications importantes et statut d’envoi lorsque le fournisseur le retourne.

### 4.7 Notes et bulletins

Le modèle prévoit les matières, évaluations, notes, coefficients, périodes et règles de calcul configurables. Ce module peut être livré après le premier MVP si les règles locales de notation ne sont pas encore cadrées. Il ne faut pas intégrer de barème national en dur dans le code.

### 4.8 Tableaux de bord et rapports

- Effectifs par année et classe.
- Suivi des inscriptions.
- Présences et absences.
- Frais facturés, paiements reçus et soldes dus.
- État de l’abonnement logiciel de l’établissement.
- Export CSV ou tableur selon les droits.
- Filtres par période, classe, campus et statut.

## 5. Abonnements, licences et services payants

### 5.1 Recommandation de modèle commercial

- L’établissement souscrit le forfait logiciel principal.
- Le forfait peut combiner un prix de base, une tranche d’élèves actifs, des modules, un nombre de sites et éventuellement un quota de personnel.
- Les comptes parents et élèves sont de préférence inclus par défaut, pour que les familles puissent accéder aux informations essentielles.
- Des services additionnels destinés aux parents peuvent être facturés séparément, uniquement s’ils sont réellement optionnels et clairement expliqués.
- L’accès aux informations essentielles relatives à la scolarité ne doit pas dépendre d’un service payant optionnel.
- Les tarifs et taxes doivent pouvoir être configurés par pays et devise.

### 5.2 Entités commerciales

- **Offre / Plan** : modules inclus, limites, tarif, périodicité, pays ou devise applicables.
- **Abonnement** : établissement, offre, dates, état, renouvellement et conditions appliquées.
- **Licence / droit d’accès** : capacités accordées par l’abonnement (élèves actifs, sites, modules, personnel).
- **Utilisation** : décompte ou instantané périodique des quotas consommés.
- **Facture et paiement SaaS** : montant dû à l’éditeur, échéance, mode et référence du paiement.
- **Service parent optionnel** : service activable pour un parent ou un élève, avec prix, période et consentement.
- **Événement d’audit** : changements d’offre, de limites, de statut ou d’accès.

Le nombre d’élèves facturés doit être calculé selon une règle explicite, par exemple les élèves avec une inscription active pendant une période de facturation. Les dossiers archivés ou préinscrits ne doivent pas être comptés sans décision commerciale documentée.

### 5.3 Cycle de vie de l’abonnement

États possibles : TRIAL, PENDING_PAYMENT, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED, EXPIRED.

- L’activation intervient après validation du paiement ou octroi explicite d’une période d’essai.
- Avant échéance, envoyer des rappels selon une politique configurable.
- Après échéance, appliquer un délai de grâce défini au contrat.
- En cas d’impayé persistant, restreindre les actions d’écriture après notification.
- Préserver au minimum un accès de consultation ou d’export pour permettre à l’école de récupérer ses données, sous réserve des règles contractuelles et légales.
- Journaliser les changements de statut et toute action manuelle de l’opérateur.

Pour la première version, l’école peut payer par virement, dépôt ou mobile money suivi manuellement par l’opérateur. Les paiements récurrents et l’intégration de paiement en ligne sont une phase ultérieure.

## 6. MVP — première version exploitable

Le MVP doit permettre à une école pilote de gérer ses opérations essentielles et à l’éditeur de contrôler l’accès au SaaS.

### Inclus dans le MVP

1. Création d’un établissement et configuration de base.
2. Gestion d’une année scolaire, des classes et des utilisateurs.
3. Gestion des élèves, parents/tuteurs et inscriptions, actives dès leur création indépendamment du paiement.
4. Paramétrage de la devise, des types de frais, des frais scolaires et des modes de paiement.
5. Enregistrement des paiements scolaires, reçus et soldes impayés.
6. Appel des présences par classe.
7. Espace parent web responsive ou mobile permettant de consulter les informations de ses enfants, présences, frais et annonces.
8. Planification de cours à distance avec lien externe, ressources et suivi simple de présence.
9. Abonnements SaaS avec offre, quotas, état et paiement manuel suivi par l’opérateur.
10. Tableau de bord simple et export des données principales.
11. Gestion des rôles, isolation des données entre écoles, journal d’audit des actions sensibles.
12. Paramètres de référence et création d’un établissement avec invitation de son administrateur par `ADMIN`.
13. Emplois du temps hebdomadaires par classe, avec modèles réutilisables, vues quotidiennes/hebdomadaires et détection des conflits.

### Hors MVP / phases ultérieures

- Paiements automatiques intégrés à plusieurs banques ou opérateurs mobile money.
- Facturation récurrente automatisée de l’abonnement.
- Visioconférence intégrée et enregistrement vidéo hébergé.
- Notes, bulletins et calculs nationaux complexes, si les règles ne sont pas validées pour le premier pays pilote.
- Comptabilité générale complète, paie, transport, bibliothèque, cantine ou gestion des stocks.
- Mode hors ligne avancé avec synchronisation conflictuelle.
- Architecture microservices, orchestration Kubernetes ou analytique avancée.

### Critères d’acceptation MVP

- Une école ne peut consulter ni modifier les données d’une autre école, y compris en manipulant directement les identifiants d’une requête.
- Un utilisateur sans permission ne peut pas exécuter l’action correspondante via l’API.
- Le secrétariat peut inscrire un élève à une classe et associer ses tuteurs.
- L’inscription à une classe est effective immédiatement; le solde des frais reste suivi indépendamment.
- Le comptable peut enregistrer un paiement partiel et le solde dû est recalculé correctement.
- Un reçu peut être retrouvé et exporté sans modifier l’historique du paiement.
- Un enseignant peut enregistrer l’appel d’une classe et le parent autorisé voit l’information.
- L’administrateur peut appliquer un modèle d’emploi du temps à une classe et consulter son planning hebdomadaire; un créneau incompatible avec la classe, l’enseignant ou la salle est refusé.
- Un enseignant peut programmer un cours à distance ; seuls les utilisateurs autorisés voient son lien et ses ressources.
- L’opérateur peut activer, suspendre ou faire expirer un abonnement et consulter les quotas appliqués.
- Les paramètres locaux sont contrôlés par les valeurs et règles permises par la plateforme.
- Les erreurs importantes et opérations sensibles sont traçables.

## 7. Architecture technique cible

### 7.1 Choix d’architecture

Commencer par un **monolithe modulaire** côté serveur : une seule application Spring Boot déployable, organisée par domaines métier. Cette forme garde le déploiement et le développement simples tout en isolant les modules. Une extraction en services séparés ne sera envisagée que si un besoin opérationnel mesuré le justifie.

~~~mermaid
flowchart TB
    Staff["Angular — administration et équipe"] --> API["API REST Spring Boot"]
    Parents["Flutter — parents et élèves"] --> API
    API --> DB[("PostgreSQL")]
    API --> Files["Stockage de fichiers"]
    API --> IdP["Identité OIDC"]
    API --> Adapters["Adaptateurs : paiement, SMS, visioconférence"]
~~~

### 7.2 Technologies

| Couche | Technologie cible | Usage |
|---|---|---|
| API backend | Java 21 + Spring Boot 4.1.x | API REST, règles métier, sécurité, traitements |
| Interface équipe | Angular | Administration, direction, secrétariat, comptabilité, enseignants |
| Application mobile | Flutter | Parents et élèves ; notifications et consultation mobile |
| Base relationnelle | PostgreSQL | Données métier, transactions et isolation des tenants |
| Migrations | Flyway | Évolution versionnée du schéma |
| Authentification | OIDC / OAuth 2.0, fournisseur compatible | Connexion, émission et validation des jetons |
| Fichiers | Stockage objet compatible S3 ou service géré | Documents pédagogiques, avatars et pièces jointes |
| Déploiement initial | Conteneur ou service applicatif géré | Environnements de développement, test et production |

Le fournisseur d’identité, de stockage, de paiement, de SMS et de visioconférence reste interchangeable derrière des interfaces d’intégration. Le choix final dépendra des pays visés, des coûts, de la disponibilité et des obligations locales.

### 7.3 Initialisation Spring Boot

Sélection recommandée dans Spring Initializr :

| Champ | Valeur initiale |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.1 stable (version proposée par Initializr au 14 octobre 2026) |
| Group | com.monentreprise (à remplacer par le domaine réel) |
| Artifact / Name | gestion-ecoles-api |
| Package name | com.monentreprise.gestionecoles |
| Packaging | Jar |
| Configuration | YAML |
| Java | 21 (Java 25 est aussi envisageable si tout l’environnement cible est compatible) |

Dépendances initiales recommandées :

- Spring Web MVC / Web — API REST.
- Spring Data JPA — persistance relationnelle.
- PostgreSQL Driver — connexion PostgreSQL.
- Validation — validation des entrées.
- Flyway Migration — migrations SQL versionnées.
- Spring Security — autorisation et protection de l’API.
- OAuth2 Resource Server — validation de jetons OIDC/JWT, si l’authentification est déléguée à un fournisseur OIDC.
- Spring Boot Actuator — santé et métriques opérationnelles.
- Lombok — optionnel ; ne l’ajouter que si l’équipe souhaite l’utiliser.

Au 14 octobre 2026, Spring Boot 4.1.1 est stable ; Java 21 est compatible et Maven 3.6.3 ou plus récent est pris en charge. Selon la version sélectionnée dans Initializr, le starter web peut apparaître sous le nom Spring Web MVC. Garder les versions de dépendances gérées par Spring Boot et éviter de définir des versions individuellement sans raison documentée.

### 7.4 Organisation du backend

#### Migrations et données locales

Les migrations initiales sont regroupées avant le premier déploiement :
- `db/migration/V1__initial_schema_and_reference_data.sql` initialise le schéma et les données de référence, sans compte utilisateur ni données métier de test.
- `db/migration-local/V2__seed_local_admin_account.sql` crée uniquement le compte de développement `admin-tech` avec son rôle `ADMIN`.

Le jeu de test VOGT a été retiré : aucun établissement, élève ou licence de test n'est inséré au démarrage.
Le profil `local` utilise H2 en mémoire : arrêter puis redémarrer le processus recrée la base avec ces migrations.
Une base persistante ayant exécuté les anciennes migrations n'est pas compatible avec cette nouvelle baseline : utiliser une base neuve, sans contourner les contrôles Flyway ni supprimer automatiquement des données.
Démarrage : `./mvnw -Plocal spring-boot:run -Dspring-boot.run.profiles=local`.

#### Structure des modules

Organisation proposée par domaines fonctionnels, à ajuster à mesure que le code évolue :

    com.monentreprise.gestionecoles
    ├── common          # erreurs, pagination, types partagés
    ├── config          # configuration de l'application
    ├── security        # identité, autorisations, contexte du tenant
    ├── school          # établissements, campus, réglages
    ├── academic        # années, périodes, classes, matières
    ├── identity        # utilisateurs, membres, rôles
    ├── student         # élèves, parents et liens familiaux
    ├── enrollment      # inscriptions et historique scolaire
    ├── attendance      # présences et absences
    ├── finance         # frais scolaires, paiements, reçus et impayés
    ├── subscription    # offres, abonnements, licences et quotas SaaS
    ├── learning        # cours à distance et ressources pédagogiques
    ├── notification    # annonces, notifications et connecteurs
    ├── reporting       # tableaux de bord et exports
    └── audit           # historique des opérations sensibles

Chaque domaine peut contenir ses propres api/controller, application/service, domain et infrastructure/repository. Les contrôleurs exposent des DTO plutôt que les entités JPA. Les règles métier ne doivent pas dépendre directement d’un fournisseur de paiement ou de visioconférence.

Dans le backend actuel, les modules sont sous `com.galactique.gestionecole` et suivent ces frontières. Le portail parent expose des informations de l’enfant après vérification du lien actif; `learning` gère les leçons/devoirs publiés et leurs pièces jointes en stockage privé; `licenses` gère les offres, périodes, suspensions et entitlements par établissement. La baseline inclut l’offre de référence legacy, sans attribuer de licence en l’absence d’établissement. Tout nouvel établissement nécessite une attribution explicite; aucun prix, quota ou essai n’est défini par défaut. Le contrat HTTP détaillé est maintenu dans `doc-api.md` et `swagger.yaml`.

### 7.5 Modèle de données de départ

Entités probables à préciser pendant la conception :

- School, Campus, SchoolSetting
- AcademicYear, Term, Level, Classroom, Subject
- User, SchoolMembership, Role, Permission
- Student, Guardian, StudentGuardian, Enrollment
- FeeDefinition, StudentCharge, Payment, Receipt
- Plan, Subscription, Entitlement, UsageSnapshot, SaasInvoice
- CourseSession, CourseMaterial, RemoteAttendance
- Announcement, Notification, AuditEvent

Les entités appartenant à une école portent une référence d’établissement (school_id). Les relations et requêtes doivent être bornées par ce périmètre, vérifiées dans les services et protégées par des tests d’autorisation. Une stratégie de filtrage ou de Row-Level Security en base peut compléter ces contrôles, mais ne les remplace pas.

## 8. Sécurité, qualité et exigences non fonctionnelles

- Authentification centralisée avec OIDC lorsque le fournisseur est choisi ; mots de passe et sessions ne doivent pas être gérés de façon improvisée.
- Autorisation côté serveur sur chaque ressource et chaque action.
- CORS global pour l’API : l’origine Angular est configurable par `CORS_ALLOWED_ORIGINS` (valeur locale par défaut : `http://localhost:4200`). En production, définir uniquement les origines exactes des interfaces déployées.
- Isolation des écoles vérifiée au niveau des services et des requêtes.
- Transport chiffré en HTTPS ; secrets hors du dépôt de code.
- Minimisation des données personnelles et contrôle des accès aux pièces jointes.
- Journal d’audit pour les opérations sensibles ; ne pas journaliser mots de passe, jetons ou données de paiement sensibles.
- Sauvegardes PostgreSQL régulières et procédure de restauration testée.
- Migrations de base versionnées et déployées de façon contrôlée.
- Tests unitaires des règles financières et d’abonnement ; tests d’intégration des dépôts, API et frontières de tenant.
- Contrôles de qualité automatisés dans la CI : compilation, tests et analyse statique.
- Accessibilité mobile et interface adaptée aux connexions lentes.
- Exports permettant à un établissement de récupérer les données auxquelles il a droit.
- Politique de conservation et suppression à définir selon le pays et les contrats.

Les objectifs chiffrés de disponibilité, temps de réponse, volume d’élèves, rétention et reprise après incident seront fixés après sélection d’un pays pilote et estimation de la charge attendue.

## 9. Plan de réalisation indicatif

### Phase 0 — Cadrage

- Choisir le pays pilote et valider le calendrier et les programmes concernés.
- Confirmer les rôles, les processus réels d’inscription et de caisse.
- Définir les forfaits initiaux et la règle de calcul des quotas.
- Choisir l’approche OIDC et les premiers services externes.
- Définir les maquettes et le parcours d’un établissement pilote.

### Phase 1 — Socle MVP

- Projet Spring Boot, migrations, sécurité, tenant et gestion des écoles.
- Angular pour l’administration et les équipes.
- Dossiers élèves, inscriptions et paramètres académiques de base.
- Frais, paiements manuels, reçus et impayés.
- Présences, annonces et notifications dans l’application.
- Cours à distance via liens externes et partage de ressources.
- Abonnements SaaS et administration des quotas.
- Flutter pour l’accès parent aux fonctions prioritaires.
- Exports, journal d’audit, sauvegardes et déploiement pilote.

### Phase 2 — Consolidation

- Notes et bulletins après validation des règles du pays pilote.
- Paiements en ligne ou mobile money selon les intégrations retenues.
- Notifications SMS/courriel et rapprochement des paiements.
- Indicateurs de gestion plus avancés et amélioration de l’expérience mobile.

### Phase 3 — Extension

- Nouveaux pays, programmes, devises et moyens de paiement.
- Fonctions hors ligne ciblées si la connectivité l’exige.
- Modules optionnels : transport, cantine, bibliothèque ou autres besoins validés.
- Évolution vers des services séparés uniquement si la charge, la disponibilité ou l’organisation des équipes le justifie.

## 10. Décisions à prendre avant le développement détaillé

1. Quel pays sera le premier pays pilote ?
2. Le MVP vise-t-il maternelle, primaire, secondaire, ou un segment plus restreint ?
3. Les notes et bulletins sont-ils indispensables au premier pilote ?
4. Quel est le volume visé par école et le mode de calcul des élèves facturés ?
5. Quels forfaits, modules et services parent optionnels seront commercialisés ?
6. Quel fournisseur d’identité, d’hébergement de fichiers, de SMS et de visioconférence est acceptable ?
7. Quels opérateurs de paiement/mobile money doivent être considérés en priorité ?
8. Le paiement des frais scolaires doit-il rester enregistré manuellement au lancement ?
9. Quels besoins hors ligne sont indispensables dans les zones ciblées ?
10. Quelles règles locales s’appliquent aux données des mineurs, à leur hébergement, à leur durée de conservation et à leur suppression ?

## 11. Références techniques

- [Spring Initializr](https://start.spring.io/)
- [Spring Boot — documentation de référence](https://docs.spring.io/spring-boot/reference/)
- [Spring Boot — exigences système](https://docs.spring.io/spring-boot/system-requirements.html)
- [Angular — documentation](https://angular.dev/)
- [Flutter — documentation](https://docs.flutter.dev/)

---

Ce README est le document de référence initial. Les éléments marqués comme décisions à prendre doivent être confirmés avant de figer les règles commerciales, réglementaires et les intégrations externes.

