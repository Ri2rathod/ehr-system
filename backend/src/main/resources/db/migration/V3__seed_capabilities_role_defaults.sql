-- =============================================================
-- V3__seed_capabilities_role_defaults.sql
-- Seed all system capabilities (permissions) and assign them
-- to the four default roles: ADMIN, DOCTOR, RECEPTIONIST, PATIENT
-- =============================================================

-- =============================================================
-- SECTION 1 — Seed permissions (capabilities)
-- Format: (code, name, module, action, description)
-- =============================================================

INSERT INTO permissions (code, name, module, action, description) VALUES

    -- ── USER MODULE ───────────────────────────────────────────
    ('USER_READ',           'View Users',              'USER', 'READ',   'List and view user profiles'),
    ('USER_CREATE',         'Create Users',            'USER', 'CREATE', 'Register a new user account'),
    ('USER_UPDATE',         'Edit Users',              'USER', 'UPDATE', 'Modify an existing user profile'),
    ('USER_DELETE',         'Delete Users',            'USER', 'DELETE', 'Soft-delete a user account'),
    ('USER_MANAGE_ROLES',   'Assign Roles to Users',   'USER', 'MANAGE', 'Assign or revoke roles for a user'),
    ('USER_VIEW_OWN',       'View Own Profile',        'USER', 'READ',   'A user can view their own profile'),
    ('USER_UPDATE_OWN',     'Edit Own Profile',        'USER', 'UPDATE', 'A user can edit their own profile'),

    -- ── ROLE MODULE ───────────────────────────────────────────
    ('ROLE_READ',           'View Roles',              'ROLE', 'READ',   'List and view roles'),
    ('ROLE_CREATE',         'Create Roles',            'ROLE', 'CREATE', 'Define a new role'),
    ('ROLE_UPDATE',         'Edit Roles',              'ROLE', 'UPDATE', 'Modify an existing role'),
    ('ROLE_DELETE',         'Delete Roles',            'ROLE', 'DELETE', 'Remove a role from the system'),

    -- ── PERMISSION MODULE ─────────────────────────────────────
    ('PERMISSION_READ',     'View Permissions',        'PERMISSION', 'READ',   'List and view all permissions'),
    ('PERMISSION_ASSIGN',   'Assign Permissions',      'PERMISSION', 'MANAGE', 'Assign or revoke permissions on roles or users'),

    -- ── DOCTOR MODULE ────────────────────────────────────────
    ('DOCTOR_READ',        'View Doctors',           'DOCTOR', 'READ',   'Search and view doctor profiles'),
    ('DOCTOR_CREATE',      'Create Doctor',          'DOCTOR', 'CREATE', 'Add a new doctor profile'),
    ('DOCTOR_UPDATE',      'Update Doctor',          'DOCTOR', 'UPDATE', 'Edit doctor professional information'),
    ('DOCTOR_DELETE',      'Delete Doctor',          'DOCTOR', 'DELETE', 'Soft-delete a doctor profile'),
    ('DOCTOR_VIEW_OWN',    'View Own Doctor Profile','DOCTOR', 'READ',   'Doctor can view their own profile'),

    -- ── PATIENT MODULE ────────────────────────────────────────
    ('PATIENT_READ',        'View Patients',           'PATIENT', 'READ',   'Search and view patient records'),
    ('PATIENT_CREATE',      'Register Patient',        'PATIENT', 'CREATE', 'Add a new patient record'),
    ('PATIENT_UPDATE',      'Update Patient',          'PATIENT', 'UPDATE', 'Edit patient demographic information'),
    ('PATIENT_DELETE',      'Delete Patient',          'PATIENT', 'DELETE', 'Soft-delete a patient record'),
    ('PATIENT_VIEW_OWN',    'View Own Health Record',  'PATIENT', 'READ',   'Patient can view their own health record'),

    -- ── APPOINTMENT MODULE ────────────────────────────────────
    ('APPOINTMENT_READ',        'View Appointments',   'APPOINTMENT', 'READ',   'View appointment calendar and details'),
    ('APPOINTMENT_CREATE',      'Book Appointment',    'APPOINTMENT', 'CREATE', 'Schedule a new appointment'),
    ('APPOINTMENT_UPDATE',      'Edit Appointment',    'APPOINTMENT', 'UPDATE', 'Reschedule or modify an appointment'),
    ('APPOINTMENT_CANCEL',      'Cancel Appointment',  'APPOINTMENT', 'DELETE', 'Cancel an existing appointment'),
    ('APPOINTMENT_VIEW_OWN',    'View Own Appointments','APPOINTMENT','READ',   'Patient can view their own appointments'),

    -- ── MEDICAL RECORD MODULE ─────────────────────────────────
    ('RECORD_READ',         'View Medical Records',    'RECORD', 'READ',   'Access patient medical records'),
    ('RECORD_CREATE',       'Create Medical Record',   'RECORD', 'CREATE', 'Add clinical notes or records'),
    ('RECORD_UPDATE',       'Update Medical Record',   'RECORD', 'UPDATE', 'Edit existing clinical notes'),
    ('RECORD_DELETE',       'Delete Medical Record',   'RECORD', 'DELETE', 'Remove a medical record entry'),
    ('RECORD_VIEW_OWN',     'View Own Medical Records','RECORD', 'READ',   'Patient can view their own medical records'),

    -- ── PRESCRIPTION MODULE ───────────────────────────────────
    ('PRESCRIPTION_READ',   'View Prescriptions',      'PRESCRIPTION', 'READ',   'Access prescriptions'),
    ('PRESCRIPTION_CREATE', 'Issue Prescription',      'PRESCRIPTION', 'CREATE', 'Write a new prescription'),
    ('PRESCRIPTION_UPDATE', 'Update Prescription',     'PRESCRIPTION', 'UPDATE', 'Modify an existing prescription'),
    ('PRESCRIPTION_VIEW_OWN','View Own Prescriptions', 'PRESCRIPTION', 'READ',   'Patient views their own prescriptions'),

    -- ── BILLING / INVOICE MODULE ──────────────────────────────
    ('BILLING_READ',        'View Billing',            'BILLING', 'READ',   'Access invoices and billing history'),
    ('BILLING_CREATE',      'Create Invoice',          'BILLING', 'CREATE', 'Generate a new invoice'),
    ('BILLING_UPDATE',      'Update Invoice',          'BILLING', 'UPDATE', 'Edit or void an invoice'),
    ('BILLING_VIEW_OWN',    'View Own Bills',          'BILLING', 'READ',   'Patient can view their own bills'),

    -- ── REPORT MODULE ─────────────────────────────────────────
    ('REPORT_READ',         'View Reports',            'REPORT', 'READ',   'View analytics and system reports'),
    ('REPORT_EXPORT',       'Export Reports',          'REPORT', 'MANAGE', 'Download or export reports'),

    -- ── AUDIT / LOG MODULE ────────────────────────────────────
    ('AUDIT_READ',          'View Audit Logs',         'AUDIT', 'READ',   'Access system audit and activity logs'),

    -- ── SYSTEM / ADMIN MODULE ─────────────────────────────────
    ('SYSTEM_SETTINGS',     'Manage System Settings',  'SYSTEM', 'MANAGE', 'Configure global system settings'),
    ('SYSTEM_TENANT_MANAGE','Manage Tenants',          'SYSTEM', 'MANAGE', 'Create and configure tenants')

