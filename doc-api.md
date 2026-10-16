# Documentation API

Le contrat OpenAPI/Swagger maintenu pour cette API se trouve dans [`swagger.yaml`](swagger.yaml).

Base URL locale : `http://localhost:8080/api/v1`

Les endpoints de l’API applicative sont versionnés dans l’URI (`/api/v1`). Les changements incompatibles de contrat devront introduire une nouvelle version, par exemple `/api/v2`.

Les réponses sont au format JSON, sauf indication contraire. Flyway crée le schéma et les rôles; le compte local est seedé uniquement avec le profil `local`. En production, `admin-tech` est provisionné séparément par `scripts/provision-admin-tech.sql` après l’application des migrations.

## Authentification

### `POST /api/v1/login`

Authentifie un compte et retourne un jeton d’accès JWT signé en HS256 ainsi qu’un refresh token opaque. Cette route est accessible sans jeton.

**Requête** (`application/json`) :

```json
{
	"login": "admin-tech",
	"password": "<mot-de-passe-local>"
}
```

Le compte `admin-tech` est créé uniquement avec les migrations locales; il n’existe pas automatiquement en production.

| Champ | Type | Contraintes |
| --- | --- | --- |
| `login` | string | Obligatoire, non blanc, 100 caractères maximum. Correspond au nom d’utilisateur. |
| `password` | string | Obligatoire et non blanc. |

**Succès `200 OK`** (`application/json`) :

```json
{
	"accessToken": "<jwt>",
	"tokenType": "Bearer",
	"expiresIn": 900,
	"refreshToken": "<refresh-token>",
	"refreshExpiresIn": 2592000
}
```

`expiresIn` et `refreshExpiresIn` sont des durées en secondes. Par défaut, le JWT d’accès expire après 900 secondes (15 minutes) et le refresh token après 2 592 000 secondes (30 jours). Le refresh token est opaque et ne doit pas être journalisé. Le client doit le remplacer à chaque renouvellement.

**Erreurs** :

| Statut | Cause |
| --- | --- |
| `400 Bad Request` | Corps JSON absent/invalide ou violation des contraintes de validation. |
| `401 Unauthorized` | Identifiants invalides. La réponse est vide. |

Pour appeler une route protégée, transmettre le jeton reçu dans l’en-tête :

```http
Authorization: Bearer <accessToken>
```

Les routes protégées requièrent une authentification. La session est sans état.

### `POST /api/v1/refresh`

Renouvelle le JWT d’accès à partir d’un refresh token. Cette route n’exige pas de JWT d’accès, car celui-ci peut être expiré; le refresh token fourni dans le corps est l’identifiant d’authentification. Chaque appel réussi invalide l’ancien refresh token et en retourne un nouveau.

**Requête** (`application/json`) :

```json
{
	"refreshToken": "<refresh-token>"
}
```

**Succès `200 OK`** : retourne le même format que `POST /api/v1/login`.

**Erreur `401 Unauthorized`** : refresh token absent de la base, expiré, déjà utilisé ou associé à un compte désactivé/verrouillé. La réutilisation d’un ancien token révoque les refresh tokens encore actifs de sa famille.

### `PUT /api/v1/user/me/password`

Change le mot de passe du compte authentifié. Le mot de passe actuel doit être fourni et vérifié; le nouveau doit contenir au moins 12 caractères et ne pas dépasser 72 octets UTF-8. La réponse ne révèle pas si le compte existe lorsque le mot de passe actuel est erroné.

```json
{
	"currentPassword": "<mot-de-passe-actuel>",
	"newPassword": "<nouveau-mot-de-passe>"
}
```

Réponse `204 No Content`. Un mot de passe actuel incorrect renvoie `403 Forbidden`; un nouveau mot de passe trop court, trop long ou identique à l’actuel renvoie `400 Bad Request`. Les mots de passe sont stockés avec BCrypt et les refresh tokens actifs du compte sont révoqués. Le JWT utilisé pour l’appel reste valide jusqu’à son expiration normale, 15 minutes maximum par défaut.

## Administration de la sécurité

Les routes `/api/v1/user`, `/api/v1/role` et `/api/v1/permission` restent globales et requièrent `ADMIN_ACCESS`. L’administrateur d’école gère les rôles de son établissement via `/api/v1/establishments/{establishmentId}/roles` et consulte les permissions assignables via `/permissions`. Les rôles système sont visibles mais non modifiables; les rôles personnalisés ne sont accessibles que dans leur établissement. Les permissions plateforme (`ADMIN_ACCESS`, `can_establishment_onboard`, `can_establishment_read`) ne sont pas assignables aux rôles locaux. Un utilisateur non authentifié reçoit `401 Unauthorized`; un utilisateur hors périmètre reçoit `403 Forbidden`.

### `GET /api/v1/user`

Retourne les comptes utilisateur et leurs codes de rôles.

**Succès `200 OK`** (`application/json`) :

```json
[
	{
		"id": "e86412cd-2f24-47e1-9738-ea6d1f73aa34",
		"username": "admin-tech",
		"enabled": true,
		"locked": false,
		"createdAt": "2026-10-14T12:00:00Z",
		"updatedAt": "2026-10-14T12:00:00Z",
		"lastLoginAt": null,
		"roles": ["ADMIN"]
	}
]
```

### `GET /api/v1/role`

Retourne les rôles avec leurs permissions associées.

**Succès `200 OK`** (`application/json`) :

```json
[
	{
		"code": "ADMIN",
		"name": "Administrator",
		"description": "Default administrator role",
		"enabled": true,
		"permissions": [
			{
				"code": "can_establishment_onboard",
				"description": "Create an establishment and invite its first administrator"
			}
		]
	}
]
```

### `POST /api/v1/role`

Crée un rôle avec un code unique, un nom, une description facultative et une ou plusieurs permissions déjà présentes dans le catalogue. Requiert `ADMIN_ACCESS`.

```json
{
	"code": "LIBRARIAN",
	"name": "Bibliothécaire",
	"description": "Accès aux ressources de bibliothèque",
	"permissionCodes": ["can_student_read"]
}
```

Réponse `201 Created` avec le rôle et ses permissions. Code déjà utilisé : `409 Conflict`; permission inconnue, liste vide ou contenu invalide : `400 Bad Request`.

