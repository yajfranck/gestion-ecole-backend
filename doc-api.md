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

## Dashboard administrateur

### `GET /api/v1/dashboard`

Retourne en un appel les agrégats du dashboard. Aucun corps de requête. JWT Bearer et permission `can_dashboard_read` requis ; la migration V5 attribue cette permission aux rôles `ADMIN` et `ESTABLISHMENT_ADMIN`. Les jetons déjà émis doivent être renouvelés ou le compte reconnecté pour intégrer cette permission.

Le serveur recharge le compte, ses rôles actifs et sa permission à chaque appel. Un compte désactivé/verrouillé, une permission retirée ou un rôle administrateur révoqué bloque aussi un JWT encore valide. La permission seule ne suffit pas pour un rôle autre que les deux rôles administrateurs. Un administrateur local doit avoir une fiche de personnel active, liée à ce même compte, avec `ESTABLISHMENT_ADMIN` dans ses rôles effectifs directs ou hérités du poste, et un établissement actif.

Le périmètre vient exclusivement de cette identité :

- `ADMIN` reçoit `scope: "PLATFORM"`, `platform` renseigné et `establishment: null`. Il ne reçoit pas les effectifs, présences, finances ou contenus détaillés des écoles. Si le compte cumule les deux rôles, la vue plateforme prime.
- `ESTABLISHMENT_ADMIN` reçoit `scope: "ESTABLISHMENT"`, `platform: null` et `establishment` renseigné pour son établissement uniquement. Aucun paramètre `establishmentId` n’est pris en charge ; en transmettre un ne permet pas de changer le périmètre.

Paramètres de requête facultatifs pour la vue établissement :

| Paramètre | Format | Règle |
|---|---|---|
| `academicYearId` | UUID | Année active de l’établissement authentifié. À défaut, l’année contenant la date UTC courante est choisie ; s’il n’y en a pas, l’année active ayant la date de début la plus récente est utilisée. En cas d’égalité, tri par UUID. |
| `date` | `YYYY-MM-DD` | Journée des présences et référence des échéances impayées ; doit être comprise dans l’année sélectionnée. À défaut, date UTC courante ramenée au début ou à la fin de cette année si nécessaire. Sans année, la date UTC courante est affichée et les agrégats annuels restent vides. |

Ces filtres sont refusés pour la vue plateforme. Fournir `date` explicitement sans année disponible est également refusé. Les données sont lues dans une transaction de consultation avec une isolation `REPEATABLE_READ` ; `generatedAt` est l’instant UTC du calcul. La date ne transforme pas la réponse en historique des encaissements : les totaux financiers incluent tous les paiements enregistrés pour les créances de l’année sélectionnée.

**Vue plateforme.** `platform` contient `totalEstablishments`, `activeEstablishments`, `disabledEstablishments`, puis les nombres d’enregistrements de licence dans chaque état : `activeLicenses`, `suspendedLicenses`, `expiredLicenses`, `upcomingLicenses`. Les licences de tous les établissements, y compris désactivés, sont comptées ; des périodes différentes d’une même école peuvent apparaître dans plusieurs compteurs. `establishmentsWithoutActiveLicense` compte les établissements actifs sans licence actuellement active rattachée à une offre active. Aucun tarif ou revenu SaaS n’est inventé à partir des frais scolaires.

**Vue établissement.** `establishment` contient :

- `id`, `name`, `academicYear` (`id`, `name`, `startsOn`, `endsOn`, ou `null` sans année).
- `license` : `state` (`ACTIVE`, `SUSPENDED`, `EXPIRED`, `UPCOMING`, `UNLICENSED`), `offerCode`, `validFrom`, `validUntil`, `features`. La période en cours prime, puis la prochaine période, puis la plus récente période passée. Une licence future ne masque pas la licence courante. `features` est vide sans période et offre actives.
- `counts` : nombres de fiches actives `students`, `employees`, `subjects` pour l’établissement entier ; `classes` et `enrollments` pour l’année sélectionnée. Les inscriptions comptées concernent des élèves et classes actifs. Ces compteurs de gestion et la licence restent consultables même sans fonctionnalité métier active.
- `classes` : liste triée par nom puis identifiant, avec `id`, `code`, `name`, `series` (libellé facultatif, `null` si aucune série), `students` (élèves actifs inscrits). Les classes actives sans élève sont incluses avec zéro.
- `attendance` : `date`, `present`, `absent`, `justifiedAbsent` (sous-ensemble des absents), `notRecorded` (inscriptions actives sans pointage ce jour). Aucun commentaire ni identité d’élève n’est exposé. Vaut `null` si `ATTENDANCE` n’est pas actif.
- `finance` : un élément par devise, trié par devise, avec `currency`, `amountBilled`, `amountPaid`, `balance`, `overdueBalance`. Les créances et paiements historiques sont conservés dans le calcul même si l’élève ou sa classe ont été archivés. Les paiements sont agrégés par créance avant sommation pour éviter de multiplier le montant facturé en cas de paiements partiels. Une échéance est en retard si elle précède strictement `date`. Les devises ne sont jamais additionnées entre elles. Vaut `[]` sans créances et `null` si `TUITION` n’est pas actif.
- `learning` : `draftLessons`, `publishedLessons`, `draftAssignments`, `publishedAssignments` pour les classes actives de l’année ; les contenus archivés sont exclus. Vaut `null` si `LEARNING_CONTENT` n’est pas actif.

Sans année disponible, `classes` est vide, les compteurs annuels valent zéro et `academicYear` vaut `null`. Les blocs métier autorisés restent présents avec des valeurs vides/zéro ; les blocs non autorisés restent `null`. Aucun mot de passe, jeton, commentaire interne, détail de paiement ou donnée personnelle d’élève n’est exposé.

Exemple plateforme (`200 OK`) :

```json
{
	"scope": "PLATFORM",
	"generatedAt": "2026-10-17T09:00:00Z",
	"platform": {
		"totalEstablishments": 10,
		"activeEstablishments": 9,
		"disabledEstablishments": 1,
		"activeLicenses": 7,
		"suspendedLicenses": 1,
		"expiredLicenses": 2,
		"upcomingLicenses": 1,
		"establishmentsWithoutActiveLicense": 2
	},
	"establishment": null
}
```

Exemple d’appel établissement :

```http
GET /api/v1/dashboard?academicYearId=22222222-2222-4222-8222-222222222222&date=2026-10-17
Authorization: Bearer <accessToken>
```

La réponse `200 OK` utilise le même objet racine avec `scope: "ESTABLISHMENT"` et les champs décrits ci-dessus. Formats UUID/date invalides, date hors année ou filtres envoyés par `ADMIN` : `400 Bad Request` ; JWT absent, invalide ou expiré : `401 Unauthorized` ; compte, rôle, permission ou rattachement personnel refusé : `403 Forbidden` ; établissement désactivé ou année absente, archivée ou d’un autre établissement : `404 Not Found`. Les erreurs métier sont des `ProblemDetail` sans détails SQL.

### Années scolaires et périodes

Les années et périodes sont configurées par établissement. Les périodes ont des dates comprises dans celles de leur année et ne peuvent pas se chevaucher. Les noms et calendriers restent configurables; l’API n’impose ni trimestres ni semestres.

#### `GET /api/v1/establishments/{establishmentId}/school-years`

Accessible avec un JWT Bearer et `can_academic_year_read` dans le périmètre de gestion existant, ou avec `can_guardian_read_children` pour un parent/tuteur ayant au moins un enfant actif lié dans cet établissement. Pour cet accès parent, le compte doit être actif et non verrouillé, le tuteur et l’établissement actifs, et une licence active doit inclure `PARENT_PORTAL`. Ces contrôles sont réévalués à chaque requête. Un parent également administrateur conserve son périmètre de gestion avec `can_academic_year_read`.

