-- V8: Fix PATIENT role missing CRUD self-service permissions
-- Previously V3 seeded PATIENT role without PATIENT_READ/PATIENT_UPDATE,
-- causing 403 on patient self-service endpoints even though V3 listed them.
-- This migration ensures the role_permissions table reflects V3 intent.

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN (
    'PATIENT_READ',
    'PATIENT_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;