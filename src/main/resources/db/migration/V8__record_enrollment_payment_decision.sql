ALTER TABLE enrollments ADD COLUMN advance_accepted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE enrollments ADD COLUMN payment_decision_by VARCHAR(255);
