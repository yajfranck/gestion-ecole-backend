package com.galactique.gestionecole.rollover.api.dto;

import java.util.List;

import com.galactique.gestionecole.rollover.application.SchoolYearRolloverService.Preview;
import com.galactique.gestionecole.schoolclasses.api.dto.AcademicYearResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.ClassroomResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.SchoolClassResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.SubjectResponse;
import com.galactique.gestionecole.tuition.api.dto.*;

public record RolloverPreviewResponse(AcademicYearResponse targetYear, AcademicYearResponse sourceYear,
	List<RoomResponse> classrooms, boolean financeAvailable, List<TuitionFeeResponse> fees,
	List<TuitionFeeTypeResponse> feeTypes, List<TuitionPaymentMethodResponse> paymentMethods) {
	public record RoomResponse(ClassroomResponse classroom, SchoolClassResponse schoolClass, List<SubjectResponse> subjects) { }
	public static RolloverPreviewResponse from(Preview preview) {
		return new RolloverPreviewResponse(AcademicYearResponse.from(preview.targetYear()),
			preview.sourceYear() == null ? null : AcademicYearResponse.from(preview.sourceYear()),
			preview.classrooms().stream().map(room -> new RoomResponse(ClassroomResponse.from(room.source().classroom()),
				SchoolClassResponse.from(room.source().schoolClass()), room.subjects().stream().map(SubjectResponse::from).toList())).toList(),
			preview.financeAvailable(), preview.fees().stream().map(TuitionFeeResponse::from).toList(),
			preview.feeTypes().stream().map(TuitionFeeTypeResponse::from).toList(),
			preview.paymentMethods().stream().map(TuitionPaymentMethodResponse::from).toList());
	}
}
