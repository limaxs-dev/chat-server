// K6 Test Configuration for NexusChat Engine
// This file contains shared configuration for all K6 tests

import http from 'k6/http';

export const config = {
  // Server configuration
  baseUrl: __ENV.BASE_URL || 'http://localhost:8080',
  wsUrl: __ENV.WS_URL || 'ws://localhost:8080/ws/chat',

  // Test user credentials
  users: {
    alice: {
      id: '550e8400-e29b-41d4-a716-446655440000',
      name: 'Alice'
    },
    bob: {
      id: '550e8400-e29b-41d4-a716-446655440001',
      name: 'Bob'
    },
    charlie: {
      id: '550e8400-e29b-41d4-a716-446655440002',
      name: 'Charlie'
    },
    diana: {
      id: '550e8400-e29b-41d4-a716-446655440003',
      name: 'Diana'
    }
  },

  // Test room IDs
  rooms: {
    engineering: '650e8400-e29b-41d4-a716-446655440000', // GROUP - Engineering Team
    product: '650e8400-e29b-41d4-a716-446655440001',     // GROUP - Product Discussion
    direct: '650e8400-e29b-41d4-a716-446655440002'       // DIRECT - Alice & Bob
  },

  // Test message IDs
  messages: {
    msg1: '750e8400-e29b-41d4-a716-446655440000',
    msg2: '750e8400-e29b-41d4-a716-446655440001',
    msg3: '750e8400-e29b-41d4-a716-446655440002',
    msg4: '750e8400-e29b-41d4-a716-446655440003',
    msg5: '750e8400-e29b-41d4-a716-446655440004'
  },

  // API endpoints - /api/front/* for frontend, /api/back/* for backend
  endpoints: {
    token: '/api/dev/token',
    // Frontend API (/api/front/*)
    frontRooms: '/api/front/rooms',
    frontMessages: '/api/front/messages',
    frontDownloadUrl: (fileId) => `/api/front/files/${fileId}/download-url`,
    frontConfig: '/api/front/config/webrtc',
    // Backend API (/api/back/*)
    backRooms: '/api/back/rooms',
    backMessages: '/api/back/messages',
    backUploadUrl: '/api/back/files/upload-url',
    backConfirmUpload: (fileId) => `/api/back/files/${fileId}/confirm`,
    backAdminArchive: (roomId) => `/api/back/admin/archive/${roomId}`,
    backArchive: '/api/back/archive'
  },

  // WebSocket events
  events: {
    SEND_MSG: 'SEND_MSG',
    SIGNAL_SDP: 'SIGNAL_SDP',
    SIGNAL_ICE: 'SIGNAL_ICE',
    TYPING: 'TYPING',
    ACK: 'ACK',
    NEW_MESSAGE: 'NEW_MESSAGE',
    MESSAGE_EDITED: 'MESSAGE_EDITED',
    MESSAGE_DELETED: 'MESSAGE_DELETED',
    PRESENCE: 'PRESENCE',
    CALL_REJECTED: 'CALL_REJECTED'
  },

  // Message types
  messageTypes: {
    TEXT: 'TEXT',
    IMAGE: 'IMAGE',
    FILE: 'FILE',
    AUDIO: 'AUDIO',
    VIDEO: 'VIDEO',
    SYSTEM: 'SYSTEM',
    VOICE_CALL: 'VOICE_CALL'
  },

  // Room types
  roomTypes: {
    DIRECT: 'DIRECT',
    GROUP: 'GROUP'
  }
};

// Helper function to generate a random UUID
export function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// Helper function to get JWT token for a user
export function getToken(user) {
  const url = `${config.baseUrl}${config.endpoints.token}/${user.id}`;
  const params = `name=${encodeURIComponent(user.name)}`;
  const res = http.get(`${url}?${params}`);

  if (res.status !== 200) {
    throw new Error(`Failed to get token: ${res.status} ${res.body}`);
  }

  const data = JSON.parse(res.body);
  return data.token;
}

// Helper function to create authenticated headers
export function createAuthHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
}