Paramètre de chemin : `establishmentId`, UUID obligatoire. Aucun corps ni paramètre de requête. Retourne toutes les années actives de cet établissement, triées par date de début décroissante, y compris les années où les enfants du parent ne sont pas inscrits. Les années archivées sont exclues. Cet accès concerne uniquement la liste ; il n’ajoute aucun droit de consultation du détail ou des périodes, de création, de modification ou d’archivage.

Réponse `200 OK` : liste de `AcademicYearResponse` (`id`, `establishmentId`, `name`, `startsOn`, `endsOn`, `enabled`, `createdAt`, `updatedAt`), ou `[]` si l’établissement n’a aucune année active.

```http
GET /api/v1/establishments/33333333-3333-4333-8333-333333333333/school-years
Authorization: Bearer <accessToken>
```

```json
[
	{
		"id": "22222222-2222-4222-8222-222222222222",
		"establishmentId": "33333333-3333-4333-8333-333333333333",
		"name": "2026-2027",
		"startsOn": "2026-09-01",
		"endsOn": "2027-06-30",
		"enabled": true,
		"createdAt": "2026-08-01T10:00:00Z",
		"updatedAt": "2026-08-01T10:00:00Z"
	}
]
```

Erreurs : UUID mal formé `400 Bad Request` ; JWT absent, invalide ou expiré `401 Unauthorized` ; permissions absentes, licence requise absente, ou parent sans enfant actif lié à l’établissement `403 Forbidden`. Pour l’accès de gestion, un établissement absent, désactivé ou hors périmètre renvoie `404 Not Found` après les contrôles de permission et de licence existants. Pour un parent sans droit de gestion, retirer le dernier lien avec un enfant de cet établissement bloque immédiatement la liste avec `403`, même avec un JWT encore valide.

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
- `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes` — `can_school_class_create`; crée avec `code`, `name` et éventuellement `series`. Réponse `201 Created` avec la classe.
- `PATCH /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` — `can_school_class_update`; modifie le code, le nom ou la série, indépendamment. Réponse `200 OK` avec la classe mise à jour.
- `DELETE /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` — `can_school_class_delete`; archive la classe.

Les paramètres de chemin `establishmentId`, `academicYearId` et, pour une fiche, `schoolClassId` sont des UUID. L’année doit être active et appartenir au même établissement que la classe. Ces routes requièrent un JWT valide, la permission indiquée, l’accès à cet établissement et la fonctionnalité `SCHOOL_MANAGEMENT` active dans sa licence. L’administrateur d’établissement reste limité à son école.

À la création, `code` (1 à 40 caractères) et `name` (1 à 100 caractères) sont obligatoires et ne peuvent pas être blancs. Le code est normalisé en majuscules. `series` est un libellé libre facultatif de 100 caractères maximum, par exemple `Scientifique` ou `Littéraire` : aucune liste de séries n’est imposée. Plusieurs classes peuvent partager la même série ; elle ne change pas l’unicité du code dans l’année. Les espaces de début et de fin sont retirés ; une valeur omise, `null`, vide ou blanche crée une classe sans série.

```json
{"code":"T-S-A","name":"Terminale A","series":"Scientifique"}
```

À la modification, chaque champ est facultatif : un champ omis ou `null` conserve sa valeur actuelle. Un code ou nom fourni doit rester non blanc. Pour changer uniquement la série :

```json
{"series":"Littéraire"}
```

Pour retirer la série, envoyer `{"series":""}` (une chaîne blanche a le même effet). Les anciennes requêtes contenant uniquement `code` et `name` restent acceptées et conservent la série existante lors d’un `PATCH`.

La création, la modification et la consultation d’une fiche renvoient `id`, `establishmentId`, `academicYearId`, `code`, `name`, `series`, `enabled`, `createdAt` et `updatedAt`. `series` vaut `null` si aucune série n’est définie, y compris pour les classes créées avant la migration V6. La liste renvoie un tableau de ces mêmes objets actifs, triés par nom ; la série figure également dans chaque entrée `establishment.classes` du dashboard.

Erreurs des routes de création/modification : corps, UUID ou valeur invalide (dont série dépassant 100 caractères) : `400 Bad Request` ; JWT absent, invalide ou expiré : `401 Unauthorized` ; permission ou licence insuffisante : `403 Forbidden` ; année, classe ou établissement absent, archivé ou hors périmètre : `404 Not Found` ; code déjà utilisé dans l’année, quelle que soit la série : `409 Conflict`. Les routes de consultation renvoient `200 OK`, ou `400`/`401`/`403`/`404` selon les mêmes règles de paramètres et d’accès.

### Salles ou groupes d’une classe

Une classe représente le niveau/groupe pédagogique parent (exemple : « Sixième », avec sa série éventuelle). Elle peut contenir plusieurs salles/groupes d’élèves (« Sixième A », « Sixième B »). Une salle appartient à une seule classe, une seule année et un seul établissement. La série et les matières restent définies sur la classe ; les frais et contenus ciblant cette classe concernent ses salles. Il ne s’agit pas d’une réservation de locaux physiques.

À la création d’une classe, une salle `DEFAULT` / `Salle principale` est créée automatiquement. Elle peut être renommée « Sixième A », puis complétée par une salle « Sixième B ». La migration V7 crée également une salle principale pour les classes existantes et y rattache les inscriptions, y compris historiques. Les identifiants d’inscription, présences, créances et paiements sont conservés.

Base des routes : `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms`. Tous les paramètres de chemin sont des UUID ; la classe et l’année doivent être actives et appartenir à l’établissement accessible au compte. JWT valide et fonctionnalité `SCHOOL_MANAGEMENT` active requis. L’administrateur d’établissement reste limité à son école. Les salles réutilisent les permissions des classes :

| Méthode et URL relative | Permission | Réponse et effet |
|---|---|---|
| `GET /classrooms` | `can_school_class_read` | `200` : tableau des salles actives, trié par nom puis identifiant. |
| `GET /classrooms/{classroomId}` | `can_school_class_read` | `200` : fiche de la salle active. |
| `POST /classrooms` | `can_school_class_create` | `201` : création avec `code` et `name` obligatoires. |
| `PATCH /classrooms/{classroomId}` | `can_school_class_update` | `200` : modification de `code` et/ou `name`. Champs omis ou `null` conservés. |
| `DELETE /classrooms/{classroomId}` | `can_school_class_delete` | `204` : archivage logique si aucune inscription ne référence la salle. |

Pour les URL relatives du tableau, le préfixe est celui de la classe (`.../classes/{schoolClassId}`). Le corps de création est par exemple :

```json
{"code":"B","name":"Sixième B"}
```

`code` est non blanc, limité à 40 caractères, nettoyé et mis en majuscules ; il est unique dans la classe, y compris parmi les salles archivées. Le même code `A` est permis dans plusieurs classes distinctes. `name` est non blanc, limité à 100 caractères et nettoyé. Un `PATCH` peut contenir uniquement `{"name":"Sixième B — groupe 2"}`. Le parent de la salle n’est pas modifiable.

Les réponses de création, modification et consultation contiennent `id`, `establishmentId`, `academicYearId`, `schoolClassId`, `code`, `name`, `enabled`, `createdAt` et `updatedAt`. La liste contient ces mêmes objets. Une salle ne peut être archivée si une inscription (même d’un élève archivé) ou un créneau actif la référence : `409`. Déplacer d’abord les élèves et archiver les créneaux concernés. Une classe sans salle active ne peut recevoir de nouvelle inscription.