### `PUT /api/v1/role/{code}`

Remplace le nom, la description et la liste complète des permissions du rôle. Le code du rôle et son état actif ne sont pas modifiables par cette route. Requiert `ADMIN_ACCESS`.

```json
{
	"name": "Lecteur de dossiers élèves",
	"description": "Consultation des dossiers et inscriptions",
	"permissionCodes": ["can_student_read", "can_enrollment_read"]
}
```

Réponse `200 OK` avec le rôle mis à jour. Code inexistant : `404 Not Found`; permissions inconnues ou liste vide : `400 Bad Request`. Le rôle `ADMIN` ne peut pas recevoir `ADMIN_ACCESS` et reste limité à l’onboarding d’établissement.

Les JWT d’accès déjà émis restent valides jusqu’à leur expiration (15 minutes par défaut); les changements de permissions s’appliquent aux jetons émis ensuite.

### `GET /api/v1/establishments/{establishmentId}/roles`

Requiert `can_role_read` et l’appartenance du demandeur à l’établissement. Retourne les rôles système consultables et les rôles personnalisés de cet établissement. Le champ `editable` vaut `false` pour un rôle système et `true` pour un rôle local.

### `POST /api/v1/establishments/{establishmentId}/roles`

Requiert `can_role_create`. Crée un rôle personnalisé pour l’établissement avec les permissions choisies dans le catalogue assignable.

```json
{
	"code": "SCHOOL_LIBRARIAN",
	"name": "Bibliothécaire",
	"description": "Gestion des ressources",
	"permissionCodes": ["can_student_read"]
}
```

Réponse `201 Created`. Les codes restent uniques à l’échelle de la plateforme. Code déjà utilisé : `409 Conflict`; permissions inconnues, dupliquées ou réservées à la plateforme : `400 Bad Request`.

### `PUT /api/v1/establishments/{establishmentId}/roles/{code}`

Requiert `can_role_update`. Remplace le nom, la description et la liste complète des permissions d’un rôle personnalisé appartenant à l’établissement. Un rôle système ou un rôle d’une autre école n’est pas modifiable.

```json
{
	"name": "Bibliothécaire principal",
	"description": "Gestion et consultation des ressources",
	"permissionCodes": ["can_student_read", "can_enrollment_read"]
}
```

Réponse `200 OK`; rôle absent ou non local à l’établissement : `404 Not Found`; permission invalide ou plateforme réservée : `400 Bad Request`; demandeur hors périmètre : `403 Forbidden`.

### `GET /api/v1/establishments/{establishmentId}/permissions`

Requiert `can_permission_read` et l’appartenance du demandeur à l’établissement. Retourne le catalogue des permissions assignables. Les permissions `ADMIN_ACCESS`, `can_establishment_onboard` et `can_establishment_read` sont exclues. Le catalogue est en lecture seule pour les établissements.

### `GET /api/v1/permission`

Retourne le catalogue des permissions.

**Succès `200 OK`** (`application/json`) :

```json
[
	{
		"code": "ADMIN_ACCESS",
		"description": "Administrative access"
	}
]
```

## Établissements

### `GET /api/v1/establishments`

Nécessite `ADMIN_ACCESS`, `can_establishment_read` ou `ESTABLISHMENT_STAFF_MANAGEMENT`. `ADMIN` possède `can_establishment_read` et voit tous les établissements; le responsable d’établissement ne voit que le sien. Cette lecture n’accorde ni la désactivation d’un établissement ni l’accès à l’administration de sécurité.

### `POST /api/v1/establishments`

Crée un établissement et invite son premier administrateur d’école dans une même transaction. Réservé au rôle `ADMIN` et à `can_establishment_onboard`.

```json
{
	"code": "SCHOOL-001",
	"name": "École Exemple",
	"administrator": {
		"employeeNumber": "ADM-001",
		"firstName": "Alex",
		"lastName": "Exemple",
		"email": "alex@example.org"
	}
}
```

Réponse `201 Created` avec l’établissement et `administratorInvitation` (`employeeId`, `status: "INVITATION_SENT"`). Le premier administrateur reçoit une invitation pour définir son mot de passe. Un code d’établissement, matricule ou email déjà utilisé renvoie `409 Conflict`; un échec SMTP renvoie `503 Service Unavailable`.

### `PATCH /api/v1/establishments/{establishmentId}`

Autorisé au rôle global `ADMIN` ou avec `can_establishment_update`. Pour un administrateur local, le service vérifie aussi que le compte employé est actif, que son rôle effectif `ESTABLISHMENT_ADMIN` est présent (directement ou hérité du poste) et qu’il appartient à l’établissement visé. `can_establishment_read` n’accorde pas cette modification.

```json
{"name":"École Exemple"}
```

`code` et `name` sont optionnels individuellement : un champ absent reste inchangé. `null`, vide ou blanc est refusé. Le code est nettoyé, converti en majuscules et limité à 40 caractères; le nom est nettoyé et limité à 200 caractères. La réponse `200 OK` est un `EstablishmentResponse`; `id`, `enabled` et `createdAt` ne changent pas, `updatedAt` est actualisé. Référence absente ou désactivée : `404`; identité locale hors établissement ou rôle effectif incorrect : `403`; entrée invalide : `400`; code déjà utilisé (y compris collision concurrente) : `409`.

### `DELETE /api/v1/establishments/{establishmentId}`

Désactive logiquement un établissement. Toutes ses fiches de personnel et leurs comptes sont également désactivés, et leurs refresh tokens sont révoqués. Cette opération exige `ADMIN_ACCESS`; le rôle `ADMIN` d’onboarding ne la possède pas.

Réponse `204 No Content`; un établissement absent renvoie `404 Not Found`.

### Années scolaires et périodes

Les années et périodes sont configurées par établissement. Les périodes ont des dates comprises dans celles de leur année et ne peuvent pas se chevaucher. Les noms et calendriers restent configurables; l’API n’impose ni trimestres ni semestres.

#### `GET /api/v1/establishments/{establishmentId}/school-years`

Requiert `can_academic_year_read`. Retourne les années actives de l’établissement, triées de la plus récente à la plus ancienne.

