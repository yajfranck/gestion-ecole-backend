package com.galactique.gestionecole.timetables.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimetableEntryTests {
	private static final UUID SCHOOL = UUID.randomUUID();
	@Test
	void overlappingDateRangesRequireAnActualSharedOccurrence() {
		UUID room = UUID.randomUUID();
		var first = entry(room, "08:00", "09:00", "2026-09-07", "2026-09-08");
		var nextWeek = entry(room, "08:00", "09:00", "2026-09-08", "2026-09-14");
		assertFalse(first.conflictsWith(nextWeek));
		assertTrue(first.conflictsWith(entry(room, "08:30", "09:30", "2026-09-07", "2026-09-07")));
	}
	@Test
	void adjacentSlotsAndDistinctUnassignedGroupsDoNotConflict() {
		UUID room = UUID.randomUUID();
		var first = entry(room, "08:00", "09:00", "2026-09-01", "2027-06-30");
		assertFalse(first.conflictsWith(entry(room, "09:00", "10:00", "2026-09-01", "2027-06-30")));
		assertFalse(first.conflictsWith(entry(UUID.randomUUID(), "08:00", "09:00", "2026-09-01", "2027-06-30")));
	}
	private TimetableEntry entry(UUID room, String start, String end, String from, String until) {
		return new TimetableEntry(UUID.randomUUID(), SCHOOL, UUID.randomUUID(), UUID.randomUUID(), room,
			UUID.randomUUID(), null, DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end), LocalDate.parse(from),
			LocalDate.parse(until), null, true, null, "test", "test", Instant.now(), Instant.now());
	}
}
