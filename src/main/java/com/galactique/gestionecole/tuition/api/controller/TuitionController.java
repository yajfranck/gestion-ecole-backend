package com.galactique.gestionecole.tuition.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.tuition.api.dto.ApplyTuitionFeeResponse;
import com.galactique.gestionecole.tuition.api.dto.CreateTuitionFeeRequest;
import com.galactique.gestionecole.tuition.api.dto.CreateTuitionFeeTypeRequest;
import com.galactique.gestionecole.tuition.api.dto.FeeCreationResponse;
import com.galactique.gestionecole.tuition.api.dto.PaymentReceiptResponse;
import com.galactique.gestionecole.tuition.api.dto.RecordTuitionPaymentRequest;
import com.galactique.gestionecole.tuition.api.dto.RenamePaymentMethodRequest;
import com.galactique.gestionecole.tuition.api.dto.RenameTuitionFeeTypeRequest;
import com.galactique.gestionecole.tuition.api.dto.ReplacePaymentMethodsRequest;
import com.galactique.gestionecole.tuition.api.dto.StudentFeeChargeResponse;
import com.galactique.gestionecole.tuition.api.dto.TuitionFeeResponse;
import com.galactique.gestionecole.tuition.api.dto.TuitionFeeTypeResponse;
import com.galactique.gestionecole.tuition.api.dto.TuitionPaymentMethodResponse;
import com.galactique.gestionecole.tuition.api.dto.TuitionPaymentResponse;
import com.galactique.gestionecole.tuition.application.TuitionApplicationService;
import com.galactique.gestionecole.tuition.application.CreateTuitionFeeCommand;
import jakarta.validation.Valid;

@RestController
public class TuitionController {
	private final TuitionApplicationService tuitionApplicationService;