#### `POST /api/v1/establishments/{establishmentId}/school-years`

Requiert `can_academic_year_create`.

```json
{
	"name": "2026-2027",
	"startsOn": "2026-09-01",
	"endsOn": "2027-06-30"
}
```

Réponse `201 Created`. Nom déjà utilisé dans l’établissement : `409 Conflict`; dates invalides : `400 Bad Request`.

#### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}`

Requiert `can_academic_year_read`. Une année absente, archivée ou hors périmètre renvoie `404`.

#### `PATCH /api/v1/establishments/{establishmentId}/school-years/{academicYearId}`

Requiert `can_academic_year_update`. Modifie partiellement le nom ou les dates; les nouvelles dates doivent encore contenir toutes les périodes actives.

#### `DELETE /api/v1/establishments/{establishmentId}/school-years/{academicYearId}`

Requiert `can_academic_year_delete`. Archive l’année sans supprimer ses données. Réponse `204 No Content`.

#### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods`

Requiert `can_academic_period_read`. Retourne les périodes actives triées par date de début.

#### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods`

Requiert `can_academic_period_create`.

```json
{
	"name": "Période 1",
	"startsOn": "2026-09-01",
	"endsOn": "2026-12-20"
}
```

Réponse `201 Created`. Dates hors année ou inversées : `400`; période chevauchante ou nom déjà pris dans l’année : `409`.

#### `PATCH /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods/{periodId}`

Requiert `can_academic_period_update`. Modifie partiellement le nom ou les dates; les règles de bornes et de chevauchement restent appliquées.

#### `DELETE /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods/{periodId}`

Requiert `can_academic_period_delete`. Archive la période sans supprimer l’historique. Réponse `204 No Content`.

## Catalogue scolaire

Les matières sont propres à un établissement et leur code est unique dans cet établissement. Une classe appartient directement à une année scolaire active; son code est unique dans l’année. Une matière peut être associée à plusieurs classes; une paire classe-matière ne peut être affectée qu’une seule fois. Les enseignants et emplois du temps ne font pas partie de ce lot.

### Matières

- `GET /api/v1/establishments/{establishmentId}/subjects` — permission `can_subject_read`; liste triée par nom.
- `GET /api/v1/establishments/{establishmentId}/subjects/{subjectId}` — `can_subject_read`; fiche de la matière.
- `POST /api/v1/establishments/{establishmentId}/subjects` — `can_subject_create`; crée avec `code` et `name`.
- `PATCH /api/v1/establishments/{establishmentId}/subjects/{subjectId}` — `can_subject_update`; modifie code ou nom.
- `DELETE /api/v1/establishments/{establishmentId}/subjects/{subjectId}` — `can_subject_delete`; archive, refus `409` si une classe active utilise la matière.

Exemple : `{"code":"MATH","name":"Mathematics"}`. Code déjà utilisé dans l’établissement : `409`.

### Classes

- `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes` — permission `can_school_class_read`; liste les classes actives de l’année.
- `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` — `can_school_class_read`; fiche de classe.
- `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes` — `can_school_class_create`; crée avec `code`, `name`.
- `PATCH /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` — `can_school_class_update`; modifie code ou nom.
- `DELETE /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` — `can_school_class_delete`; archive la classe.

Exemple : `{"code":"P1-A","name":"Primary 1 A"}`. L’année doit appartenir au même établissement. Code déjà utilisé dans l’année : `409`.

### Affectations classe-matière

- `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects` — permission `can_class_subject_read`.
- `PUT /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects/{subjectId}` — `can_class_subject_assign`; affecte ou réactive l’association. Réponse `200 OK` avec l’affectation.
- `DELETE /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects/{subjectId}` — `can_class_subject_unassign`; archive l’association.

La classe et la matière doivent appartenir au même établissement. Réaffecter une paire déjà active renvoie `409`; une référence absente ou hors périmètre renvoie `404`.

## Inscriptions

Une inscription rattache immédiatement un élève actif à une classe pour une année scolaire active, indépendamment des paiements. L’élève, l’année et la classe doivent appartenir au même établissement, et la classe doit être celle de l’année indiquée. Un élève ne peut avoir qu’une inscription par année (`409` en cas de doublon). L’inscription est conservée sans endpoint de modification ou suppression; les années suivantes créent de nouveaux enregistrements et préservent l’historique. Les frais sont suivis séparément.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments`

Requiert `can_enrollment_read`. Retourne les inscriptions de l’année, triées par date de création.

### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments`

Requiert `can_enrollment_create`.

```json
{
	"studentId": "<uuid-eleve>",
	"schoolClassId": "<uuid-classe>"
}
```

Réponse `201 Created` avec `id`, `establishmentId`, `studentId`, `academicYearId`, `schoolClassId` et `createdAt`. L’inscription est effective immédiatement, sans paiement préalable. Un élève, une année ou une classe absent, archivé ou hors périmètre renvoie `404`; une inscription déjà existante pour le même élève et la même année renvoie `409 Conflict`.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/students`

Requiert `can_enrollment_read`. Retourne les élèves actifs inscrits dans cette classe pour l’année indiquée, triés par nom, prénom et numéro. Une classe absente ou hors périmètre renvoie `404`.

## Présences quotidiennes

Un pointage est journalier et rattaché à l’inscription, sans granularité par cours ou demi-journée. `can_attendance_read` permet la lecture; `can_attendance_write` permet la saisie et la correction. Ces permissions sont attribuées par défaut à `ESTABLISHMENT_ADMIN` et sont assignables au personnel via les rôles et les postes. Le compte doit être actif et rattaché à l’établissement; les permissions du rôle global `ADMIN` ne donnent pas accès aux présences. Aucun accès parent n’est exposé.

Les états enregistrés sont `PRESENT` et `ABSENT`. L’état `NON_RENSEIGNE` est calculé pour un élève sans ligne de pointage et ne peut pas être transmis à l’API. Aucun calendrier de jours ouvrés ou fériés n’est appliqué. La date doit seulement être comprise dans les bornes de l’année scolaire.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/attendance?date=YYYY-MM-DD`

Requiert `can_attendance_read`. Retourne une ligne par élève actif inscrit dans la classe et les totaux présents, absents et non renseignés.

