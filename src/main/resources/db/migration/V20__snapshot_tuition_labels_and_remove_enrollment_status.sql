ALTER TABLE tuition_fee_definitions ADD COLUMN fee_type_code VARCHAR(40);
ALTER TABLE tuition_fee_definitions ADD COLUMN fee_type_name VARCHAR(100);

UPDATE tuition_fee_definitions fee
SET fee_type_code = (
        SELECT fee_type.code FROM tuition_fee_types fee_type WHERE fee_type.id = fee.fee_type_id
    ),
    fee_type_name = (
        SELECT fee_type.name FROM tuition_fee_types fee_type WHERE fee_type.id = fee.fee_type_id
    )
WHERE fee.fee_type_id IS NOT NULL;

ALTER TABLE tuition_payments ADD COLUMN payment_method_name VARCHAR(100);

UPDATE tuition_payments payment
SET payment_method_name = COALESCE(
    (SELECT method.name FROM school_payment_methods method
     WHERE method.establishment_id = payment.establishment_id
       AND method.code = payment.payment_method_code),
    payment.payment_method_code
);

ALTER TABLE tuition_payments ALTER COLUMN payment_method_name SET NOT NULL;

ALTER TABLE enrollments DROP CONSTRAINT ck_enrollments_status;
ALTER TABLE enrollments DROP COLUMN status;