# Migration des autorisations — V18

Documentation consultée : `README.md`, `.github/copilot-instructions.md`, `doc-api.md`, `swagger.yaml`. Audit effectué avant modification, plan présenté dans la conversation avant la refactorisation.

## Plan appliqué

1. Recenser les contrôleurs et résoudre les doublons des déplacements de packages déjà présents dans le dépôt.
2. Déclarer les autorisations fonctionnelles sur chaque méthode REST avec `@PreAuthorize` et des permissions `can_*` ; conserver les conditions de paiement, remplacement et attribution d’un administrateur dans ces annotations.
3. Résoudre les autorités JWT actuelles depuis la base et les limiter au rattachement demandé. Conserver l’authentification JWT, l’état des comptes, les liens parent/enfant, les licences et les validations métier.
4. Séparer le périmètre plateforme dans `security_role_scopes` des permissions fonctionnelles. Une habilitation PLATFORM ne donne aucune permission et aucun rôle ne déclenche de bypass. Une permission métier ne donne aucun périmètre supplémentaire.
5. Imposer les contextes des routes annuelles et du personnel, convertir les attributions historiques par Flyway, tester puis mettre à jour les contrats.

## Déploiement et conservation des droits

Appliquer V18 après les migrations existantes. Les migrations V1 à V17 ne sont pas réécrites. La conversion des anciennes autorités conserve les autres associations et n’est exécutée qu’une fois par Flyway. Les permissions existantes `can_academic_year_*`, notamment, sont conservées plutôt que renommées en doublons `can_school_year_*`.

V18 crée 20 permissions : les sept `can_employee_*`, `can_user_read`, `can_user_change_password`, les trois `can_platform_role_*`, `can_establishment_delete`, les deux `can_license_offer_*` et les cinq `can_license_assign/suspend/reactivate/end/event_read`. Le rôle ADMIN reçoit explicitement les opérations plateforme correspondant à ses anciens accès et les opérations d’administration de sécurité. Les rôles qui possédaient les anciennes autorités reçoivent leurs remplacements ; `ESTABLISHMENT_ADMIN` reçoit les opérations du personnel hors promotion d’un administrateur. Le rôle PARENT reçoit les lectures dédiées des établissements et des années, toujours bornées par les liens enfants. Le catalogue local et le catalogue global retournent toutes les permissions ; la délégation locale de permissions plateforme reste interdite. Un rôle portant une habilitation PLATFORM ne peut pas être délégué par un poste d’établissement, même s’il ne contient qu’une permission métier.

Les utilisateurs ADMIN ou les anciens rôles globaux dotés de l’autorité administrative générale reçoivent une habilitation PLATFORM explicite. Une attribution à un autre rôle global (y compris un super administrateur) doit être explicite : permissions via les routes globales de rôles et périmètre via `security_role_scopes`. Le nom du rôle n’est jamais testé pour autoriser une opération. L’initialisation et les connexions suivantes ne réattribuent aucun droit retiré. Le script de provisionnement production conserve un compte existant et termine avec succès sans le modifier.

Les autorités sont résolues à chaque requête JWT : une révocation est donc prise en compte avec un jeton déjà émis. Aucun renouvellement n’est nécessaire pour les décisions du serveur. Les clients qui affichent les droits du claim peuvent renouveler leur JWT pour actualiser leur affichage.

Les chemins restent en `/api/v1` dans ce backend en cours de développement. Il s’agit d’une rupture de contrat : adapter les clients aux nouvelles routes du personnel, des finances annuelles, du portail parent annuel et du dashboard établissement. Les anciennes routes sans contexte sont retirées. Aucun frontend n’est modifié.

## Contextes

- Globaux : authentification, catalogue de permissions, comptes et rôles plateforme, offres de licence, dashboard plateforme.
- Établissement uniquement : dossiers élèves, liens tuteur/élève, personnel et postes, matières, types de frais, modes de paiement, licences et catalogue d’années.
- Établissement et année obligatoires : périodes, classes, salles, inscriptions, présences, emplois du temps, contenus, frais, créances, paiements, reçus, dashboard établissement et vues parent annuelles.
- `academicYearId` est l’identifiant de l’année scolaire (`schoolYearId` dans la demande), conservé pour éviter un renommage sans effet fonctionnel. Les données ne prennent jamais une année implicite.
- Connexion, renouvellement et acceptations d’invitation restent publics car ils valident leurs propres secrets. La santé et la console H2 locale ne sont pas des endpoints métier.

## Correspondance exhaustive