```json
{
	"establishmentId": "<uuid-etablissement>",
	"academicYearId": "<uuid-annee>",
	"schoolClassId": "<uuid-classe>",
	"date": "2026-02-12",
	"presentCount": 1,
	"absentCount": 1,
	"unreportedCount": 0,
	"entries": [
		{"studentId":"<uuid-eleve-1>","studentNumber":"STU-00000001","firstName":"Ada","lastName":"Lovelace","status":"PRESENT","absenceJustified":false,"comment":null,"createdBy":"teacher@example.org","updatedBy":"teacher@example.org","createdAt":"2026-02-12T08:05:00Z","updatedAt":"2026-02-12T08:05:00Z"},
		{"studentId":"<uuid-eleve-2>","studentNumber":"STU-00000002","firstName":"Grace","lastName":"Hopper","status":"ABSENT","absenceJustified":true,"comment":"Maladie signalée","createdBy":"teacher@example.org","updatedBy":"teacher@example.org","createdAt":"2026-02-12T08:06:00Z","updatedAt":"2026-02-12T08:06:00Z"}
	]
}
```

Une entrée non renseignée a `status: "NON_RENSEIGNE"`, `absenceJustified: false`, et ses champs de pointage/commentaire/audit valent `null`. Année ou classe absente/inactive/hors périmètre : `404`; date invalide ou hors année : `400`; permission absente : `403`.

### `PUT /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/attendance/{date}`

Requiert `can_attendance_write`. Upsert idempotent par inscription et date : une répétition avec les mêmes valeurs ne crée pas de doublon et ne change pas les horodatages; les élèves omis restent inchangés. Pour changer une ligne existante, le client doit transmettre `expectedUpdatedAt` égal à son `updatedAt` courant; une version absente ou périmée renvoie `409 Conflict`. Une répétition strictement identique peut omettre cette version. Les corrections valides mettent à jour `updatedBy` et `updatedAt`, en conservant `createdBy` et `createdAt`. Les entrées identiques répétées dans le même lot sont acceptées; deux entrées contradictoires pour le même élève rejettent tout le lot. Le lot entier est validé avant écriture et exécuté dans une transaction.

```json
{
	"entries": [
		{"studentId":"<uuid-eleve-1>","status":"PRESENT"},
		{"studentId":"<uuid-eleve-2>","status":"ABSENT","absenceJustified":true,"comment":"Maladie signalée"}
	]
}
```

Pour corriger ultérieurement cette absence, transmettre la version `updatedAt` retournée par la feuille : `{"entries":[{"studentId":"<uuid-eleve-2>","status":"PRESENT","expectedUpdatedAt":"2026-02-12T08:06:00Z"}]}`. Une répétition sans changement peut réutiliser l’entrée sans version; toute correction différente exige la version courante.

`absenceJustified` est facultatif, vaut `false` par défaut et ne peut être vrai que pour `ABSENT`. Le commentaire est facultatif et limité à 500 caractères. La réponse `200 OK` retourne la feuille recalculée. Lot vide, élève non inscrit dans cette classe/année, doublons contradictoires, date invalide ou justification incohérente : `400`; contexte absent/inactif/hors périmètre : `404`; écriture concurrente incompatible : `409`; l’écriture partielle du lot est annulée.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/students/{studentId}/attendance?from=YYYY-MM-DD&to=YYYY-MM-DD`

Requiert `can_attendance_read`. Les bornes sont inclusives, ordonnées, situées dans l’année et limitées à 366 jours. `records` ne contient que les jours réellement pointés; une journée sans pointage n’est jamais convertie en présence ou absence. L’historique demeure accessible après archivage de l’élève ou de sa classe, tant que le compte appelant est actif et rattaché à l’établissement.

```json
{
	"establishmentId":"<uuid-etablissement>",
	"academicYearId":"<uuid-annee>",
	"studentId":"<uuid-eleve>",
	"from":"2026-02-01",
	"to":"2026-02-28",
	"records":[
		{"date":"2026-02-12","status":"ABSENT","absenceJustified":true,"comment":"Maladie signalée","createdBy":"teacher@example.org","updatedBy":"teacher@example.org","createdAt":"2026-02-12T08:05:00Z","updatedAt":"2026-02-12T08:05:00Z"}
	]
}
```

Élève sans inscription dans l’année, établissement ou année absents : `404`; plage invalide, hors année ou supérieure à 366 jours : `400`; permission absente ou établissement hors périmètre : `403`.

## Frais et paiements scolaires

Les types de frais sont configurés par établissement et désactivables sans supprimer les frais historiques. Chaque nouvelle définition de frais doit référencer un type actif (`feeTypeId`); le code et le nom du type sont copiés dans la définition pour préserver l’historique. Les frais sont définis dans le périmètre d’une année scolaire, avec une portée facultative par classe ou par élève. Un frais sans `schoolClassId` ni `studentId` s’applique à toute l’année. Sa création crée immédiatement une créance pour chaque élève actuellement inscrit qui correspond à la portée. Une créance conserve une copie du montant, de la devise, de l’échéance et des informations de l’élève; le frais d’origine n’est pas modifiable par l’API. Après de nouvelles inscriptions, `POST .../apply` applique de nouveau le frais sans créer de doublons.

La migration vers ce modèle désactive les anciennes définitions ciblées par niveau afin de ne pas les convertir en frais globaux. Les créances et paiements déjà enregistrés sont conservés.

Les montants sont positifs, au plus deux décimales, et la devise est un code de trois lettres. Les paiements sont enregistrés manuellement, immuables et associés à une seule créance. Le solde est recalculé depuis l’historique; un paiement supérieur au solde est refusé. Les permissions financières sont attribuées au rôle `ESTABLISHMENT_ADMIN`.

### `GET /api/v1/establishments/{establishmentId}/tuition-fee-types`

Requiert `can_tuition_fee_type_read`. Retourne les types actifs du catalogue de l’établissement.

### `POST /api/v1/establishments/{establishmentId}/tuition-fee-types`

Requiert `can_tuition_fee_type_create`. Crée un type avec code unique et libellé.

```json
{"code":"TUITION","name":"Frais de scolarité"}
```

Réponse `201 Created`; code déjà utilisé : `409 Conflict`.

### `PATCH /api/v1/establishments/{establishmentId}/tuition-fee-types/{feeTypeId}`

Requiert `can_tuition_fee_type_update`. Renomme un type actif avec `{"name":"Scolarité annuelle"}`. Le libellé déjà copié dans les frais existants reste inchangé.

### `DELETE /api/v1/establishments/{establishmentId}/tuition-fee-types/{feeTypeId}`

Requiert `can_tuition_fee_type_delete`. Désactive le type sans effacer les définitions de frais et créances historiques. Réponse `204 No Content`.

### `GET /api/v1/establishments/{establishmentId}/payment-methods`

Requiert `can_tuition_read`. Retourne les moyens de paiement configurés pour l’établissement.

### `PUT /api/v1/establishments/{establishmentId}/payment-methods`

Requiert `can_payment_method_update`. Remplace la liste des méthodes acceptées; la liste ne peut pas être vide. Les codes sont normalisés en majuscules et doivent être uniques.

```json
{
	"methods": [
		{"code": "CASH", "name": "Espèces"},
		{"code": "BANK_TRANSFER", "name": "Virement bancaire"}
	]
}
```

Réponse `200 OK` avec les méthodes actives enregistrées. Une méthode non configurée ou désactivée ne peut pas être utilisée pour encaisser un paiement.

### `PATCH /api/v1/establishments/{establishmentId}/payment-methods/{code}`

Requiert `can_payment_method_update`. Modifie le libellé d’un moyen actif avec `{"name":"Paiement espèces"}`. Les reçus déjà émis conservent le libellé historique.

### `DELETE /api/v1/establishments/{establishmentId}/payment-methods/{code}`

Requiert `can_payment_method_delete`. Désactive logiquement un moyen de paiement, sans modifier les paiements et reçus existants. Réponse `204 No Content`; un code inconnu ou déjà désactivé renvoie `404`.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees`