	public TuitionController(TuitionApplicationService tuitionApplicationService) {
		this.tuitionApplicationService = tuitionApplicationService;
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees")
	@PreAuthorize("hasAuthority('can_tuition_read')")
	public List<TuitionFeeResponse> getFees(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return tuitionApplicationService.findFees(establishmentId, academicYearId, toActor(authentication))
			.stream().map(TuitionFeeResponse::from).toList();
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/tuition-fee-types")
	@PreAuthorize("hasAuthority('can_tuition_fee_type_read')")
	public List<TuitionFeeTypeResponse> getFeeTypes(@PathVariable UUID establishmentId, Authentication authentication) {
		return tuitionApplicationService.findFeeTypes(establishmentId, toActor(authentication)).stream()
			.map(TuitionFeeTypeResponse::from).toList();
	}

	@PostMapping("/api/v1/establishments/{establishmentId}/tuition-fee-types")
	@PreAuthorize("hasAuthority('can_tuition_fee_type_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public TuitionFeeTypeResponse createFeeType(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreateTuitionFeeTypeRequest request, Authentication authentication) {
		return TuitionFeeTypeResponse.from(tuitionApplicationService.createFeeType(
			establishmentId, request.code(), request.name(), toActor(authentication)));
	}

	@PatchMapping("/api/v1/establishments/{establishmentId}/tuition-fee-types/{feeTypeId}")
	@PreAuthorize("hasAuthority('can_tuition_fee_type_update')")
	public TuitionFeeTypeResponse renameFeeType(@PathVariable UUID establishmentId, @PathVariable UUID feeTypeId,
		@Valid @RequestBody RenameTuitionFeeTypeRequest request, Authentication authentication) {
		return TuitionFeeTypeResponse.from(tuitionApplicationService.renameFeeType(
			establishmentId, feeTypeId, request.name(), toActor(authentication)));
	}

	@DeleteMapping("/api/v1/establishments/{establishmentId}/tuition-fee-types/{feeTypeId}")
	@PreAuthorize("hasAuthority('can_tuition_fee_type_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteFeeType(@PathVariable UUID establishmentId, @PathVariable UUID feeTypeId,
		Authentication authentication) {
		tuitionApplicationService.deleteFeeType(establishmentId, feeTypeId, toActor(authentication));
	}

	@PostMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees")
	@PreAuthorize("hasAuthority('can_tuition_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public FeeCreationResponse createFee(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@Valid @RequestBody CreateTuitionFeeRequest request, Authentication authentication) {
		return FeeCreationResponse.from(tuitionApplicationService.createFee(establishmentId, academicYearId,
			new CreateTuitionFeeCommand(request.name(), request.amount(), request.currency(), request.dueDate(),
				request.feeTypeId(), request.schoolClassId(), request.studentId()), toActor(authentication)));
	}

	@PostMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/{feeId}/apply")
	@PreAuthorize("hasAuthority('can_tuition_apply')")
	public ApplyTuitionFeeResponse applyFee(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID feeId, Authentication authentication) {
		return new ApplyTuitionFeeResponse(tuitionApplicationService.applyFee(
			establishmentId, academicYearId, feeId, toActor(authentication)));
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/tuition-fees/outstanding")
	@PreAuthorize("hasAuthority('can_tuition_outstanding_read')")
	public List<StudentFeeChargeResponse> getOutstanding(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return tuitionApplicationService.findOutstanding(establishmentId, academicYearId, toActor(authentication))
			.stream().map(StudentFeeChargeResponse::from).toList();
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/students/{studentId}/tuition-fees")
	@PreAuthorize("hasAuthority('can_tuition_read')")
	public List<StudentFeeChargeResponse> getStudentCharges(@PathVariable UUID establishmentId,
		@PathVariable UUID studentId, Authentication authentication) {
		return tuitionApplicationService.findStudentCharges(establishmentId, studentId, toActor(authentication))
			.stream().map(StudentFeeChargeResponse::from).toList();
	}

	@PostMapping("/api/v1/establishments/{establishmentId}/students/{studentId}/tuition-fees/{chargeId}/payments")
	@PreAuthorize("hasAuthority('can_payment_record')")
	@ResponseStatus(HttpStatus.CREATED)
	public TuitionPaymentResponse recordPayment(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID chargeId, @Valid @RequestBody RecordTuitionPaymentRequest request,
		Authentication authentication) {
		return TuitionPaymentResponse.from(tuitionApplicationService.recordPayment(establishmentId,
			studentId, chargeId, request.amount(), request.paymentMethodCode(), request.reference(), toActor(authentication)).payment());
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/payments/{paymentId}/receipt")
	@PreAuthorize("hasAuthority('can_payment_receipt_read')")
	public PaymentReceiptResponse getReceipt(@PathVariable UUID establishmentId, @PathVariable UUID paymentId,
		Authentication authentication) {
		return PaymentReceiptResponse.from(tuitionApplicationService.findReceipt(
			establishmentId, paymentId, toActor(authentication)));
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/payment-methods")
	@PreAuthorize("hasAuthority('can_tuition_read')")
	public List<TuitionPaymentMethodResponse> getPaymentMethods(@PathVariable UUID establishmentId,
		Authentication authentication) {
		return tuitionApplicationService.findPaymentMethods(establishmentId, toActor(authentication))
			.stream().map(TuitionPaymentMethodResponse::from).toList();
	}

	@PutMapping("/api/v1/establishments/{establishmentId}/payment-methods")
	@PreAuthorize("hasAuthority('can_payment_method_update')")
	public List<TuitionPaymentMethodResponse> replacePaymentMethods(@PathVariable UUID establishmentId,
		@Valid @RequestBody ReplacePaymentMethodsRequest request, Authentication authentication) {
		List<TuitionApplicationService.PaymentMethodInput> methods = request.methods().stream()
			.map(method -> new TuitionApplicationService.PaymentMethodInput(method.code(), method.name())).toList();
		return tuitionApplicationService.replacePaymentMethods(establishmentId, methods, toActor(authentication))
			.stream().map(TuitionPaymentMethodResponse::from).toList();
	}

	@PatchMapping("/api/v1/establishments/{establishmentId}/payment-methods/{code}")
	@PreAuthorize("hasAuthority('can_payment_method_update')")
	public TuitionPaymentMethodResponse renamePaymentMethod(@PathVariable UUID establishmentId,
		@PathVariable String code, @Valid @RequestBody RenamePaymentMethodRequest request,
		Authentication authentication) {
		return TuitionPaymentMethodResponse.from(tuitionApplicationService.renamePaymentMethod(
			establishmentId, code, request.name(), toActor(authentication)));
	}

	@DeleteMapping("/api/v1/establishments/{establishmentId}/payment-methods/{code}")
	@PreAuthorize("hasAuthority('can_payment_method_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePaymentMethod(@PathVariable UUID establishmentId, @PathVariable String code,
		Authentication authentication) {
		tuitionApplicationService.deletePaymentMethod(establishmentId, code, toActor(authentication));
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName(), hasAuthority(authentication, "ADMIN_ACCESS"),
			hasAuthority(authentication, "ESTABLISHMENT_STAFF_MANAGEMENT"));
	}

	private boolean hasAuthority(Authentication authentication, String expectedAuthority) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority != null && expectedAuthority.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}