INSERT INTO equipment (id, equipment_id, name, factory_area, status, last_heartbeat_time, created_at, updated_at)
VALUES
    (1, 'EQP-001', 'Wire Bonder 01', 'A-Line', 'RUNNING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'EQP-002', 'Die Bonder 01', 'A-Line', 'RUNNING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'EQP-003', 'Tester 01', 'B-Line', 'RUNNING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE equipment ALTER COLUMN id RESTART WITH 4;