Erreurs : UUID/corps/code/nom invalide : `400` ; JWT absent, invalide ou expiré : `401` ; permission ou licence insuffisante : `403` ; établissement, année, classe ou salle absent, archivé ou hors périmètre : `404` ; code déjà utilisé dans la classe ou archivage d’une salle référencée : `409`. Les données d’une autre classe, année ou école ne peuvent pas être sélectionnées en modifiant les UUID.

### Affectations classe-matière

- `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects` — permission `can_class_subject_read`.
- `PUT /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects/{subjectId}` — `can_class_subject_assign`; affecte ou réactive l’association. Réponse `200 OK` avec l’affectation.
- `DELETE /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects/{subjectId}` — `can_class_subject_unassign`; archive l’association.

La classe et la matière doivent appartenir au même établissement. Réaffecter une paire déjà active renvoie `409`; une référence absente ou hors périmètre renvoie `404`.

## Reprise de salles depuis l’année précédente

Base : `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classrooms/rollover`.
`establishmentId` et `academicYearId` sont des UUID ; l’année dans l’URL est l’année **cible**, existante et active.

Les deux opérations requièrent un JWT avec `can_school_year_rollover`, attribuée par V10 à `ESTABLISHMENT_ADMIN` et délégable aux rôles de l’établissement. Le service revérifie le compte actif/non verrouillé, les permissions actuelles en base, le rattachement à un employé actif du même établissement et la licence `SCHOOL_MANAGEMENT`. Un compte plateforme seul ou un parent seul n’a pas accès à cette opération. Renouveler le JWT après attribution de la permission.

### `GET .../classrooms/rollover`

Prépare la sélection sans modifier de données. Aucun paramètre de requête ni corps.
La source est l’année du même établissement dont la fin précède le début de la cible, avec la fin la plus récente (puis début, puis UUID en cas d’égalité). Elle peut être archivée. Une interruption entre deux années n’empêche donc pas la reprise de la dernière année disponible.

Réponse `200 OK` :

- `targetYear`, `sourceYear` : fiches d’années (`id`, `establishmentId`, `name`, `startsOn`, `endsOn`, `enabled`, dates d’audit). En l’absence d’année précédente, `sourceYear: null`, `classrooms: []`, `fees: []`.
- `classrooms` : objets `{classroom, schoolClass, subjects}`. Toutes les salles de la source, même archivées, sont proposées avec leur état ; la classe inclut son code, nom et sa série. `subjects` contient les matières encore actives et affectées activement à cette classe. Les matières sont définies au niveau de la classe, partagées entre ses salles.
- `financeAvailable` : droit de licence `TUITION` actif ; si faux, les trois listes financières sont vides.
- `fees` : définitions actives de frais annuels ou affectés aux classes de la source (format `TuitionFeeResponse` : identifiant, type, nom, montant, devise, échéance, portée et `schoolClassIds`). Les frais individuels sont exclus.
- `feeTypes`, `paymentMethods` : types et moyens de paiement actifs de l’établissement, au format des endpoints existants. Leur configuration est commune aux années ; elle est réutilisée sans duplication.

### `POST .../classrooms/rollover`

Reprend une ou plusieurs salles en une transaction. Réponse `200 OK`, y compris si les ressources ont déjà été reprises.

```json
{
  "sourceAcademicYearId": "11111111-1111-4111-8111-111111111111",
  "classroomIds": ["22222222-2222-4222-8222-222222222222", "33333333-3333-4333-8333-333333333333"],
  "includeAnnualFees": true,
  "feeOverrides": {
    "44444444-4444-4444-8444-444444444444": {
      "dueDate": "2027-10-15",
      "feeTypeId": "55555555-5555-4555-8555-555555555555"
    }
  }
}
```

- `sourceAcademicYearId` obligatoire : l’année source proposée par le GET. Une autre année est refusée.
- `classroomIds` obligatoire : 1 à 200 UUID distincts de salles de cette source et de cet établissement. Aucune salle supplémentaire n’est créée implicitement, notamment aucune salle `DEFAULT`.
- `includeAnnualFees` facultatif, défaut `true` (également pour `null`) : inclut les frais applicables à toute l’année. À `false`, seuls les frais des classes sélectionnées sont repris. Un frais annuel conserve sa portée annuelle, même si une seule salle est sélectionnée.
- `feeOverrides` facultatif : au plus 200 entrées, indexées par UUID de frais source effectivement retenus. Chaque objet accepte `dueDate` et/ou `feeTypeId`, tous deux facultatifs. Une entrée vide conserve les valeurs par défaut. Un UUID inconnu ou un frais exclu de la sélection donne `400`.

Règles de reprise :

1. Copier les classes parentes une seule fois (`code`, `name`, `series`), puis les salles sélectionnées (`code`, `name`) en état actif. Les sources archivées sont conservées et peuvent être reprises.
2. Réutiliser les matières actives et recopier leurs affectations à la classe cible ; réactiver une affectation cible désactivée. Une matière source devenue inactive/absente est ignorée et signalée par `skippedSubjectIds`. Une classe sans matière est acceptée.
3. Copier les définitions de frais actives, leurs types, montants, devises et échéances, pour les seules classes reprises. Un frais multiclasse est créé une fois ; les lots suivants ajoutent les classes nouvellement reprises à cette même définition. Les types et modes de paiement sont communs à l’établissement : leurs identifiants et paramètres existants sont conservés. Un type archivé n’est pas réactivé : fournir un `feeTypeId` actif de la même école. Pour un ancien frais sans identifiant de type, un type actif de même code est réutilisé, sinon un remplacement explicite est nécessaire.
4. Décaler l’échéance du nombre d’années civiles séparant les débuts des deux années scolaires (29 février vers 28 février si nécessaire). L’échéance doit appartenir à la cible ; utiliser `feeOverrides.dueDate` si les calendriers diffèrent.
5. Réutiliser les ressources déjà liées par une reprise antérieure. Sans lien, une classe de même code est réutilisée si son nom et sa série correspondent ; une salle de même code dans la classe doit avoir le même nom. Une ressource cible archivée ou incompatible donne `409`. Les modifications ultérieures de code/nom/série d’une classe ou salle déjà liée sont conservées.
6. Un frais cible de même nom n’est réutilisé que si son montant, devise, échéance, type et portée (annuelle ou classes, sans élève individuel) correspondent. Seules les classes du périmètre peuvent être ajoutées. Aucun montant ou paramètre existant n’est écrasé. Une divergence, y compris sur un frais déjà lié, donne `409`.
7. **Seul le paramétrage financier est repris.** Aucun élève, inscription, charge/créance, impayé, paiement, reçu ou séquence de reçus n’est copié ni modifié. Aucune facturation automatique des élèves déjà inscrits dans la cible : l’application ultérieure relève de `POST .../tuition-fees/{feeId}/apply`. Les frais copiés s’appliqueront normalement aux futures inscriptions, avec règlement complet ou avance explicitement acceptée. Les emplois du temps et contenus pédagogiques ne sont pas repris par cette opération.
8. Les années sont verrouillées pour sérialiser les reprises concurrentes et éviter les doublons. Toute erreur annule les créations, extensions, affectations et liens de la demande entière. La provenance et l’auteur sont conservés en base ; les sources restent inchangées.

Exemple de réponse :

```json
{
  "sourceAcademicYearId": "11111111-1111-4111-8111-111111111111",
  "targetAcademicYearId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  "classes": [{"sourceId": "66666666-6666-4666-8666-666666666666", "targetId": "77777777-7777-4777-8777-777777777777", "created": true}],
  "classrooms": [{"sourceId": "22222222-2222-4222-8222-222222222222", "targetId": "88888888-8888-4888-8888-888888888888", "created": true}],
  "subjectAssignmentsCreated": 2,
  "skippedSubjectIds": [],
  "fees": [{"sourceId": "44444444-4444-4444-8444-444444444444", "targetId": "99999999-9999-4999-8999-999999999999", "created": true}],
  "createdBy": "administrateur@example.org"
}
```

