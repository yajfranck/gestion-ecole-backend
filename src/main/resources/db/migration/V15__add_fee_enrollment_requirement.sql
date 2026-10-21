ALTER TABLE tuition_fee_definitions
    ADD COLUMN required_for_enrollment BOOLEAN NOT NULL DEFAULT TRUE;