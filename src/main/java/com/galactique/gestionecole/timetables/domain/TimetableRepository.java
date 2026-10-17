package com.galactique.gestionecole.timetables.domain;

import java.util.List;
import java.util.UUID;

public interface TimetableRepository {
	List<TimetableEntry> findActiveByEstablishment(UUID establishmentId);
	TimetableEntry save(TimetableEntry entry);
	void archive(TimetableEntry entry, String actor);
	List<NonWorkingDay> findNonWorkingDays(UUID establishmentId, UUID academicYearId);
	void replaceNonWorkingDays(UUID establishmentId, UUID academicYearId, List<NonWorkingDay> days, String actor);
}
