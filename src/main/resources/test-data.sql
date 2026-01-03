-- Test Data for NexusChat Engine
-- Run this after creating the database and schema

-- Insert test users
INSERT INTO chat_users (id, name, tenant_id, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'Alice', 'test-tenant', NOW()),
('550e8400-e29b-41d4-a716-446655440001', 'Bob', 'test-tenant', NOW()),
('550e8400-e29b-41d4-a716-446655440002', 'Charlie', 'test-tenant', NOW()),
('550e8400-e29b-41d4-a716-446655440003', 'Diana', 'test-tenant', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert test rooms
INSERT INTO rooms (id, type, name, tenant_id, created_at, updated_at) VALUES
('650e8400-e29b-41d4-a716-446655440000', 'GROUP', 'Engineering Team', 'test-tenant', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440001', 'GROUP', 'Product Discussion', 'test-tenant', NOW(), NOW()),
('650e8400-e29b-41d4-a716-446655440002', 'DIRECT', NULL, 'test-tenant', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert room participants
INSERT INTO room_participants (room_id, user_id, role, joined_at) VALUES
-- Engineering Team
('650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440000', 'ADMIN', NOW()),
('650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440001', 'MEMBER', NOW()),
('650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440002', 'MEMBER', NOW()),
-- Product Discussion
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'MEMBER', NOW()),
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440003', 'ADMIN', NOW()),
-- Direct Chat (Alice & Bob)
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', 'MEMBER', NOW()),
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'MEMBER', NOW())
ON CONFLICT (room_id, user_id) DO NOTHING;

-- Insert test messages
INSERT INTO messages (id, room_id, sender_id, type, content_text, created_at) VALUES
('750e8400-e29b-41d4-a716-446655440000', '650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440000', 'TEXT', 'Welcome to the Engineering Team chat!', NOW()),
('750e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440001', 'TEXT', 'Thanks Alice! Looking forward to collaborating.', NOW()),
('750e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440002', 'TEXT', 'Hello everyone! 👋', NOW()),
('750e8400-e29b-41d4-a716-446655440003', '650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440003', 'TEXT', 'Can we discuss the new feature requirements?', NOW()),
('750e8400-e29b-41d4-a716-446655440004', '650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', 'TEXT', 'Hi Bob, let sync up on the project.', NOW())
ON CONFLICT (client_ref) DO NOTHING;

-- Insert read states
INSERT INTO room_read_states (room_id, user_id, last_read_message_id, updated_at) VALUES
('650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440000', '750e8400-e29b-41d4-a716-446655440002', NOW()),
('650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440002', NOW()),
('650e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440002', NOW())
ON CONFLICT (room_id, user_id) DO NOTHING;

-- Verify data
SELECT 'Users:' as info;
SELECT id, name, tenant_id FROM chat_users ORDER BY name;

SELECT 'Rooms:' as info;
SELECT id, type, name, tenant_id FROM rooms ORDER BY created_at;

SELECT 'Messages (last 5):' as info;
SELECT id, sender_id, type, LEFT(content_text, 50) as content, created_at
FROM messages ORDER BY created_at DESC LIMIT 5;
