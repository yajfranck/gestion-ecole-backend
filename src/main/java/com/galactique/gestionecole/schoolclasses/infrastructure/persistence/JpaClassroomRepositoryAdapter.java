package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.galactique.gestionecole.schoolclasses.domain.Classroom;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomStatistics;

@Repository
@Transactional
public class JpaClassroomRepositoryAdapter implements ClassroomRepository {
	private final ClassroomJpaRepository repository;
	public JpaClassroomRepositoryAdapter(ClassroomJpaRepository repository) { this.repository = repository; }

	@Override
	public Classroom save(UUID establishmentId, UUID academicYearId, UUID schoolClassId, String code, String name,
		int maxStudents) {
		return repository.saveAndFlush(new ClassroomEntity(establishmentId, academicYearId, schoolClassId,
			code.strip().toUpperCase(Locale.ROOT), name.strip(), maxStudents)).toDomain();
	}
	@Override @Transactional(readOnly = true)
	public List<Classroom> findAll(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		return repository.findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrueOrderByNameAscIdAsc(
			establishmentId, academicYearId, schoolClassId).stream().map(ClassroomEntity::toDomain).toList();
	}
	@Override @Transactional(readOnly = true)
	public Optional<Classroom> findById(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId) {
		return repository.findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrue(
			classroomId, establishmentId, academicYearId, schoolClassId).map(ClassroomEntity::toDomain);
	}
	@Override
	public Classroom update(Classroom current, String code, String name, int maxStudents) {
		var entity = findEntity(current);
		entity.update(code.strip().toUpperCase(Locale.ROOT), name.strip(), maxStudents);
		return repository.saveAndFlush(entity).toDomain();
	}
	@Override
	public void archive(Classroom current) { findEntity(current).archive(); }
	@Override @Transactional(readOnly = true)
	public boolean hasEnrollments(UUID classroomId) { return repository.countEnrollments(classroomId) > 0; }
	@Override @Transactional(readOnly = true)
	public long countEnrollments(UUID classroomId) { return repository.countEnrollments(classroomId); }
	@Override @Transactional(readOnly = true)
	public Map<UUID, ClassroomStatistics> findStatisticsByEstablishmentIdAndAcademicYearId(
		UUID establishmentId, UUID academicYearId) {
		return repository.findStatisticsByEstablishmentIdAndAcademicYearId(establishmentId, academicYearId).stream()
			.collect(Collectors.toUnmodifiableMap(statistics -> toUuid(statistics.getSchoolClassId()),
				statistics -> new ClassroomStatistics(statistics.getClassroomCount(), statistics.getTotalCapacity(),
					statistics.getEnrolledStudentCount())));
	}
	@Override @Transactional(readOnly = true)
	public boolean hasTimetableEntries(UUID classroomId) { return repository.countTimetableEntries(classroomId) > 0; }
	@Override @Transactional(readOnly = true)
	public Optional<Classroom> findInYear(UUID establishmentId, UUID academicYearId, UUID classroomId) {
		return repository.findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(classroomId, establishmentId, academicYearId)
			.map(ClassroomEntity::toDomain);
	}

	private ClassroomEntity findEntity(Classroom current) {
		return repository.findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrue(
			current.id(), current.establishmentId(), current.academicYearId(), current.schoolClassId()).orElseThrow();
	}

	private UUID toUuid(Object value) {
		if (value instanceof UUID uuid) {
			return uuid;
		}
		if (value instanceof byte[] bytes && bytes.length == 16) {
			var buffer = ByteBuffer.wrap(bytes);
			return new UUID(buffer.getLong(), buffer.getLong());
		}
		return UUID.fromString(value.toString());
	}
}