Requiert `can_tuition_read`. Retourne les définitions de frais de l’année scolaire.

### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees`

Requiert `can_tuition_create`. Crée un frais et ses créances pour les élèves inscrits correspondants. `schoolClassId` et `studentId` sont facultatifs mais ne peuvent pas être fournis ensemble; sans les deux, le frais concerne toute l’année.

```json
{
	"name": "Frais de scolarité - période 1",
	"feeTypeId": "<uuid-type-frais>",
	"amount": 125000,
	"currency": "XOF",
	"dueDate": "2026-10-31",
	"schoolClassId": "<uuid-classe>"
}
```

Réponse `201 Created` avec `fee` (incluant `feeTypeId`, `feeTypeCode` et `feeTypeName`) et `chargesCreated`. L’échéance doit être comprise dans l’année scolaire. Type, classe, élève ou année absents/hors périmètre : `404`; nom déjà utilisé dans l’année : `409`; montant ou portée invalide : `400`.

### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/{feeId}/apply`

Requiert `can_tuition_apply`. Applique la définition aux inscriptions présentes, utile après l’ajout de nouveaux élèves. L’opération est idempotente : une créance existante n’est pas dupliquée. Réponse `200 OK` avec `chargesCreated`.

### `GET /api/v1/establishments/{establishmentId}/students/{studentId}/tuition-fees`