`created: false` indique une ressource réutilisée ; un frais réutilisé peut avoir reçu de nouvelles classes. `subjectAssignmentsCreated` compte les affectations créées ou réactivées pendant cette demande. La réponse ci-dessus illustre une seule salle ; chaque salle demandée a son entrée dans `classrooms`.

Erreurs : `400` pour corps/UUID invalides, liste vide/doublonnée, mauvaise année source, type ou échéance invalides ; `401` sans JWT valide ; `403` sans permission/compte/licence requis (`TUITION` aussi si des frais sont à reprendre) ; `404` pour établissement/année cible hors périmètre ou inactif, salle hors source, ou absence de précédente au POST ; `409` pour collision incompatible, cible archivée ou conflit d’intégrité. Les erreurs métier renvoient `application/problem+json` avec un détail et aucune écriture partielle.

## Emplois du temps par salle

Les emplois du temps sont définis pour une salle/groupe d’élèves (`classroomId`, par exemple « Sixième A ») d’une classe et d’une année. Deux groupes d’une même classe peuvent avoir des cours simultanés avec des ressources différentes. Le champ facultatif `roomCode` identifie séparément un local physique partagé, par exemple `LAB-A`.

Base des routes par salle, notée `B` ci-dessous : `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable`.

Tous les identifiants de chemin sont des UUID. Le compte authentifié doit être actif, non verrouillé, rattaché à un membre du personnel actif de cette école, avec la permission de l’action. Le lien au compte, les rôles actifs et les permissions actuelles sont revérifiés en base ; un JWT contenant une ancienne permission ne suffit pas après révocation. La fonctionnalité `SCHOOL_MANAGEMENT` doit être active. L’école, l’année, la classe et la salle doivent être actives et cohérentes. Aucun accès parent n’est ajouté. Le rôle plateforme `ADMIN` n’obtient pas ces permissions par défaut.

La migration V9 attribue `can_timetable_read`, `can_timetable_create`, `can_timetable_update`, `can_timetable_delete` et `can_timetable_copy` à `ESTABLISHMENT_ADMIN`. Ces droits sont assignables via les rôles locaux. Renouveler le jeton après attribution.

| Méthode et URL | Permission | Réponse |
|---|---|---|
| `GET B` | `can_timetable_read` | `200` : créneaux récurrents actifs ; filtre facultatif `dayOfWeek`. |
| `GET B/{entryId}` | `can_timetable_read` | `200` : fiche d’un créneau actif de cette salle. |
| `POST B` | `can_timetable_create` | `201` : créneau créé. |
| `PUT B/{entryId}` | `can_timetable_update` | `200` : remplacement complet des paramètres du créneau. |
| `DELETE B/{entryId}` | `can_timetable_delete` | `204` : archivage logique du créneau. |
| `GET B/day?date=YYYY-MM-DD` | `can_timetable_read` | `200` : occurrences de la journée. |
| `GET B/week?date=YYYY-MM-DD` | `can_timetable_read` | `200` : semaine contenant la date, du lundi au dimanche. |
| `POST B/copy` | `can_timetable_copy` | `201` : liste des nouveaux créneaux de la salle cible. `can_timetable_delete` est aussi requis pour remplacer la destination. |

### Création et modification d’un créneau

Le `POST` et le `PUT` prennent le même corps :

```json
{
  "subjectId":"<uuid-matiere>",
  "teacherId":"<uuid-membre-personnel>",
  "dayOfWeek":"MONDAY",
  "startTime":"08:00",
  "endTime":"09:00",
  "validFrom":"2026-09-01",
  "validUntil":"2027-06-30",
  "roomCode":"LAB-A"
}
```

- `subjectId`, `dayOfWeek`, `startTime`, `endTime` sont obligatoires. La matière doit être active et affectée à la classe parente de la salle.
- `teacherId` est facultatif ; `null` signifie enseignant à affecter. Une valeur renseignée doit désigner un membre du personnel actif de la même école ; aucun rôle métier « enseignant » n’est imposé dans le modèle actuel du personnel.
- `dayOfWeek` vaut `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY` ou `SUNDAY`. Le filtre de liste utilise les mêmes valeurs.
- Les heures sont locales à l’école, sans conversion UTC, en minutes entières (`HH:mm` ou `HH:mm:00`). La fin doit être strictement après le début dans la même journée ; pas de créneau passant minuit ni de `24:00`.
- `validFrom` et `validUntil` sont des dates inclusives dans l’année. Une valeur omise ou `null` prend respectivement le début et la fin de l’année. La période doit contenir au moins une occurrence du jour choisi.
- `roomCode` est facultatif, limité à 40 caractères, nettoyé et normalisé en majuscules ; vide/blanc devient `null`. Il s’agit d’un libellé de local physique, sans catalogue supplémentaire.

Le `PUT` remplace l’ensemble de ces paramètres : omettre `teacherId` ou `roomCode` les remet à `null`, et omettre les dates rétablit les bornes de l’année. L’identifiant, la salle, la classe, l’année, l’auteur/date de création et la provenance de copie sont conservés. Le déplacement d’un créneau vers une autre salle passe par une copie puis l’archivage de l’ancien.

