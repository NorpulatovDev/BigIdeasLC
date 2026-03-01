-- =============================================================
-- Migration Script (PostgreSQL)
-- Run BEFORE deploying the new application version.
-- =============================================================

-- ---------------------------------------------------------------
-- CHANGE 1: payments table
-- Replace payment_year + payment_month + due_date
-- with payment_date + payment_due_date
-- ---------------------------------------------------------------

-- Step 1: Add new columns (nullable so existing rows are accepted)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_date DATE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_due_date DATE;

-- Step 2: Migrate data from old integer columns
UPDATE payments
SET payment_date    = TO_DATE(payment_year || '-' || LPAD(payment_month::TEXT, 2, '0') || '-01', 'YYYY-MM-DD'),
    payment_due_date = due_date
WHERE payment_date IS NULL;

-- Step 3: Enforce NOT NULL after data migration
ALTER TABLE payments ALTER COLUMN payment_date SET NOT NULL;

-- Step 4: Drop old columns (run after verifying the migration)
ALTER TABLE payments DROP COLUMN IF EXISTS payment_year;
ALTER TABLE payments DROP COLUMN IF EXISTS payment_month;
ALTER TABLE payments DROP COLUMN IF EXISTS due_date;

-- ---------------------------------------------------------------
-- CHANGE 2: enrollments table
-- Replace group_students join table + group.price + group.teacher_salary_per_student
-- with per-student enrollment records
-- ---------------------------------------------------------------

-- Step 1: Create enrollments table
CREATE TABLE IF NOT EXISTS enrollments (
    id                    BIGSERIAL PRIMARY KEY,
    student_id            BIGINT        NOT NULL REFERENCES students(id),
    group_id              BIGINT        NOT NULL REFERENCES groups(id),
    tuition_fee           DECIMAL(10,2) NOT NULL,
    teacher_salary_amount DECIMAL(10,2),
    enrolled_at           DATE          NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (student_id, group_id)
);

-- Step 2: Migrate data from group_students join table
--         Use group.price as default tuition_fee and group.teacher_salary_per_student
INSERT INTO enrollments (student_id, group_id, tuition_fee, teacher_salary_amount, enrolled_at)
SELECT gs.student_id,
       gs.group_id,
       COALESCE(g.price, 0),
       g.teacher_salary_per_student,
       CURRENT_DATE
FROM group_students gs
JOIN groups g ON gs.group_id = g.id
ON CONFLICT (student_id, group_id) DO NOTHING;

-- Step 3: Drop old columns and join table (run after verifying the migration)
ALTER TABLE groups DROP COLUMN IF EXISTS price;
ALTER TABLE groups DROP COLUMN IF EXISTS teacher_salary_per_student;
DROP TABLE IF EXISTS group_students;