Requiert `can_tuition_read`. Retourne les créances de l’élève, leurs paiements, le total payé, le solde et l’indicateur `overdue`.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/outstanding`

Requiert `can_tuition_outstanding_read`. Retourne toutes les créances avec un solde positif de l’année; `overdue` distingue celles dont l’échéance est passée.

### `POST /api/v1/establishments/{establishmentId}/students/{studentId}/tuition-fees/{chargeId}/payments`

Requiert `can_payment_record`. Enregistre un paiement partiel ou total avec une méthode configurée pour l’établissement. Le serveur attribue le numéro de reçu et l’horodatage; une référence externe est facultative.

```json
{
	"amount": 50000,
	"paymentMethodCode": "CASH",
	"reference": "CAISSE-2026-001"
}
```

Réponse `201 Created` avec le paiement, son `receiptNumber` et ses informations d’enregistrement. Créance absente ou hors périmètre : `404`; dépassement du solde : `409`; méthode inconnue ou montant invalide : `400`.

### `GET /api/v1/establishments/{establishmentId}/payments/{paymentId}/receipt`

Requiert `can_payment_receipt_read`. Retourne le reçu JSON immuable : numéro unique, élève et frais tels qu’enregistrés, montant reçu, méthode, référence, auteur et solde après ce paiement. Le reçu peut être retrouvé par l’identifiant du paiement; la réponse ne modifie pas l’historique.

Les définitions, créances et paiements ne disposent pas de routes de modification ou suppression afin de préserver l’historique financier.

### Gestion des postes

Les endpoints de gestion sont portés par le module `positions`. Ils nécessitent respectivement `can_position_read`, `can_position_create`, `can_position_update`, `can_position_assign_roles` et `can_position_delete`; ces permissions sont attribuées aux rôles `ADMIN` et `ESTABLISHMENT_ADMIN`. Le service limite ensuite un responsable au seul établissement auquel il est rattaché. Un poste (`Position`) appartient à un seul établissement. Une fiche de personnel référence son poste par `positionId`; les rôles effectifs d’un membre sont l’union de ses rôles directs et des rôles du poste. Un poste peut recevoir plusieurs rôles actifs du catalogue, y compris les rôles personnalisés créés par l’administration. `ADMIN` et `PARENT` sont réservés et ne peuvent pas être attribués à un poste.

#### `GET /api/v1/establishments/{establishmentId}/positions`

Retourne les postes actifs de l’établissement. Requiert `can_position_read`; un responsable qui cible un établissement hors périmètre reçoit `403`.

#### `POST /api/v1/establishments/{establishmentId}/positions`

Crée un poste dans l’établissement. Requiert `can_position_create` et le contrôle de périmètre du service.

```json
{
	"name": "Enseignant",
	"roleCodes": ["STAFF"]
}
```

`roleCodes` est facultatif et vaut `[]` par défaut; il peut contenir plusieurs codes de rôles actifs existants. Réponse `201 Created` avec l’objet poste, ses rôles et ses dates. Un nom déjà utilisé dans l’établissement renvoie `409`; un code inexistant, désactivé, `ADMIN` ou `PARENT` renvoie `400`.

#### `PATCH /api/v1/establishments/{establishmentId}/positions/{positionId}`

Renomme le poste avec `{"name":"Enseignant principal"}`. Requiert `can_position_update`. Réponse `200 OK`; poste absent, désactivé ou hors établissement renvoie `404`; un nom déjà utilisé renvoie `409`.

#### `PUT /api/v1/establishments/{establishmentId}/positions/{positionId}/roles`

Remplace entièrement les rôles hérités du poste, par exemple `{"roleCodes":["STAFF","LIBRARIAN"]}`. Requiert `can_position_assign_roles`. Les comptes actifs des titulaires sont synchronisés dans la même transaction; leurs rôles directs sont conservés. Réponse `200 OK`; un rôle inexistant, désactivé, `ADMIN` ou `PARENT` renvoie `400`.

#### `DELETE /api/v1/establishments/{establishmentId}/positions/{positionId}`

Désactive le poste. Requiert `can_position_delete`. La désactivation est refusée par `409 Conflict` tant qu’une fiche de personnel lui est rattachée. Réponse `204 No Content`.

## Élèves

Les élèves sont gérés par établissement par des employés autorisés. Le numéro d’élève est attribué automatiquement par une séquence globale (`STU-00000001`, `STU-00000002`, etc.), unique et immuable via l’API. L’archivage est logique; un élève archivé n’apparaît plus dans les listes et ne peut plus être consulté ou modifié.

### `GET /api/v1/establishments/{establishmentId}/students`

Requiert `can_student_read`. Retourne les élèves actifs de l’établissement, triés par nom, prénom et numéro. Le service vérifie le périmètre du responsable.

### `POST /api/v1/establishments/{establishmentId}/students`

Requiert `can_student_create`.

```json
{
	"firstName": "Ada",
	"lastName": "Lovelace"
}
```

Réponse `201 Created` avec la fiche créée et son `studentNumber` généré. Le numéro n’est pas fourni par le client. Établissement absent, désactivé ou hors périmètre renvoie `404`.

### `GET /api/v1/establishments/{establishmentId}/students/{studentId}`

Requiert `can_student_read`. Retourne la fiche; une fiche absente, archivée ou hors périmètre renvoie `404`.

### `PATCH /api/v1/establishments/{establishmentId}/students/{studentId}`

Requiert `can_student_update`. Modifie partiellement le prénom ou le nom. Le numéro généré est immuable et ne peut pas être modifié via l’API. Au moins un des deux champs doit être fourni; fiche absente, archivée ou hors périmètre renvoie `404`.

```json
{
	"firstName": "Augusta"
}
```

### `DELETE /api/v1/establishments/{establishmentId}/students/{studentId}`

Requiert `can_student_delete`. Archive l’élève sans supprimer son historique. Réponse `204 No Content`; fiche absente, déjà archivée ou hors périmètre renvoie `404`.

## Parents et tuteurs

Un tuteur est identifié par son adresse e-mail et peut être lié à plusieurs élèves, y compris dans plusieurs établissements. La création du premier lien envoie une invitation à usage unique valable 48 heures. Après activation, le tuteur se connecte par `POST /api/v1/login` avec son adresse e-mail et le mot de passe choisi. Un lien retiré supprime uniquement l’accès à l’élève concerné.

### `GET /api/v1/establishments/{establishmentId}/students/{studentId}/guardians`

Requiert `can_guardian_read`. Retourne les tuteurs actifs associés à l’élève, dans le périmètre de l’établissement.

```json
[
	{
		"id": "<uuid-tuteur>",
		"firstName": "Grace",
		"lastName": "Hopper",
		"email": "grace@example.org"
	}
]
```

### `POST /api/v1/establishments/{establishmentId}/students/{studentId}/guardians`

Requiert `can_guardian_link`. Crée le lien entre un tuteur et l’élève. Si l’adresse n’a pas encore d’accès parent, un courriel d’invitation est envoyé.

```json
{
	"firstName": "Grace",
	"lastName": "Hopper",
	"email": "grace@example.org"
}
```

Réponse `201 Created` : `guardian` identifie le tuteur et `accessStatus` vaut `INVITATION_SENT` ou `ACCESS_ALREADY_ACTIVE`. Un lien déjà présent renvoie `409 Conflict`; une fiche élève absente ou hors périmètre renvoie `404 Not Found`.

### `DELETE /api/v1/establishments/{establishmentId}/students/{studentId}/guardians/{guardianId}`

Requiert `can_guardian_unlink`. Supprime le lien uniquement entre cet élève et ce tuteur. Réponse `204 No Content`; lien absent ou élève hors périmètre : `404 Not Found`.

### `POST /api/v1/guardians/invitations/accept`

Accessible sans authentification. Le jeton provient du fragment de l’URL reçue par courriel; il est stocké sous forme hachée, valable 48 heures et à usage unique.

```json
{
	"token": "<jeton-invitation>",
	"password": "<mot-de-passe-de-12-caracteres-minimum>"
}
```

Le mot de passe doit comporter au moins 12 caractères et ne pas dépasser 72 octets UTF-8. Réponse `204 No Content`; jeton invalide, expiré ou déjà utilisé : `401 Unauthorized`; compte existant pour cette adresse : `409 Conflict`.

### `GET /api/v1/guardians/me/children`

Requiert la permission `can_guardian_read_children`, attribuée au rôle `PARENT`. Retourne uniquement les élèves actifs liés au compte parent ainsi que les établissements actifs correspondants. Aucun identifiant d’élève fourni par le client ne peut élargir ce périmètre.

```json
[
	{
		"id": "<uuid-eleve>",
		"establishmentId": "<uuid-etablissement>",
		"studentNumber": "STU-00000001",
		"firstName": "Ada",
		"lastName": "Lovelace"
	}
]
```

Un utilisateur authentifié sans la permission reçoit `403 Forbidden`.

### Portail parent par enfant

Toutes les routes ci-dessous requièrent `can_guardian_read_children`. Chaque requête vérifie que le compte authentifié a toujours un lien actif avec l’élève, que l’élève et l’établissement sont actifs et que l’inscription correspond à l’année demandée. Le portail exige une licence active avec `PARENT_PORTAL`; les appels financiers requièrent également `TUITION` et les présences `ATTENDANCE`. Les montants sont totalisés séparément par devise.

- `GET /api/v1/guardians/me/children/{studentId}/summary?academicYearId={uuid}` — identité scolaire, établissement, année, classe et soldes par devise (`amountBilled`, `amountPaid`, `balance`).
- `GET /api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/tuition-fees` — créances de l’enfant pour l’année avec paiements et soldes, sans identité ou audit interne.
- `GET /api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/tuition-fees/{chargeId}/payments?from=YYYY-MM-DD&to=YYYY-MM-DD` — paiements d’une créance; dates inclusives, plage limitée à 366 jours.
- `GET /api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/attendance?from=YYYY-MM-DD&to=YYYY-MM-DD` — pointages d’inscriptions, plage inclusive maximale de 366 jours.

Les réponses de présence exposent seulement `parentComment` comme commentaire destiné à la famille. Le commentaire interne, `createdBy` et `updatedBy` ne sont jamais retournés. Une relation absente, inactive ou hors périmètre renvoie `404`; permission/licence refusée `403`; période invalide `400`.

## Contenus pédagogiques et pièces jointes

Les contenus appartiennent à une classe d’une année scolaire et sont de type `LESSON` ou `ASSIGNMENT`. Leur état suit `DRAFT -> PUBLISHED -> ARCHIVED`; seule une ressource publiée est consultable par un parent. Les matières sont facultatives, mais doivent être affectées à la classe. Les leçons n’acceptent pas de date limite. Le personnel doit disposer des permissions `can_learning_content_read`, `can_learning_content_create`, `can_learning_content_update`, `can_learning_content_publish` ou `can_learning_content_archive` selon l’action. L’établissement doit avoir la fonctionnalité `LEARNING_CONTENT` active.

Base des routes staff : `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content`.

- `GET` — liste paginée; filtres facultatifs `type`, `page` (défaut 0), `size` (défaut 20, maximum 100). Requiert `can_learning_content_read`.
- `POST` — crée un brouillon avec `type`, `title`, `description`, `body`, `subjectId` et `dueDate` facultatifs selon les règles ci-dessus. Requiert `can_learning_content_create`; réponse `201 Created`.
- `GET /{contentId}` — détail non archivé, brouillon inclus. Requiert `can_learning_content_read`.
- `PUT /{contentId}` — remplace les champs éditables d’un contenu non archivé; son type ne peut pas changer. Requiert `can_learning_content_update`.
- `POST /{contentId}/publish` — publie un brouillon. Requiert `can_learning_content_publish`.
- `POST /{contentId}/archive` — archive un contenu sans supprimer l’historique. Requiert `can_learning_content_archive`.
- `POST /{contentId}/attachments` (`multipart/form-data`, champ `file`) — ajoute une pièce jointe uniquement à un brouillon. Requiert `can_learning_content_update`; réponse `201 Created`.
- `GET /{contentId}/attachments` — liste les métadonnées sans clé de stockage. Requiert `can_learning_content_read`.
- `GET /{contentId}/attachments/{attachmentId}` — téléchargement privé autorisé après vérification du périmètre contenu/classe. Requiert `can_learning_content_read`.

Les uploads acceptent PDF, PNG, JPG/JPEG, WEBP, TXT, DOCX et PPTX uniquement avec leur type MIME autorisé correspondant. La limite par défaut est 10 MiB (`LEARNING_MAX_FILE_SIZE_BYTES` et `spring.servlet.multipart.max-file-size`), stockée hors du webroot dans `${LEARNING_STORAGE_ROOT}` ou le dossier temporaire privé par défaut. Le serveur crée une clé UUID; aucun chemin disque n’est renvoyé. Une pièce jointe n’est pas supprimable séparément; archiver le contenu masque ses pièces jointes aux parents.

Base des routes parent : `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content`.

- `GET` — liste paginée des seuls contenus `PUBLISHED` de la classe où l’enfant est inscrit; accepte `type`, `page`, `size`.
- `GET /{contentId}` — détail publié uniquement.
- `GET /{contentId}/attachments` et `GET /{contentId}/attachments/{attachmentId}` — métadonnées/téléchargement des pièces jointes du contenu publié.

Les routes parent réutilisent `can_guardian_read_children` et réévaluent le lien parent/enfant et l’inscription à chaque accès. Un brouillon, contenu archivé, pièce jointe liée à un autre contenu ou objet hors classe est masqué par `404`. Upload invalide, taille dépassée, média interdit ou plage de pagination invalide : `400`; transition d’état incompatible : `409`.

## Licences plateforme

L’établissement est le titulaire du droit d’accès, conformément au modèle SaaS du README. Aucun prix, quota ou essai n’est fixé par cette tranche. Une offre contient un ensemble de capacités (`SCHOOL_MANAGEMENT`, `STUDENT_RECORDS`, `PARENT_PORTAL`, `TUITION`, `ATTENDANCE`, `LEARNING_CONTENT`). Une licence possède une période `[validFrom, validUntil)` (fin facultative) et peut être suspendue. L’état retourné est `UPCOMING`, `ACTIVE`, `EXPIRED` ou `SUSPENDED`. Les événements d’attribution, remplacement, suspension et réactivation sont audités.

La migration V30 attribue `LEGACY_FULL_ACCESS` sans échéance aux établissements déjà présents, afin de ne pas couper leurs services. Les nouveaux établissements ne reçoivent pas de licence implicite : un administrateur plateforme doit en attribuer une. Les changements de licence verrouillent la ligne établissement et refusent les chevauchements avec une licence future; une licence antérieure chevauchante est terminée au début de sa remplaçante.

- `GET /api/v1/establishments/{establishmentId}/license` — `can_license_read` (ou `ROLE_ADMIN`); établissement strictement limité au périmètre autorisé.
- `GET /api/v1/admin/license-offers` — rôle `ADMIN` uniquement; catalogue actif.
- `POST /api/v1/admin/license-offers` — rôle `ADMIN`; crée une offre (`code`, `name`, `features`), réponse `201 Created`.
- `POST /api/v1/admin/establishments/{establishmentId}/licenses` — rôle `ADMIN`; attribue/remplace une licence (`offerId`, `validFrom` facultatif, `validUntil` facultatif), réponse `201 Created`.
- `POST /api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/suspend` — rôle `ADMIN`; suspend une licence active ou à venir.
- `POST /api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/reactivate` — rôle `ADMIN`; réactive une licence suspendue non expirée.
- `POST /api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/end` — rôle `ADMIN`; termine immédiatement une licence en cours.
- `GET /api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/events` — rôle `ADMIN`; historique d’audit.

Code d’offre ou période invalide : `400`; offre/établissement/licence absent : `404`; état incompatible, offre en doublon ou chevauchement non remplaçable : `409`; rôle ou permission absente : `403`. Les contrôles d’entitlement sont appliqués côté services métier, en plus des permissions HTTP. Les routes d’authentification, de santé et d’administration de licences restent utilisables même lorsqu’une licence est suspendue/expirée.

## Personnel

Les routes de gestion requièrent `ADMIN_ACCESS` ou `ESTABLISHMENT_STAFF_MANAGEMENT`. Un responsable d’établissement ne voit et ne gère que le personnel de son établissement; l’administrateur global conserve l’accès global.

### `POST /api/v1/employees`

Crée une fiche de personnel et envoie une invitation par courriel. Un responsable d’établissement ne peut pas attribuer directement `ESTABLISHMENT_ADMIN`, mais le membre peut hériter de ce rôle si son poste le porte.

```json
{
	"employeeNumber": "STAFF-001",
	"firstName": "Alex",
	"lastName": "Exemple",
	"email": "alex@example.org",
	"positionId": "<uuid-poste>",
	"directRoleCodes": ["STAFF"]
}
```

L’établissement est déterminé par le poste. Réponse : `201 Created` avec `employeeId` et `status: "INVITATION_SENT"`. Un matricule ou email déjà utilisé renvoie `409 Conflict`; l’envoi SMTP indisponible renvoie `503 Service Unavailable`.

### `POST /api/v1/employees/invitations/accept`

Accepte une invitation sans JWT. Le token reçu par courriel est à usage unique et expire après 48 heures. Le mot de passe doit contenir au moins 12 caractères et ne pas dépasser 72 octets UTF-8.

```json
{
	"token": "<token-reçu-par-courriel>",
	"password": "<mot-de-passe-choisi>"
}
```

Réponse : `204 No Content`. Un token invalide, expiré ou déjà utilisé renvoie `401 Unauthorized`.

### `GET /api/v1/employees/me`

Retourne la fiche associée au compte connecté. Requiert la permission `STAFF_ACCESS`.

### `GET /api/v1/employees`

Requiert `ADMIN_ACCESS` ou `ESTABLISHMENT_STAFF_MANAGEMENT`. Retourne le personnel visible dans le périmètre de l’appelant : tous les établissements pour l’administrateur global, son établissement pour le responsable.

### `GET /api/v1/employees/{employeeId}`

Retourne une fiche dans le périmètre de l’appelant. Une fiche hors périmètre est masquée par `404 Not Found`.

### `PATCH /api/v1/employees/{employeeId}`

Requiert `ADMIN_ACCESS` ou `ESTABLISHMENT_STAFF_MANAGEMENT`. Met à jour partiellement le matricule, le prénom, le nom, l’email ou le poste avec `positionId`. Le nouveau poste doit appartenir au même établissement. Au moins un champ doit être fourni. L’email est également utilisé comme login; sa modification synchronise le compte de sécurité et un changement de poste recalcule ses rôles effectifs.

```json
{
	"positionId": "<uuid-poste>"
}
```

Réponse `200 OK` avec la fiche à jour. Une fiche hors périmètre renvoie `404`; un matricule ou email déjà utilisé renvoie `409`.

### `PUT /api/v1/employees/{employeeId}/roles`

Remplace les rôles directs du membre. Les valeurs acceptées pour les rôles directs restent `STAFF` et `ESTABLISHMENT_ADMIN`; les rôles effectifs incluent aussi les rôles personnalisés hérités du poste. Les rôles personnalisés se gèrent au niveau des postes, pas via cette route.

```json
{
	"roleCodes": ["STAFF"]
}
```

Réponse `200 OK` avec la fiche mise à jour. Les rôles inconnus ou le rôle `ADMIN` renvoient `400`; une cible hors périmètre renvoie `404`.

### `DELETE /api/v1/employees/{employeeId}`

Désactive le membre sans supprimer sa fiche. Le compte de sécurité est également désactivé et ses refresh tokens révoqués. Réservé à `ADMIN` ou à l’administrateur du même établissement.

Réponse `204 No Content`; une fiche hors périmètre renvoie `404`.

## SMTP

Configurer `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `EMPLOYEE_INVITATION_FROM`, `EMPLOYEE_INVITATION_ACCEPT_URL`, `GUARDIAN_INVITATION_FROM` et `GUARDIAN_INVITATION_ACCEPT_URL` selon l’environnement. Les anciennes variables `STAFF_INVITATION_FROM` et `STAFF_INVITATION_ACCEPT_URL` restent acceptées en fallback. Les URL d’acceptation doivent pointer vers les écrans fournis par le client web; le dépôt actuel expose seulement l’API. Le token est placé dans le fragment de l’URL (`#token=...`) afin qu’il ne soit pas envoyé aux journaux HTTP du serveur frontal. Le client lit ce fragment puis appelle la route d’acceptation correspondante avec le token et le mot de passe choisi.

## Santé de l’application

### `GET /actuator/health`

Retourne l’état de santé de l’application. Cette route est accessible sans authentification.

**Succès `200 OK`** (`application/json`) :

```json
{
	"status": "UP"
}
```

## Interface de développement

La console H2 est une interface Web, pas une route REST. Elle est activée uniquement avec le profil `local` et accessible à `http://localhost:8080/h2-console`. Connexion locale : URL JDBC `jdbc:h2:mem:gestion_ecole`, utilisateur `sa`, mot de passe vide.