Les réponses de fiche/création/modification et les entrées des listes contiennent : `id`, `establishmentId`, `academicYearId`, `schoolClassId`, `classroomId`, `subjectId`, `teacherId`, `dayOfWeek`, `startTime`, `endTime`, `validFrom`, `validUntil`, `roomCode`, `enabled`, `copiedFromId`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`. Les heures sont renvoyées au format `HH:mm:ss`. `copiedFromId` est `null` pour une création directe et référence le créneau d’origine pour une copie. Les listes sont triées par jour, heure de début puis identifiant. L’archivage conserve les données et l’auteur de l’opération ; le créneau disparaît des lectures actives.

### Conflits de planning

Deux créneaux actifs sont incompatibles s’ils partagent un jour de semaine, une occurrence dans leurs périodes inclusives et des heures qui se recouvrent, avec au moins une ressource commune : salle/groupe, enseignant renseigné ou `roomCode` renseigné. Un cours de 08:00 à 09:00 peut être suivi d’un autre à 09:00. Les contrôles portent sur toutes les années actives de l’école lorsque leurs dates se recouvrent ; une année différente ne rend pas l’enseignant disponible automatiquement.

Les jours non ouvrés retirent les occurrences des vues datées mais ne libèrent pas les réservations du modèle : retirer ensuite un jour férié ne doit pas créer de double réservation. Les écritures des emplois du temps d’une école sont sérialisées pour éviter deux réservations concurrentes, y compris lorsque les salles n’ont encore aucun créneau.

### Vues journalière et hebdomadaire

`date` est obligatoire et doit appartenir à l’année. Une journée renvoie :

```json
{"date":"2026-09-07","withinAcademicYear":true,"nonWorkingDay":null,"entries":[]}
```

`entries` contient les créneaux de ce jour dans leur période de validité. Pour un jour non ouvré configuré, le tableau est vide et `nonWorkingDay` contient son libellé. Aucun week-end n’est fermé implicitement.

La vue semaine renvoie `startsOn` (lundi), `endsOn` (dimanche) et `days` (sept objets de journée). Aux bords de l’année, les jours hors année ont `withinAcademicYear: false` et aucune entrée. Le modèle renvoyé par `GET B` reste consultable intégralement, sans filtrage des vacances.

### Duplication vers une autre salle

`POST B/copy` prend par exemple :

```json
{
  "targetClassroomId":"<uuid-salle-destination>",
  "replaceExisting":false,
  "copyTeachers":false,
  "copyRoomCodes":false,
  "teacherMappings":{"<uuid-enseignant-source>":"<uuid-enseignant-destination>"}
}
```

`targetClassroomId` est obligatoire. La cible doit être différente de la source, active et dans la même école et année. Elle peut appartenir à une autre classe si toutes les matières copiées y sont déjà affectées et actives. Une source vide est refusée.

Les trois options booléennes sont facultatives, avec `false` par défaut (également pour `null`) :

- `replaceExisting: false` ajoute les créneaux à la cible en conservant ses créneaux actuels. Avec `true`, **tous les créneaux actifs de la cible sont archivés**, puis remplacés par la copie ; il faut aussi `can_timetable_delete` dans le jeton et en base.
- `copyTeachers: false` laisse les enseignants non affectés, sauf ceux indiqués par `teacherMappings`. Avec `true`, les enseignants source sont conservés en l’absence de remplacement explicite. Conserver un enseignant déjà réservé aux mêmes heures par la source provoque `409`.
- `copyRoomCodes: false` laisse les locaux physiques non affectés. Avec `true`, leurs codes sont conservés et restent soumis au contrôle de conflit, y compris avec la source.

`teacherMappings` est un objet facultatif de zéro à 100 paires UUID source → UUID destination, sans valeurs nulles. Chaque source doit être un enseignant présent dans le planning source ; les enseignants destination doivent être actifs dans cette école. Le mapping prime sur `copyTeachers`.

`validFrom` et `validUntil` peuvent être fournis pour redéfinir la période de toutes les copies ; sinon chaque créneau conserve ses dates source. Les nouvelles périodes doivent rester dans l’année et contenir une occurrence de leur jour.

Tous les créneaux candidats et leurs références sont validés avant le remplacement. Le contrôle inclut les conflits avec la source, la cible conservée, les autres salles et les autres copies. Un échec ne modifie ni source ni destination. Réponse `201 Created` : tableau des nouveaux créneaux avec de nouveaux UUID, leur `copiedFromId` et le compte authentifié comme auteur. La source reste intacte. Répéter une copie aux mêmes horaires est normalement refusé par les conflits ; aucun doublon n’est silencieusement ignoré.

### Jours non ouvrés de l’année

Base : `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/timetable/non-working-days`.

- `GET` requiert `can_timetable_read` et renvoie `200` avec les objets `{date,name}` triés par date.
- `PUT` requiert `can_timetable_update` et remplace toute la liste pour cette année dans l’école. Corps : `{"days":[{"date":"2026-12-25","name":"Jour férié"}]}`. La liste est obligatoire, de zéro à 730 entrées non nulles, avec des dates uniques dans l’année et des noms non blancs de 100 caractères maximum. `{"days":[]}` efface la liste. Réponse `200` avec la liste normalisée. Un corps invalide laisse la liste précédente intacte.

Ces jours s’appliquent à toutes les salles de cette année et seulement aux vues datées des emplois du temps ; les règles du module de présence restent distinctes.

### Erreurs et sécurité

- `400 Bad Request` : UUID, corps, jour, heure, date, filtre ou mapping invalide ; période sans occurrence ; source vide ou identique à la cible ; calendrier incohérent.
- `401 Unauthorized` : JWT absent, invalide ou expiré.
- `403 Forbidden` : permission insuffisante (y compris remplacement sans droit de suppression), compte/employé inactif ou non lié, permission révoquée, ou licence `SCHOOL_MANAGEMENT` absente.
- `404 Not Found` : école, année, classe, salle, matière affectée, enseignant ou créneau absent, archivé ou hors périmètre ; cible d’une autre école/année.
- `409 Conflict` : chevauchement ou conflit d’intégrité. Le conflit métier renvoie un `ProblemDetail` avec `conflictingEntryId`, identifiant du premier créneau incompatible dans cette école. Aucun détail SQL n’est exposé.

Les clés étrangères vérifient aussi en base le périmètre de la salle, de la matière affectée, de l’enseignant et de la provenance de copie. Les créneaux des classes, salles ou années archivées restent conservés mais sont exclus des lectures et réservations actives.

## Inscriptions

Une inscription rattache un élève actif à une classe et à une seule salle pour une année scolaire active. Lorsqu’un frais actif s’applique à l’élève, son inscription doit inclure le règlement intégral des frais applicables, ou un versement positif avec acceptation explicite d’une avance. Sans frais applicables, aucun paiement n’est requis. L’élève, l’année, la classe et la salle doivent appartenir au même établissement ; la salle doit appartenir à cette classe et à cette année. Un élève ne peut avoir qu’une inscription par année (`409` en cas de doublon), même si une autre salle est choisie. Seule la salle peut être modifiée dans la même classe via le `PATCH` ci-dessous ; l’inscription n’est pas supprimable. Les années suivantes créent de nouveaux enregistrements et préservent l’historique.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments`

Requiert `can_enrollment_read`. Retourne les inscriptions de l’année, triées par date de création, avec leur `classroomId` unique. Réponse `200 OK` : tableau d’objets `id`, `establishmentId`, `studentId`, `academicYearId`, `schoolClassId`, `classroomId`, `advanceAccepted`, `paymentDecisionBy`, `createdAt`. `advanceAccepted` conserve l’autorisation donnée à la création ; il n’est pas recalculé lorsque le solde est réglé plus tard. Pour une inscription antérieure à V8, ce drapeau vaut `false` et l’auteur vaut `null`, sans attester d’un règlement intégral historique.

### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments`

Requiert `can_enrollment_create`. Si le corps contient au moins un paiement, `can_payment_record` est également obligatoire. Les fonctionnalités `STUDENT_RECORDS` et `SCHOOL_MANAGEMENT` doivent être actives ; `TUITION` est aussi exigée si des frais actifs s’appliquent. Le compte reste limité à son établissement.

```json
{
	"studentId": "<uuid-eleve>",
	"schoolClassId": "<uuid-classe>",
	"classroomId": "<uuid-salle>",
	"acceptAdvance": true,
	"payments": [
		{"feeId":"<uuid-frais>","amount":25000,"paymentMethodCode":"CASH","reference":"versement-inscription"}
	]
}
```

`studentId` et `schoolClassId` sont obligatoires. `classroomId` peut être omis ou `null` uniquement si la classe possède exactement une salle active : celle-ci est alors sélectionnée. Avec plusieurs salles actives, le choix explicite est obligatoire (`400` sinon) ; sans salle active, l’inscription est refusée (`400`). Une salle inactive ou d’une autre classe, année ou école renvoie `404`.

`acceptAdvance` est facultatif : absent, `null` ou `false`, tous les frais applicables doivent être soldés. Avec `true`, l’opérateur accepte une avance : au moins un versement strictement positif sur un frais applicable est exigé ; les autres soldes, y compris ceux de frais non couverts par l’avance, restent suivis. Cette option ne permet jamais une inscription gratuite si des frais s’appliquent. La décision et l’utilisateur authentifié sont enregistrés dans `advanceAccepted` et `paymentDecisionBy`, et restent conservés lors d’un changement de salle.

Les frais applicables sont les définitions actives du même établissement et de la même année, de portée annuelle, de portée classe (y compris les sélections multiples contenant la classe choisie) ou visant cet élève. Les frais d’une autre classe, année ou personne et les définitions désactivées sont exclus. Une échéance future n’exclut pas un frais : utiliser l’acceptation d’une avance pour un versement initial partiel. Des créances déjà payées pour ces mêmes frais/élève/année sont réutilisées sans nouvel encaissement ni doublon.

`payments` est facultatif, omis ou `null` signifiant liste vide, et contient au maximum 100 entrées non nulles. Chaque entrée comprend :

