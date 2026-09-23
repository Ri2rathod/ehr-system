-- V14: Add missing appointment permissions and update role assignments

-- Add missing appointment permissions
INSERT INTO permissions (code, name, module, action, description) VALUES
    ('APPOINTMENT_RESCHEDULE',    'Reschedule Appointment',    'APPOINTMENT', 'UPDATE',  'Reschedule an existing appointment to a new time'),
    ('APPOINTMENT_CHECK_IN',      'Check In Patient',          'APPOINTMENT', 'UPDATE',  'Mark patient as arrived and check in for appointment'),
    ('APPOINTMENT_STATUS_UPDATE', 'Update Appointment Status', 'APPOINTMENT', 'UPDATE',  'Progress appointment through workflow states'),
    ('APPOINTMENT_VIEW_ALL',      'View All Appointments',     'APPOINTMENT', 'READ',    'View all appointments across doctors and patients')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: grant all new permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('APPOINTMENT_RESCHEDULE', 'APPOINTMENT_CHECK_IN', 'APPOINTMENT_STATUS_UPDATE', 'APPOINTMENT_VIEW_ALL')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECEPTIONIST: add CHECK_IN, STATUS_UPDATE, VIEW_ALL, RESCHEDULE
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN ('APPOINTMENT_CHECK_IN', 'APPOINTMENT_STATUS_UPDATE', 'APPOINTMENT_VIEW_ALL', 'APPOINTMENT_RESCHEDULE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR: add STATUS_UPDATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN ('APPOINTMENT_STATUS_UPDATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