ON CONFLICT (code) DO NOTHING;

-- =============================================================
-- SECTION 2 — Assign capabilities to default roles
-- Uses sub-selects so IDs never need to be hard-coded
-- =============================================================

-- ── Helper: resolve role IDs once ────────────────────────────
-- (used inline per INSERT for clarity)

-- ─────────────────────────────────────────────────────────────
-- 2A. ADMIN  →  ALL permissions
-- ─────────────────────────────────────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- 2B. DOCTOR  →  Clinical + limited user/patient ops
-- ─────────────────────────────────────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN (
    'USER_VIEW_OWN',
    'USER_UPDATE_OWN',
    'PATIENT_READ',
    'PATIENT_CREATE',
    'PATIENT_UPDATE',
    'DOCTOR_READ',
    'DOCTOR_UPDATE',
    'DOCTOR_VIEW_OWN',
    'APPOINTMENT_READ',
    'APPOINTMENT_CREATE',
    'APPOINTMENT_UPDATE',
    'APPOINTMENT_CANCEL',
    'RECORD_READ',
    'RECORD_CREATE',
    'RECORD_UPDATE',
    'RECORD_DELETE',
    'PRESCRIPTION_READ',
    'PRESCRIPTION_CREATE',
    'PRESCRIPTION_UPDATE',
    'BILLING_READ',
    'BILLING_CREATE',
    'BILLING_UPDATE',
    'REPORT_READ'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- 2C. RECEPTIONIST  →  Front-desk ops (scheduling, billing, patient reg)
-- ─────────────────────────────────────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN (
    'USER_VIEW_OWN',
    'USER_UPDATE_OWN',
    'PATIENT_READ',
    'PATIENT_CREATE',
    'PATIENT_UPDATE',
    'DOCTOR_READ',
    'APPOINTMENT_READ',
    'APPOINTMENT_CREATE',
    'APPOINTMENT_UPDATE',
    'APPOINTMENT_CANCEL',
    'RECORD_READ',
    'BILLING_READ',
    'BILLING_CREATE',
    'BILLING_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- 2D. PATIENT  →  Self-service / read-only own data
-- ─────────────────────────────────────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN (
    'USER_VIEW_OWN',
    'USER_UPDATE_OWN',
    'PATIENT_READ',
    'PATIENT_UPDATE',
    'PATIENT_VIEW_OWN',
    'APPOINTMENT_READ',
    'APPOINTMENT_VIEW_OWN',
    'APPOINTMENT_CREATE',
    'APPOINTMENT_CANCEL',
    'RECORD_READ',
    'RECORD_VIEW_OWN',
    'PRESCRIPTION_READ',
    'PRESCRIPTION_VIEW_OWN',
    'BILLING_READ',
    'BILLING_VIEW_OWN'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;