- `feeId` : UUID d’un frais applicable, unique dans la requête ; il s’agit de l’identifiant de définition renvoyé par la liste existante des frais de l’année, pas d’un identifiant de créance.
- `amount` : montant strictement positif, au maximum 10 chiffres entiers et 2 décimales, sans dépasser le solde de ce frais. La devise est celle du frais, déterminée côté serveur.
- `paymentMethodCode` : moyen de paiement actif dans cet établissement, non blanc, 40 caractères maximum ; nettoyé et normalisé en majuscules.
- `reference` : facultative, 120 caractères maximum ; une valeur vide/blanche devient `null`.

Pour un règlement intégral, omettre `acceptAdvance` (ou le fixer à `false`) et fournir les montants nécessaires pour solder chaque frais applicable, dans sa propre devise. Si aucun frais ne s’applique, omettre `payments` ou envoyer `[]`. Un paiement visant un frais non applicable est refusé.

La transaction crée l’inscription, génère/réutilise ses créances, enregistre les paiements et valide la décision. Un refus annule toutes les nouvelles écritures de la tentative, y compris un premier paiement valide si le suivant échoue ; aucun élève n’est ajouté à la classe après une tentative échouée. Les créations de frais et d’inscriptions d’une même année sont sérialisées afin de vérifier une grille cohérente. Les numéros de reçus peuvent présenter des trous après annulation, sans réutilisation.

Réponse `201 Created` avec les champs d’inscription ci-dessus et `payments`, tableau des encaissements créés par cette requête. Chaque paiement contient `id`, `feeId`, `chargeId`, `receiptNumber`, `amount`, `currency`, `paymentMethodCode`, `paymentMethodName`, `reference`, `receivedAt`, `recordedBy`, `totalPaid` et `balance`. `totalPaid` et `balance` concernent sa créance, dans sa devise. Le reçu détaillé reste consultable via `GET /api/v1/establishments/{establishmentId}/payments/{paymentId}/receipt` avec `can_payment_receipt_read`. Les versements ultérieurs utilisent l’endpoint existant de paiement d’une créance.

Erreurs : corps/UUID/montant invalide, mode de paiement inactif/inconnu, doublon de `feeId` ou liste invalide : `400` ; JWT absent/invalide/expiré : `401` ; permission, licence ou périmètre du compte insuffisant : `403` ; élève, année, classe, salle ou frais absent, archivé ou hors périmètre : `404` ; inscription déjà existante, surpaiement ou paiement ne satisfaisant pas la décision : `409`. Un refus de paiement obligatoire renvoie un `ProblemDetail` avec `code: "ENROLLMENT_PAYMENT_REQUIRED"` et `unpaidFeeIds` (frais non soldés par la tentative). Aucun paiement de la tentative n’est conservé ; renvoyer l’ensemble des versements souhaités après correction.

La migration V8 préserve les inscriptions et paiements antérieurs, sans inventer de règlement ni d’auteur. Les nouveaux frais créés après une inscription restent des créances à régler ; ils n’annulent pas rétroactivement l’inscription existante. Les contrôles du paiement concernent la création, et un changement de salle ne rejoue aucun encaissement.

### `PATCH /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments/{enrollmentId}`

Requiert `can_enrollment_update`, attribuée par la migration V7 à `ESTABLISHMENT_ADMIN` et assignable via les rôles locaux. Renouveler le jeton après attribution. Les identifiants de chemin sont des UUID ; l’inscription doit appartenir à l’année et à l’établissement accessibles au compte.

```json
{"classroomId":"<uuid-nouvelle-salle>"}
```

`classroomId` est obligatoire, non nul et doit désigner une salle active de la même classe. L’élève, la classe et l’année doivent rester actifs. La réponse `200 OK` contient l’inscription avec sa nouvelle salle ; son `id`, sa date de création, son élève, sa classe et son année restent les mêmes. Les présences restent liées à cette inscription et aucun frais n’est réappliqué. Réaffecter la salle actuelle est permis. La distribution par salle reflète l’affectation actuelle ; aucun historique séparé des changements de salle n’est exposé.

Erreurs : corps/UUID invalide ou `classroomId` absent/null : `400` ; JWT absent/invalide/expiré : `401` ; permission ou licence insuffisante : `403` ; inscription, élève, année, classe ou salle absent, archivé ou hors périmètre (y compris une salle d’une autre classe) : `404`.

### `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/students`

Requiert `can_enrollment_read`. Retourne les élèves actifs inscrits dans cette classe pour l’année indiquée. Le paramètre de requête facultatif `classroomId` (UUID) limite le résultat aux élèves de cette salle ; sans filtre, toutes les salles de la classe sont incluses. Exemple : `GET .../classes/{schoolClassId}/students?classroomId={uuid-salle}`. Réponse `200 OK` : tableau de `id`, `studentNumber`, `firstName`, `lastName`. Une classe ou salle absente, archivée ou hors périmètre renvoie `404` ; filtre UUID invalide : `400` ; JWT absent/invalide : `401` ; permission ou licence insuffisante : `403`.

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

Les types de frais sont configurés par établissement et désactivables sans supprimer les frais historiques. Chaque nouvelle définition de frais doit référencer un type actif (`feeTypeId`); le code et le nom du type sont copiés dans la définition pour préserver l’historique. Les frais sont définis dans le périmètre d’une année scolaire, avec une portée facultative sur une ou plusieurs classes, ou sur un élève. `schoolClassIds` permet de sélectionner plusieurs classes simultanément ; l’ancien champ `schoolClassId` reste accepté pour une seule classe. Un frais sans sélection de classes ni `studentId` s’applique à toute l’année. Sa création crée immédiatement une créance pour chaque élève actuellement inscrit qui correspond à la portée. Une créance conserve une copie du montant, de la devise, de l’échéance et des informations de l’élève; le frais d’origine n’est pas modifiable par l’API. Les nouvelles inscriptions appliquent automatiquement les frais de leur classe ; `POST .../apply` permet également de les réappliquer sans créer de doublons.

La migration vers ce modèle désactive les anciennes définitions ciblées par niveau afin de ne pas les convertir en frais globaux. Les créances et paiements déjà enregistrés sont conservés.

La migration V4 ajoute la table `tuition_fee_classes` et reprend les affectations existantes à une seule classe. Les frais annuels ou individuels conservent leur portée. Le nom du frais reste unique par établissement et année : sélectionner plusieurs classes crée une seule définition, sans dupliquer son nom.

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

Requiert `can_tuition_read`. Retourne les définitions de frais de l’année scolaire, avec `schoolClassIds` contenant toutes les classes ciblées. Le tableau est vide pour une portée annuelle ou individuelle. `schoolClassId` conserve sa valeur pour une seule classe et vaut `null` pour plusieurs classes. `scope` vaut `SCHOOL_CLASS` dans les deux cas.

### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees`

Requiert un JWT Bearer et `can_tuition_create`, dans le périmètre de gestion de l’établissement avec la fonctionnalité `TUITION` active. Les paramètres de chemin `establishmentId` et `academicYearId` sont des UUID obligatoires ; l’année doit être active dans cet établissement. Aucun paramètre de requête.

Crée un seul frais et ses créances pour les élèves inscrits correspondants. Le corps JSON contient `name` (non vide, 120 caractères maximum), `feeTypeId` (UUID d’un type actif de cet établissement), `amount` (strictement positif, au plus 10 chiffres avant et 2 après la virgule), `currency` (3 lettres, normalisées en majuscules) et `dueDate` (date ISO comprise dans l’année scolaire).

La sélection facultative détermine la portée :

