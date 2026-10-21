package com.galactique.gestionecole.schoolclasses.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.ClassroomRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;

@Service
@Transactional(readOnly = true)
public class SchoolClassOperationalityService {
	private final SchoolClassRepository schoolClassRepository;
	private final ClassroomRepository classroomRepository;

	public SchoolClassOperationalityService(SchoolClassRepository schoolClassRepository,
		ClassroomRepository classroomRepository) {
		this.schoolClassRepository = schoolClassRepository;
		this.classroomRepository = classroomRepository;
	}

	public void requireActiveClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		schoolClassRepository.findByIdAndEstablishmentIdAndAcademicYearId(schoolClassId, establishmentId, academicYearId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		if (classroomRepository.findAll(establishmentId, academicYearId, schoolClassId).isEmpty()) {
			throw new SchoolClassRequiresClassroomException();
		}
	}
}