Les expressions ci-dessous décrivent les permissions ; les validations de périmètre et de licence restent dans les services. « PUBLIC » désigne uniquement les quatre opérations d’authentification ou d’acceptation d’invitation.

| Méthode | Route cible | Autorisation avant audit | Autorisation cible | Ancienne route si modifiée |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/attendance` | `hasAuthority('can_attendance_read')` | `hasAuthority('can_attendance_read')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/attendance/{date}` | `hasAuthority('can_attendance_write')` | `hasAuthority('can_attendance_write')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/students/{studentId}/attendance` | `hasAuthority('can_attendance_read')` | `hasAuthority('can_attendance_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/employees` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')` | `hasAuthority('can_employee_create') and (#request.directRoleCodes() == null or !#request.directRoleCodes().contains('ESTABLISHMENT_ADMIN') or hasAuthority('can_employee_assign_administrator'))` | `/api/v1/employees` |
| PATCH | `/api/v1/establishments/{establishmentId}/employees/{employeeId}` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')` | `hasAuthority('can_employee_update')` | `/api/v1/employees/{employeeId}` |
| PUT | `/api/v1/establishments/{establishmentId}/employees/{employeeId}/roles` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')` | `hasAuthority('can_employee_assign_roles') and (!#request.roleCodes().contains('ESTABLISHMENT_ADMIN') or hasAuthority('can_employee_assign_administrator'))` | `/api/v1/employees/{employeeId}/roles` |
| DELETE | `/api/v1/establishments/{establishmentId}/employees/{employeeId}` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')` | `hasAuthority('can_employee_delete')` | `/api/v1/employees/{employeeId}` |
| POST | `/api/v1/employees/invitations/accept` | `PUBLIC` | `PUBLIC` | — |
| GET | `/api/v1/establishments/{establishmentId}/employees` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')` | `hasAuthority('can_employee_read')` | `/api/v1/employees` |
| GET | `/api/v1/establishments/{establishmentId}/employees/me` | `hasAuthority('STAFF_ACCESS')` | `hasAuthority('can_employee_read_self')` | `/api/v1/employees/me` |
| GET | `/api/v1/establishments/{establishmentId}/employees/{employeeId}` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')` | `hasAuthority('can_employee_read')` | `/api/v1/employees/{employeeId}` |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/students` | `hasAuthority('can_enrollment_read')` | `hasAuthority('can_enrollment_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments` | `hasAuthority('can_enrollment_read')` | `hasAuthority('can_enrollment_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments` | `hasAuthority('can_enrollment_create') and (#request.payments() == null or #request.payments().isEmpty() or hasAuthority('can_payment_record'))` | `hasAuthority('can_enrollment_create') and (#request.payments() == null or #request.payments().isEmpty() or hasAuthority('can_payment_record'))` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments/{enrollmentId}` | `hasAuthority('can_enrollment_update')` | `hasAuthority('can_enrollment_update')` | — |
| GET | `/api/v1/establishments` | `isAuthenticated()` | `hasAuthority('can_establishment_read')` | — |
| POST | `/api/v1/establishments` | `hasRole('ADMIN') and hasAuthority('can_establishment_onboard')` | `hasAuthority('can_establishment_onboard')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}` | `hasRole('ADMIN') or hasAuthority('can_establishment_update')` | `hasAuthority('can_establishment_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}` | `hasAuthority('ADMIN_ACCESS')` | `hasAuthority('can_establishment_delete')` | — |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/summary` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/summary` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/tuition-fees` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/tuition-fees` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/tuition-fees/{chargeId}/payments` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/tuition-fees/{chargeId}/payments` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/attendance` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/attendance` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content/{contentId}` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content/{contentId}/attachments` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content/{contentId}/attachments` |
| GET | `/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content/{contentId}/attachments/{attachmentId}` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | `/api/v1/guardians/me/children/{studentId}/school-years/{academicYearId}/learning-content/{contentId}/attachments/{attachmentId}` |
| GET | `/api/v1/establishments/{establishmentId}/students/{studentId}/guardians` | `hasAuthority('can_guardian_read')` | `hasAuthority('can_guardian_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/students/{studentId}/guardians` | `hasAuthority('can_guardian_link')` | `hasAuthority('can_guardian_link')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/students/{studentId}/guardians/{guardianId}` | `hasAuthority('can_guardian_unlink')` | `hasAuthority('can_guardian_unlink')` | — |
| POST | `/api/v1/guardians/invitations/accept` | `PUBLIC` | `PUBLIC` | — |
| GET | `/api/v1/guardians/me/children` | `hasAuthority('can_guardian_read_children')` | `hasAuthority('can_guardian_read_children')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content` | `hasAuthority('can_learning_content_read')` | `hasAuthority('can_learning_content_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content` | `hasAuthority('can_learning_content_create')` | `hasAuthority('can_learning_content_create')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}` | `hasAuthority('can_learning_content_read')` | `hasAuthority('can_learning_content_read')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}` | `hasAuthority('can_learning_content_update')` | `hasAuthority('can_learning_content_update')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}/publish` | `hasAuthority('can_learning_content_publish')` | `hasAuthority('can_learning_content_publish')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}/archive` | `hasAuthority('can_learning_content_archive')` | `hasAuthority('can_learning_content_archive')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}/attachments` | `hasAuthority('can_learning_content_update')` | `hasAuthority('can_learning_content_update')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}/attachments` | `hasAuthority('can_learning_content_read')` | `hasAuthority('can_learning_content_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content/{contentId}/attachments/{attachmentId}` | `hasAuthority('can_learning_content_read')` | `hasAuthority('can_learning_content_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/license` | `hasAuthority('can_license_read') or hasRole('ADMIN')` | `hasAuthority('can_license_read')` | — |
| GET | `/api/v1/admin/license-offers` | `hasRole('ADMIN')` | `hasAuthority('can_license_offer_read')` | — |
| POST | `/api/v1/admin/license-offers` | `hasRole('ADMIN')` | `hasAuthority('can_license_offer_create')` | — |
| POST | `/api/v1/admin/establishments/{establishmentId}/licenses` | `hasRole('ADMIN')` | `hasAuthority('can_license_assign')` | — |
| POST | `/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/suspend` | `hasRole('ADMIN')` | `hasAuthority('can_license_suspend')` | — |
| POST | `/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/reactivate` | `hasRole('ADMIN')` | `hasAuthority('can_license_reactivate')` | — |
| POST | `/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/end` | `hasRole('ADMIN')` | `hasAuthority('can_license_end')` | — |
| GET | `/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/events` | `hasRole('ADMIN')` | `hasAuthority('can_license_event_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/positions` | `hasAuthority('can_position_read')` | `hasAuthority('can_position_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/positions` | `hasAuthority('can_position_create')` | `hasAuthority('can_position_create')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/positions/{positionId}` | `hasAuthority('can_position_update')` | `hasAuthority('can_position_update')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/positions/{positionId}/roles` | `hasAuthority('can_position_assign_roles')` | `hasAuthority('can_position_assign_roles')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/positions/{positionId}` | `hasAuthority('can_position_delete')` | `hasAuthority('can_position_delete')` | — |
| GET | `/api/v1/dashboard` | `hasAuthority('can_dashboard_read')` | `hasAuthority('can_dashboard_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/dashboard` | `Nouvelle route explicite` | `hasAuthority('can_dashboard_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classrooms/rollover` | `hasAuthority('can_school_year_rollover')` | `hasAuthority('can_school_year_rollover')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classrooms/rollover` | `hasAuthority('can_school_year_rollover')` | `hasAuthority('can_school_year_rollover')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms` | `hasAuthority('can_school_class_read')` | `hasAuthority('can_school_class_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}` | `hasAuthority('can_school_class_read')` | `hasAuthority('can_school_class_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms` | `hasAuthority('can_school_class_create')` | `hasAuthority('can_school_class_create')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}` | `hasAuthority('can_school_class_update')` | `hasAuthority('can_school_class_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}` | `hasAuthority('can_school_class_delete')` | `hasAuthority('can_school_class_delete')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years` | `@establishmentScope.visible(authentication, #establishmentId)` | `hasAuthority('can_academic_year_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_year_create', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_year_create')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_year_read', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_year_read')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_year_update', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_year_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_year_delete', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_year_delete')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_period_read', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_period_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_period_create', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_period_create')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods/{periodId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_period_update', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_period_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/periods/{periodId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_academic_period_delete', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_academic_period_delete')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes` | `hasAuthority('can_school_class_read')` | `hasAuthority('can_school_class_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` | `hasAuthority('can_school_class_read')` | `hasAuthority('can_school_class_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes` | `hasAuthority('can_school_class_create')` | `hasAuthority('can_school_class_create')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` | `hasAuthority('can_school_class_update')` | `hasAuthority('can_school_class_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}` | `hasAuthority('can_school_class_delete')` | `hasAuthority('can_school_class_delete')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects` | `hasAuthority('can_class_subject_read')` | `hasAuthority('can_class_subject_read')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects/{subjectId}` | `hasAuthority('can_class_subject_assign')` | `hasAuthority('can_class_subject_assign')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/subjects/{subjectId}` | `hasAuthority('can_class_subject_unassign')` | `hasAuthority('can_class_subject_unassign')` | — |
| GET | `/api/v1/establishments/{establishmentId}/subjects` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_subject_read', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_subject_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/subjects/{subjectId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_subject_read', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_subject_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/subjects` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_subject_create', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_subject_create')` | — |
| POST | `/api/v1/establishments/{establishmentId}/subjects/batch` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_subject_create', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_subject_create')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/subjects/{subjectId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_subject_update', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_subject_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/subjects/{subjectId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_subject_delete', 'SCHOOL_MANAGEMENT')` | `hasAuthority('can_subject_delete')` | — |
| POST | `/api/v1/login` | `PUBLIC` | `PUBLIC` | — |
| POST | `/api/v1/refresh` | `PUBLIC` | `PUBLIC` | — |
| GET | `/api/v1/establishments/{establishmentId}/roles` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('can_role_read')` | `hasAuthority('can_role_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/roles` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('can_role_create')` | `hasAuthority('can_role_create')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/roles/{code}` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('can_role_update')` | `hasAuthority('can_role_update')` | — |
| GET | `/api/v1/establishments/{establishmentId}/permissions` | `hasAuthority('ADMIN_ACCESS') or hasAuthority('can_permission_read')` | `hasAuthority('can_permission_read')` | — |
| PUT | `/api/v1/user/me/password` | `isAuthenticated()` | `hasAuthority('can_user_change_password')` | — |
| POST | `/api/v1/role` | `hasAuthority('ADMIN_ACCESS')` | `hasAuthority('can_platform_role_create')` | — |
| PUT | `/api/v1/role/{code}` | `hasAuthority('ADMIN_ACCESS')` | `hasAuthority('can_platform_role_update')` | — |
| GET | `/api/v1/user` | `hasAuthority('ADMIN_ACCESS')` | `hasAuthority('can_user_read')` | — |
| GET | `/api/v1/role` | `hasAuthority('ADMIN_ACCESS')` | `hasAuthority('can_platform_role_read')` | — |
| GET | `/api/v1/permission` | `hasAuthority('ADMIN_ACCESS')` | `hasAuthority('can_permission_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/students` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_student_read', 'STUDENT_RECORDS')` | `hasAuthority('can_student_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/students` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_student_create', 'STUDENT_RECORDS')` | `hasAuthority('can_student_create')` | — |
| GET | `/api/v1/establishments/{establishmentId}/students/{studentId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_student_read', 'STUDENT_RECORDS')` | `hasAuthority('can_student_read')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/students/{studentId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_student_update', 'STUDENT_RECORDS')` | `hasAuthority('can_student_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/students/{studentId}` | `@establishmentScope.allowed(authentication, #establishmentId, 'can_student_delete', 'STUDENT_RECORDS')` | `hasAuthority('can_student_delete')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable` | `hasAuthority('can_timetable_read')` | `hasAuthority('can_timetable_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable/{entryId}` | `hasAuthority('can_timetable_read')` | `hasAuthority('can_timetable_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable` | `hasAuthority('can_timetable_create')` | `hasAuthority('can_timetable_create')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable/{entryId}` | `hasAuthority('can_timetable_update')` | `hasAuthority('can_timetable_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable/{entryId}` | `hasAuthority('can_timetable_delete')` | `hasAuthority('can_timetable_delete')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable/copy` | `hasAuthority('can_timetable_copy') and (#request.replaceExisting() == null or !#request.replaceExisting() or hasAuthority('can_timetable_delete'))` | `hasAuthority('can_timetable_copy') and (#request.replaceExisting() == null or !#request.replaceExisting() or hasAuthority('can_timetable_delete'))` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable/day` | `hasAuthority('can_timetable_read')` | `hasAuthority('can_timetable_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms/{classroomId}/timetable/week` | `hasAuthority('can_timetable_read')` | `hasAuthority('can_timetable_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/timetable/non-working-days` | `hasAuthority('can_timetable_read')` | `hasAuthority('can_timetable_read')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/timetable/non-working-days` | `hasAuthority('can_timetable_update')` | `hasAuthority('can_timetable_update')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees` | `hasAuthority('can_tuition_read')` | `hasAuthority('can_tuition_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/tuition-fee-types` | `hasAuthority('can_tuition_fee_type_read')` | `hasAuthority('can_tuition_fee_type_read')` | — |
| POST | `/api/v1/establishments/{establishmentId}/tuition-fee-types` | `hasAuthority('can_tuition_fee_type_create')` | `hasAuthority('can_tuition_fee_type_create')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/tuition-fee-types/{feeTypeId}` | `hasAuthority('can_tuition_fee_type_update')` | `hasAuthority('can_tuition_fee_type_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/tuition-fee-types/{feeTypeId}` | `hasAuthority('can_tuition_fee_type_delete')` | `hasAuthority('can_tuition_fee_type_delete')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees` | `hasAuthority('can_tuition_create')` | `hasAuthority('can_tuition_create')` | — |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/{feeId}/apply` | `hasAuthority('can_tuition_apply')` | `hasAuthority('can_tuition_apply')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/outstanding` | `hasAuthority('can_tuition_outstanding_read')` | `hasAuthority('can_tuition_outstanding_read')` | — |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/students/{studentId}/tuition-fees` | `hasAuthority('can_tuition_read')` | `hasAuthority('can_tuition_read')` | `/api/v1/establishments/{establishmentId}/students/{studentId}/tuition-fees` |
| POST | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/students/{studentId}/tuition-fees/{chargeId}/payments` | `hasAuthority('can_payment_record')` | `hasAuthority('can_payment_record')` | `/api/v1/establishments/{establishmentId}/students/{studentId}/tuition-fees/{chargeId}/payments` |
| GET | `/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/payments/{paymentId}/receipt` | `hasAuthority('can_payment_receipt_read')` | `hasAuthority('can_payment_receipt_read')` | `/api/v1/establishments/{establishmentId}/payments/{paymentId}/receipt` |
| GET | `/api/v1/establishments/{establishmentId}/payment-methods` | `hasAuthority('can_tuition_read')` | `hasAuthority('can_tuition_read')` | — |
| PUT | `/api/v1/establishments/{establishmentId}/payment-methods` | `hasAuthority('can_payment_method_update')` | `hasAuthority('can_payment_method_update')` | — |
| PATCH | `/api/v1/establishments/{establishmentId}/payment-methods/{code}` | `hasAuthority('can_payment_method_update')` | `hasAuthority('can_payment_method_update')` | — |
| DELETE | `/api/v1/establishments/{establishmentId}/payment-methods/{code}` | `hasAuthority('can_payment_method_delete')` | `hasAuthority('can_payment_method_delete')` | — |

## Contrôles conservés et supprimés

Supprimés : accès fonctionnels par rôle administrateur, autorités générales historiques, permissions vérifiées dans les services, annotations au niveau des classes et exemption de licence ADMIN. Conservés : validation des entrées, état des comptes et rôles lors de la résolution des autorités, appartenance des années/classes/salles/élèves/créances, liens parents/enfants, licences, transactions et contraintes SQL.

## Validation finale

- Java 21.0.8 arm64 explicitement sélectionné ; compilation complète et suite Maven `clean test` avec le profil `local`.
- 165 tests, 43 suites : aucun échec, aucune erreur, aucun test ignoré. Mockito a été chargé comme agent au démarrage de la JVM de test, afin d’éviter la restriction d’attachement dynamique de cet environnement.
- Autorisations : permission présente/absente, administrateur sans permission, super administrateur avec permission et périmètre explicites, catalogue complet, absence d’endpoint sensible sans annotation de méthode.
- Isolation : permissions distinctes entre rattachements, rattachement désactivé, année étrangère, paiements/reçus d’une autre année sans écriture, lien parent/enfant, personnel également tuteur, interdiction de déléguer un rôle portant un périmètre plateforme via un poste local.
- Révocations : même JWT avant/après retrait de permission, nouvelle connexion et rotation du refresh token sans réattribution du droit ; conversion Flyway historique et deuxième exécution sans migration ni restauration du droit révoqué.
- Non-régression : règles financières, invitations, droits du personnel, capacités et affectations des salles, présences, cours, emplois du temps, reprise annuelle et licences.
- Swagger : 128 opérations applicatives et la santé Actuator, paramètres de chemin obligatoires, références de schémas résolues et identifiants d’opération uniques. `git diff --check` réussi.

Les migrations sont exécutées dans les tests sur H2 en mode PostgreSQL. Aucun serveur PostgreSQL réel ni compte de production n’a été modifié. Les modifications déjà présentes dans le dépôt ont été intégrées ; leurs incohérences de compilation et les doublons de contrôleurs ont été corrigés sans réinitialiser le travail existant.