- `schoolClassIds` : tableau non vide d’UUID distincts et non nuls, pour affecter le même frais à plusieurs classes simultanément ; une liste d’une seule classe est également acceptée.
- `schoolClassId` : UUID d’une seule classe, conservé pour compatibilité ; ne peut pas être fourni avec `schoolClassIds`.
- `studentId` : UUID d’un élève inscrit dans cette année ; ne peut être combiné avec aucun ciblage de classe.
- Sans sélection de classe et sans élève (champs absents ou `null`), le frais s’applique à toute l’année. Une liste vide est refusée pour éviter une portée annuelle involontaire.

Toutes les classes sélectionnées doivent être actives dans le même établissement et la même année. Le montant, la devise et l’échéance s’appliquent à chaque élève concerné ; le montant n’est pas partagé entre les classes. La requête est atomique : une seule classe invalide empêche la création du frais et de toutes ses créances.

```json
{
	"name": "Frais de scolarité - période 1",
	"feeTypeId": "<uuid-type-frais>",
	"amount": 125000,
	"currency": "XOF",
	"dueDate": "2026-10-31",
	"schoolClassIds": ["<uuid-classe-A>", "<uuid-classe-B>"]
}
```

Réponse `201 Created` avec `fee` (incluant `feeTypeId`, `feeTypeCode`, `feeTypeName`, `schoolClassIds`, `scope` et le champ historique `schoolClassId`) et `chargesCreated`, nombre total de créances créées sur toutes les classes choisies. `scope` reste `SCHOOL_CLASS` pour une ou plusieurs classes ; `schoolClassId` vaut l’unique UUID s’il n’y en a qu’un, sinon `null`. `schoolClassIds` contient la sélection persistée, sans ordre garanti. Les élèves des autres classes ne sont pas facturés et une classe sans élève peut être sélectionnée sans créer immédiatement de créance.

Erreurs : corps/UUID/montant/devise/échéance invalide, tableau vide ou contenant des doublons/valeurs nulles, combinaison de champs de portée incompatible : `400 Bad Request`. JWT absent, invalide ou expiré : `401 Unauthorized`. Permission, licence ou accès à l’établissement refusé : `403 Forbidden`. Type, classe, élève ou année absents, désactivés ou hors périmètre : `404 Not Found`. Nom déjà utilisé dans l’année, y compris pour une autre sélection de classes : `409 Conflict`.

### `POST /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/{feeId}/apply`

Requiert `can_tuition_apply`. Applique la définition aux inscriptions présentes de toutes les classes ciblées, utile après l’ajout de nouveaux élèves. La sélection de classes persistée est réutilisée ; un frais pour plusieurs classes ne devient jamais annuel lors de la réapplication. L’opération est idempotente : une créance existante n’est pas dupliquée. Réponse `200 OK` avec `chargesCreated`, total des nouvelles créances.

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

Toute personne peut être tuteur, sans condition de parenté ni restriction de rôle : nouveau contact, tuteur existant, membre du personnel ou administrateur d’établissement. Un tuteur peut être lié à plusieurs élèves, y compris dans plusieurs établissements. L’adresse e-mail permet de retrouver une fiche tuteur ou un compte existant. Ce compte est réutilisé et reçoit le rôle `PARENT` en complément de ses rôles actuels, sans changement de mot de passe ni nouvelle invitation. Ses éventuels états désactivé ou verrouillé sont conservés. Si aucun compte n’existe, une invitation à usage unique valable 48 heures permet de créer l’accès. Le tuteur se connecte par `POST /api/v1/login` avec ses identifiants existants ou ceux choisis lors de l’activation. Un lien retiré supprime uniquement l’accès à l’élève concerné.

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

Requiert un JWT et `can_guardian_link`. Les paramètres de chemin `establishmentId` et `studentId` sont des UUID ; l’élève doit appartenir à l’établissement accessible à l’appelant. Le rôle de la personne désignée comme tuteur n’impose aucune restriction. Le lien n’accorde aucun droit de gestion sur l’établissement.

Le corps JSON contient `firstName` et `lastName` (obligatoires, non vides, 100 caractères maximum) et `email` (obligatoire, adresse valide, 254 caractères maximum). L’e-mail est normalisé en minuscules après retrait des espaces périphériques. Les noms servent à créer la fiche ; une fiche existante est réutilisée sans être renommée. Si un compte existe déjà pour ce login, il est rattaché à la fiche tuteur et conserve son identité, ses rôles et son mot de passe. Si aucun compte n’existe, un courriel d’invitation est envoyé.

```json
{
	"firstName": "Grace",
	"lastName": "Hopper",
	"email": "grace@example.org"
}
```

Réponse `201 Created` : `guardian` identifie le tuteur et `accessStatus` vaut `INVITATION_SENT` pour une invitation ou `ACCESS_ALREADY_ACTIVE` pour un compte réutilisé. Exemple pour un administrateur déjà inscrit :

```json
{
	"guardian": {
		"id": "<uuid-tuteur>",
		"firstName": "Grace",
		"lastName": "Hopper",
		"email": "grace@example.org"
	},
	"accessStatus": "ACCESS_ALREADY_ACTIVE"
}
```

Un compte existant ne constitue pas à lui seul un conflit. Un lien déjà présent, un conflit d’unicité ou une adresse réattribuée dont la fiche tuteur appartient encore à un autre compte renvoie `409 Conflict` ; une fiche élève absente, archivée, un tuteur désactivé ou un établissement hors périmètre renvoie `404 Not Found`. Corps invalide : `400 Bad Request` ; JWT absent/invalide/expiré : `401 Unauthorized` ; permission ou périmètre de gestion refusé : `403 Forbidden`. Pour un compte qui vient de recevoir `PARENT`, se reconnecter ou renouveler les jetons afin d’obtenir `can_guardian_read_children`.

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

Requiert la permission `can_guardian_read_children`, attribuée au rôle `PARENT`, y compris lorsqu’il complète un rôle de personnel ou d’administrateur. Retourne uniquement les élèves actifs liés au compte parent ainsi que les établissements actifs correspondants. La fiche tuteur est résolue par l’identifiant du compte authentifié : un changement de login du personnel ne fait pas perdre les liens existants. Aucun identifiant d’élève fourni par le client ne peut élargir ce périmètre.

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

Les contenus appartiennent obligatoirement à une classe d’une année scolaire et à une matière de cette classe, et sont de type `LESSON` ou `ASSIGNMENT`. Leur état suit `DRAFT -> PUBLISHED -> ARCHIVED`; seule une ressource publiée est consultable par un parent. `subjectId` est obligatoire et doit désigner une affectation active de matière à la classe ciblée dans le même établissement. Les leçons n’acceptent pas de date limite. Le personnel doit disposer des permissions `can_learning_content_read`, `can_learning_content_create`, `can_learning_content_update`, `can_learning_content_publish` ou `can_learning_content_archive` selon l’action. L’établissement doit avoir la fonctionnalité `LEARNING_CONTENT` active.

Base des routes staff : `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content`.

- `GET` — liste paginée; filtres facultatifs `type`, `page` (défaut 0), `size` (défaut 20, maximum 100). Requiert `can_learning_content_read`.
- `POST` — crée un brouillon avec `type`, `title` et `subjectId` obligatoires ; `description`, `body` et `dueDate` sont facultatifs selon les règles ci-dessus. Requiert `can_learning_content_create`; réponse `201 Created`.
- `GET /{contentId}` — détail non archivé, brouillon inclus. Requiert `can_learning_content_read`.
- `PUT /{contentId}` — remplace les champs éditables d’un contenu non archivé ; `subjectId` doit toujours être fourni et son affectation à la classe est revérifiée. Son type et sa classe ne peuvent pas changer. Requiert `can_learning_content_update`.
- `POST /{contentId}/publish` — publie un brouillon après vérification de son rattachement à la matière et à la classe ; si la matière a été retirée de la classe, publication refusée avec `400`. Requiert `can_learning_content_publish`.
- `POST /{contentId}/archive` — archive un contenu sans supprimer l’historique. Requiert `can_learning_content_archive`.
- `POST /{contentId}/attachments` (`multipart/form-data`, champ `file`) — ajoute une pièce jointe uniquement à un brouillon. Requiert `can_learning_content_update`; réponse `201 Created`.
- `GET /{contentId}/attachments` — liste les métadonnées sans clé de stockage. Requiert `can_learning_content_read`.
- `GET /{contentId}/attachments/{attachmentId}` — téléchargement privé autorisé après vérification du périmètre contenu/classe. Requiert `can_learning_content_read`.

