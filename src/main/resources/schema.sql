CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: chat_users
CREATE TABLE chat_users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: rooms
CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(10) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(255),
    last_message_preview TEXT,
    tenant_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_rooms_tenant_updated_at ON rooms (tenant_id, updated_at DESC);

-- Table: room_participants
CREATE TABLE room_participants (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES chat_users(id),
    role VARCHAR(10) NOT NULL CHECK (role IN ('MEMBER', 'ADMIN')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

-- Table: messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES chat_users(id),
    type VARCHAR(15) NOT NULL CHECK (type IN ('TEXT', 'IMAGE', 'FILE', 'AUDIO', 'VIDEO', 'SYSTEM', 'VOICE_CALL')),
    content_text TEXT,
    content_meta JSONB,
    client_ref UUID UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ
);
CREATE INDEX idx_messages_room_history ON messages(room_id, created_at DESC);

-- Table: room_read_states
CREATE TABLE room_read_states (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES chat_users(id),
    last_read_message_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

-- Table: files
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploader_id UUID NOT NULL REFERENCES chat_users(id),
    object_key VARCHAR(512) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    mime VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    expiration_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- ARCHIVE TABLES (Read-only audit trail)
-- ============================================================================

-- Archived rooms (read-only audit)
CREATE TABLE archived_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_room_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL,
    name VARCHAR(255),
    tenant_id VARCHAR(50) NOT NULL,
    participant_count INT NOT NULL DEFAULT 0,
    message_count INT NOT NULL DEFAULT 0,
    first_message_at TIMESTAMPTZ,
    last_message_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_by VARCHAR(100) NOT NULL
);
CREATE INDEX idx_archived_rooms_tenant ON archived_rooms(tenant_id, archived_at DESC);
CREATE INDEX idx_archived_rooms_original ON archived_rooms(original_room_id);

-- Archived messages (read-only audit)
CREATE TABLE archived_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    archived_room_id UUID NOT NULL,
    original_room_id UUID NOT NULL,
    original_message_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    type VARCHAR(15) NOT NULL,
    content_text TEXT,
    content_meta JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_archived_messages_room ON archived_messages(archived_room_id, created_at);
CREATE INDEX idx_archived_messages_sender ON archived_messages(sender_id);

-- Archived room participants (for audit trail)
CREATE TABLE archived_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    archived_room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_archived_participants_room ON archived_participants(archived_room_id);

