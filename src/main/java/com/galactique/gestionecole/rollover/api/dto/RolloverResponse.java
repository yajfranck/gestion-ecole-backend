package com.galactique.gestionecole.rollover.api.dto;

import java.util.List;
import java.util.UUID;
import com.galactique.gestionecole.rollover.application.SchoolYearRolloverService.Result;

public record RolloverResponse(UUID sourceAcademicYearId, UUID targetAcademicYearId, List<LinkResponse> classes,
    List<LinkResponse> classrooms, int subjectAssignmentsCreated, List<UUID> skippedSubjectIds,
    List<LinkResponse> fees, String createdBy) {
    public record LinkResponse(UUID sourceId, UUID targetId, boolean created) { }
    public static RolloverResponse from(Result result) {
        return new RolloverResponse(result.sourceAcademicYearId(), result.targetAcademicYearId(),
            result.classes().stream().map(l -> new LinkResponse(l.sourceId(), l.targetId(), l.created())).toList(),
            result.classrooms().stream().map(l -> new LinkResponse(l.sourceId(), l.targetId(), l.created())).toList(),
            result.subjectAssignmentsCreated(), result.skippedSubjectIds(),
            result.fees().stream().map(l -> new LinkResponse(l.sourceId(), l.targetId(), l.created())).toList(), result.createdBy());
    }
}