### Rattachement obligatoire des cours et devoirs

Les routes existantes de création (`POST` sur la base ci-dessus), de modification (`PUT` avec `/{contentId}`) et de publication (`POST` avec `/{contentId}/publish`) sont conservées. Les paramètres de chemin `establishmentId`, `academicYearId`, `schoolClassId` et, le cas échéant, `contentId` sont des UUID obligatoires. L’année et la classe doivent être actives et appartenir au même établissement autorisé. Le contenu doit appartenir exactement à cette classe et cette année ; une autre URL ne permet pas de le déplacer.

À la création comme au remplacement par `PUT`, le corps JSON contient `type` (`LESSON` ou `ASSIGNMENT`), `title` (non vide, 160 caractères maximum) et `subjectId` (UUID non nul d’une matière affectée à cette classe). `description` (2 000 caractères maximum), `body` (100 000 caractères maximum) et `dueDate` (date ISO, autorisée uniquement pour un devoir) restent facultatifs. Exemple de devoir :

```json
{
	"type": "ASSIGNMENT",
	"title": "Exercices sur les fractions",
	"subjectId": "44444444-4444-4444-8444-444444444444",
	"description": "Exercices 1 à 5",
	"dueDate": "2026-11-02"
}
```

La classe est transmise dans l’URL, pas dans le corps. Pour sélectionner une matière valide, utiliser `GET /api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects` avec `can_class_subject_read`. La publication ne prend ni corps ni paramètre de requête et réévalue l’affectation de la matière.

Les réponses `201 Created` (création) ou `200 OK` (modification/publication) sont des `LearningContentResponse` avec notamment `schoolClassId` et `subjectId` non nuls. Une modification replace le contenu en `DRAFT` ; une publication réussie le passe en `PUBLISHED`.

Sécurité et erreurs : JWT Bearer requis (`401` si absent, invalide ou expiré) ; permission de l’action et périmètre/licence requis (`403` si refusés). Une matière absente, nulle, inconnue, d’un autre établissement ou non affectée à la classe renvoie `400 Bad Request`, ainsi qu’un UUID mal formé ou un corps invalide. Classe, année ou contenu absent, archivé ou dans un autre périmètre : `404 Not Found`. Publication d’un contenu qui n’est pas un brouillon ou conflit d’intégrité : `409 Conflict`. Ces règles s’appliquent aux deux types de contenu.

Les uploads acceptent PDF, PNG, JPG/JPEG, WEBP, TXT, DOCX et PPTX uniquement avec leur type MIME autorisé correspondant. La limite par défaut est 10 MiB (`LEARNING_MAX_FILE_SIZE_BYTES` et `spring.servlet.multipart.max-file-size`), stockée hors du webroot dans `${LEARNING_STORAGE_ROOT}` ou le dossier temporaire privé par défaut. Le serveur crée une clé UUID; aucun chemin disque n’est renvoyé. Une pièce jointe n’est pas supprimable séparément; archiver le contenu masque ses pièces jointes aux parents.

Base des routes parent : `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content`.

- `GET` — liste paginée des seuls contenus `PUBLISHED` de la classe où l’enfant est inscrit; accepte `type`, `page`, `size`.
- `GET /{contentId}` — détail publié uniquement.
- `GET /{contentId}/attachments` et `GET /{contentId}/attachments/{attachmentId}` — métadonnées/téléchargement des pièces jointes du contenu publié.

Les routes parent réutilisent `can_guardian_read_children` et réévaluent le lien parent/enfant et l’inscription à chaque accès. Un brouillon, contenu archivé, pièce jointe liée à un autre contenu ou objet hors classe est masqué par `404`. Upload invalide, taille dépassée, média interdit ou plage de pagination invalide : `400`; transition d’état incompatible : `409`.

## Licences plateforme

L’établissement est le titulaire du droit d’accès, conformément au modèle SaaS du README. Aucun prix, quota ou essai n’est fixé par cette tranche. Une offre contient un ensemble de capacités (`SCHOOL_MANAGEMENT`, `STUDENT_RECORDS`, `PARENT_PORTAL`, `TUITION`, `ATTENDANCE`, `LEARNING_CONTENT`). Une licence possède une période `[validFrom, validUntil)` (fin facultative) et peut être suspendue. L’état retourné est `UPCOMING`, `ACTIVE`, `EXPIRED` ou `SUSPENDED`. Les événements d’attribution, remplacement, suspension et réactivation sont audités.

La baseline V1 crée l’offre de référence `LEGACY_FULL_ACCESS`, sans établissement ni licence de test préchargés. Les nouveaux établissements ne reçoivent pas de licence implicite : un administrateur plateforme doit en attribuer une. Les changements de licence verrouillent la ligne établissement et refusent les chevauchements avec une licence future; une licence antérieure chevauchante est terminée au début de sa remplaçante.

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

Requiert `ADMIN_ACCESS` ou `ESTABLISHMENT_STAFF_MANAGEMENT`. Met à jour partiellement le matricule, le prénom, le nom, l’email ou le poste avec `positionId`. Le nouveau poste doit appartenir au même établissement. Au moins un champ doit être fourni. L’email est également utilisé comme login; sa modification synchronise le compte de sécurité et un changement de poste recalcule ses rôles effectifs. Le rôle `PARENT` et les liens tuteur/enfant existants sont conservés.

```json
{
	"positionId": "<uuid-poste>"
}
```

Réponse `200 OK` avec la fiche à jour. Une fiche hors périmètre renvoie `404`; un matricule ou email déjà utilisé renvoie `409`.

### `PUT /api/v1/employees/{employeeId}/roles`

Remplace les rôles directs du membre. Les valeurs acceptées pour les rôles directs restent `STAFF` et `ESTABLISHMENT_ADMIN`; les rôles effectifs incluent aussi les rôles personnalisés hérités du poste. Les rôles personnalisés se gèrent au niveau des postes, pas via cette route. Le rôle `PARENT`, ajouté par un rattachement comme tuteur, est conservé lors de la synchronisation des rôles du personnel ou du poste.

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

L’origine de ces liens se configure dans `src/main/resources/application.yaml`, sous `app.invitations.scheme` (`http` par défaut), `app.invitations.host` (`localhost`) et `app.invitations.port` (`3000`). Les variables `INVITATION_SCHEME`, `INVITATION_HOST` et `INVITATION_PORT` permettent de remplacer ces valeurs par environnement. L’hôte doit être renseigné sans protocole, port ni chemin ; le port est celui du client web, distinct de l’API et de SMTP. Par exemple, `https`, `ecole.example.org`, `443` donnent `https://ecole.example.org:443/accept-invitation#token=...` pour le personnel et le premier administrateur d’école, et `https://ecole.example.org:443/accept-guardian-invitation#token=...` pour les tuteurs. Une URL complète fournie par `EMPLOYEE_INVITATION_ACCEPT_URL` (prioritaire sur `STAFF_INVITATION_ACCEPT_URL`) ou `GUARDIAN_INVITATION_ACCEPT_URL` remplace entièrement cette construction.

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
