// K6 Test: Full End-to-End Integration Test
// Tests the complete workflow of NexusChat Engine
// Usage: k6 run 04-integration-test.js

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { config, generateUUID, getToken, createAuthHeaders } from './config.js';

// Test configuration
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

// Global test state
const testState = {
  aliceToken: null,
  bobToken: null,
  newRoomId: null,
  newMessageId: null,
  uploadedFileId: null,
  wsMessages: [],
  wsConnected: false
};

export function setup() {
  console.log('\n=== Setup: Getting tokens for test users ===');

  // Get tokens for test users
  testState.aliceToken = getToken(config.users.alice);
  testState.bobToken = getToken(config.users.bob);

  console.log('Alice token:', testState.aliceToken.substring(0, 20) + '...');
  console.log('Bob token:', testState.bobToken.substring(0, 20) + '...');

  return testState;
}

export default function(data) {
  const baseUrl = config.baseUrl;
  const wsUrl = `${config.wsUrl}?token=${data.aliceToken}`;

  console.log('\n========================================');
  console.log('NEXUSCHAT ENGINE - INTEGRATION TEST');
  console.log('========================================');

  // ========================================================================
  // PHASE 1: AUTHENTICATION
  // ========================================================================
  console.log('\n--- PHASE 1: AUTHENTICATION ---');

  const tokenRes = http.get(
    `${baseUrl}${config.endpoints.token}/${config.users.alice.id}?name=${config.users.alice.name}&tenant=${config.users.alice.tenant}`
  );

  check(tokenRes, {
    '[AUTH] Get token - Status 200': (r) => r.status === 200,
    '[AUTH] Get token - Has JWT': (r) => JSON.parse(r.body).token !== undefined,
  });

  const tokenData = JSON.parse(tokenRes.body);
  console.log('[AUTH] Token obtained for user:', tokenData.name);

  sleep(1);

  // ========================================================================
  // PHASE 2: ROOM MANAGEMENT
  // ========================================================================
  console.log('\n--- PHASE 2: ROOM MANAGEMENT ---');

  // 2.1: List existing rooms
  const listRoomsRes = http.get(
    `${baseUrl}${config.endpoints.rooms}?page=0&size=20`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(listRoomsRes, {
    '[ROOM] List rooms - Status 200': (r) => r.status === 200,
    '[ROOM] List rooms - Has rooms': (r) => JSON.parse(r.body).length >= 1,
  });

  const existingRooms = JSON.parse(listRoomsRes.body);
  console.log('[ROOM] Found', existingRooms.length, 'existing rooms');

  sleep(1);

  // 2.2: Create a new GROUP room
  const newRoomName = 'Integration Test Room ' + Date.now();
  const createRoomRes = http.post(
    `${baseUrl}${config.endpoints.rooms}`,
    JSON.stringify({
      type: config.roomTypes.GROUP,
      name: newRoomName,
      participantIds: [config.users.alice.id, config.users.bob.id]
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(createRoomRes, {
    '[ROOM] Create room - Status 201': (r) => r.status === 201,
    '[ROOM] Create room - Has ID': (r) => JSON.parse(r.body).id !== undefined,
    '[ROOM] Create room - Type is GROUP': (r) => JSON.parse(r.body).type === 'GROUP',
  });

  testState.newRoomId = JSON.parse(createRoomRes.body).id;
  console.log('[ROOM] Created new room:', testState.newRoomId);

  sleep(1);

  // 2.3: Get the newly created room
  const getRoomRes = http.get(
    `${baseUrl}${config.endpoints.rooms}/${testState.newRoomId}`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getRoomRes, {
    '[ROOM] Get room - Status 200': (r) => r.status === 200,
    '[ROOM] Get room - Correct name': (r) => JSON.parse(r.body).name === newRoomName,
  });

  console.log('[ROOM] Retrieved room details');

  sleep(1);

  // ========================================================================
  // PHASE 3: FILE UPLOAD (Prepare for image message)
  // ========================================================================
  console.log('\n--- PHASE 3: FILE UPLOAD ---');

  const imageFileName = 'integration-test-image-' + Date.now() + '.png';
  const imageFileSize = 256000; // 250 KB
  const imageContentType = 'image/png';

  const uploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(imageFileName)}&fileSize=${imageFileSize}&contentType=${encodeURIComponent(imageContentType)}`,
    null,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(uploadUrlRes, {
    '[FILE] Get upload URL - Status 200': (r) => r.status === 200,
    '[FILE] Get upload URL - Has fileId': (r) => JSON.parse(r.body).fileId !== undefined,
  });

  const uploadData = JSON.parse(uploadUrlRes.body);
  testState.uploadedFileId = uploadData.fileId;

  console.log('[FILE] Got upload URL for file:', testState.uploadedFileId);

  sleep(1);

  // Confirm upload (simulated)
  const confirmUploadRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(testState.uploadedFileId)}`,
    '',
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(confirmUploadRes, {
    '[FILE] Confirm upload - Status 204': (r) => r.status === 204,
  });

  console.log('[FILE] Upload confirmed');

  sleep(1);

  // ========================================================================
  // PHASE 4: MESSAGING (REST API)
  // ========================================================================
  console.log('\n--- PHASE 4: MESSAGING (REST API) ---');

  // 4.1: Send a text message
  const clientRef = generateUUID();
  const textMessageRes = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.newRoomId,
      type: config.messageTypes.TEXT,
      contentText: 'Hello from integration test! ' + Date.now(),
      contentMeta: null,
      clientRef: clientRef
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(textMessageRes, {
    '[MSG] Send text - Status 201': (r) => r.status === 201,
    '[MSG] Send text - Has ID': (r) => JSON.parse(r.body).id !== undefined,
    '[MSG] Send text - Type is TEXT': (r) => JSON.parse(r.body).type === 'TEXT',
  });

  testState.newMessageId = JSON.parse(textMessageRes.body).id;
  console.log('[MSG] Sent text message:', testState.newMessageId);

  sleep(1);

  // 4.2: Send an image message (referencing uploaded file)
  const imageMessageRes = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.newRoomId,
      type: config.messageTypes.IMAGE,
      contentText: 'Check out this image!',
      contentMeta: JSON.stringify({ fileId: testState.uploadedFileId }),
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(imageMessageRes, {
    '[MSG] Send image - Status 201': (r) => r.status === 201,
    '[MSG] Send image - Type is IMAGE': (r) => JSON.parse(r.body).type === 'IMAGE',
  });

  const imageMessageId = JSON.parse(imageMessageRes.body).id;
  console.log('[MSG] Sent image message:', imageMessageId);

  sleep(1);

  // 4.3: Get messages from the room
  const getMessagesRes = http.get(
    `${baseUrl}${config.endpoints.messages}/${testState.newRoomId}?page=0&size=10`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getMessagesRes, {
    '[MSG] Get messages - Status 200': (r) => r.status === 200,
    '[MSG] Get messages - Has messages': (r) => JSON.parse(r.body).length >= 2,
    '[MSG] Get messages - Contains text message': (r) => {
      const msgs = JSON.parse(r.body);
      return msgs.some(m => m.id === testState.newMessageId);
    },
  });

  const messages = JSON.parse(getMessagesRes.body);
  console.log('[MSG] Retrieved', messages.length, 'messages from room');

  sleep(1);

  // ========================================================================
  // PHASE 5: WEBSOCKET CONNECTION
  // ========================================================================
  console.log('\n--- PHASE 5: WEBSOCKET CONNECTION ---');

  const wsRes = ws.connect(wsUrl, {}, function (socket) {
    testState.wsConnected = true;
    console.log('[WS] WebSocket connected');

    socket.on('open', () => {
      console.log('[WS] Connection opened');

      // Send a message via WebSocket
      const wsMessage = {
        event: config.events.SEND_MSG,
        traceId: generateUUID(),
        data: {
          roomId: testState.newRoomId,
          type: config.messageTypes.TEXT,
          contentText: 'Hello via WebSocket! ' + Date.now(),
          contentMeta: null,
          clientRef: generateUUID()
        }
      };

      socket.send(JSON.stringify(wsMessage));
      console.log('[WS] Sent message via WebSocket');
    });

    socket.on('message', (message) => {
      const msg = JSON.parse(message);
      testState.wsMessages.push(msg);
      console.log('[WS] Received event:', msg.event);

      // Verify message structure
      if (msg.event === config.events.NEW_MESSAGE) {
        check(msg, {
          '[WS] NEW_MESSAGE - Has data': (m) => m.data !== undefined,
          '[WS] NEW_MESSAGE - Has senderId': (m) => m.data.senderId !== undefined,
        });
      }
    });

    socket.on('error', (error) => {
      console.error('[WS] Error:', error);
    });

    // Close connection after 5 seconds
    setTimeout(() => {
      console.log('[WS] Closing connection');
      socket.close();
    }, 5000);
  });

  check(wsRes, {
    '[WS] Connection - Status 101': (r) => r.status === 101,
    '[WS] Connection - Connected': () => testState.wsConnected,
  });

  // Wait for WebSocket messages
  sleep(7);

  // Verify WebSocket received messages
  check(testState.wsMessages, {
    '[WS] Received - At least 1 message': () => testState.wsMessages.length > 0,
    '[WS] Received - NEW_MESSAGE event': () => {
      return testState.wsMessages.some(m => m.event === config.events.NEW_MESSAGE);
    },
    '[WS] Received - PRESENCE event': () => {
      return testState.wsMessages.some(m => m.event === config.events.PRESENCE);
    },
  });

  console.log('[WS] Total messages received:', testState.wsMessages.length);
  console.log('[WS] Event types:', [...new Set(testState.wsMessages.map(m => m.event))]);

  sleep(1);

  // ========================================================================
  // PHASE 6: TYPING INDICATOR
  // ========================================================================
  console.log('\n--- PHASE 6: TYPING INDICATOR ---');

  const ws2Res = ws.connect(wsUrl, {}, function (socket) {
    socket.on('open', () => {
      // Send typing indicator
      const typingMessage = {
        event: config.events.TYPING,
        traceId: generateUUID(),
        data: {
          roomId: testState.newRoomId,
          isTyping: true
        }
      };

      socket.send(JSON.stringify(typingMessage));
      console.log('[TYPING] Sent typing indicator');

      // Send stop typing after 1 second
      setTimeout(() => {
        const stopTypingMessage = {
          event: config.events.TYPING,
          traceId: generateUUID(),
          data: {
            roomId: testState.newRoomId,
            isTyping: false
          }
        };

        socket.send(JSON.stringify(stopTypingMessage));
        console.log('[TYPING] Sent stop typing');
      }, 1000);

      // Close connection
      setTimeout(() => socket.close(), 2000);
    });

    socket.on('message', (message) => {
      const msg = JSON.parse(message);
      if (msg.event === config.events.TYPING) {
        console.log('[TYPING] Received typing indicator');
      }
    });
  });

  check(ws2Res, {
    '[TYPING] Connection - Status 101': (r) => r.status === 101,
  });

  sleep(4);

  // ========================================================================
  // PHASE 7: FILE DOWNLOAD
  // ========================================================================
  console.log('\n--- PHASE 7: FILE DOWNLOAD ---');

  const downloadUrlRes = http.get(
    `${baseUrl}${config.endpoints.downloadUrl(testState.uploadedFileId)}`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(downloadUrlRes, {
    '[FILE] Get download URL - Status 200': (r) => r.status === 200,
    '[FILE] Get download URL - Has URL': (r) => JSON.parse(r.body).downloadUrl !== undefined,
  });

  console.log('[FILE] Got download URL');

  sleep(1);

  // ========================================================================
  // PHASE 8: MULTI-USER SCENARIO
  // ========================================================================
  console.log('\n--- PHASE 8: MULTI-USER SCENARIO ---');

  // Bob sends a message to the same room
  const bobMessageRes = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.newRoomId,
      type: config.messageTypes.TEXT,
      contentText: 'Bob replied from integration test!',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(bobMessageRes, {
    '[MULTI] Bob send message - Status 201': (r) => r.status === 201,
    '[MULTI] Bob send message - Has ID': (r) => JSON.parse(r.body).id !== undefined,
  });

  const bobMessageId = JSON.parse(bobMessageRes.body).id;
  console.log('[MULTI] Bob sent message:', bobMessageId);

  sleep(1);

  // Alice gets messages (should see Bob's message)
  const aliceGetMessagesRes = http.get(
    `${baseUrl}${config.endpoints.messages}/${testState.newRoomId}?page=0&size=10`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(aliceGetMessagesRes, {
    '[MULTI] Alice get messages - Status 200': (r) => r.status === 200,
    '[MULTI] Alice get messages - Has Bob message': (r) => {
      const msgs = JSON.parse(r.body);
      return msgs.some(m => m.id === bobMessageId);
    },
  });

  console.log('[MULTI] Alice can see Bob\'s message');

  sleep(1);

  // ========================================================================
  // PHASE 9: ERROR HANDLING
  // ========================================================================
  console.log('\n--- PHASE 9: ERROR HANDLING ---');

  // 9.1: Try to get non-existent room
  const fakeRoomId = generateUUID();
  const fakeRoomRes = http.get(
    `${baseUrl}${config.endpoints.rooms}/${fakeRoomId}`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(fakeRoomRes, {
    '[ERR] Non-existent room - Status 404': (r) => r.status === 404,
  });

  console.log('[ERR] Non-existent room returns 404');

  sleep(1);

  // 9.2: Try to send message to non-existent room
  const fakeMessageRes = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: fakeRoomId,
      type: config.messageTypes.TEXT,
      contentText: 'This should fail',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(fakeMessageRes, {
    '[ERR] Message to fake room - Status 400 or 404': (r) => r.status === 400 || r.status === 404,
  });

  console.log('[ERR] Message to fake room rejected');

  sleep(1);

  // 9.3: Unauthorized request (no token)
  const noAuthRes = http.get(`${baseUrl}${config.endpoints.rooms}`);

  check(noAuthRes, {
    '[ERR] No token - Status 401': (r) => r.status === 401,
  });

  console.log('[ERR] Unauthorized request returns 401');

  // ========================================================================
  // TEST SUMMARY
  // ========================================================================
  console.log('\n========================================');
  console.log('INTEGRATION TEST SUMMARY');
  console.log('========================================');
  console.log('Room created:', testState.newRoomId);
  console.log('Text message sent:', testState.newMessageId);
  console.log('Image message sent:', imageMessageId);
  console.log('Bob message sent:', bobMessageId);
  console.log('File uploaded:', testState.uploadedFileId);
  console.log('WebSocket connected:', testState.wsConnected);
  console.log('WebSocket messages received:', testState.wsMessages.length);
  console.log('\n✅ All integration tests completed!');
  console.log('========================================\n');
}

export function teardown(data) {
  console.log('\n========================================');
  console.log('INTEGRATION TEST - DATABASE VERIFICATION');
  console.log('========================================');
  console.log('\nRun these SQL queries to verify data:');

  console.log('\n-- Verify room was created:');
  console.log(`SELECT id, type, name, tenant_id FROM rooms WHERE id = '${testState.newRoomId}';`);

  console.log('\n-- Verify room participants:');
  console.log(`SELECT room_id, user_id, role FROM room_participants WHERE room_id = '${testState.newRoomId}';`);

  console.log('\n-- Verify messages were sent:');
  console.log(`SELECT id, room_id, sender_id, type, LEFT(content_text, 50) as content FROM messages WHERE room_id = '${testState.newRoomId}';`);

  console.log('\n-- Verify file was uploaded:');
  console.log(`SELECT id, file_name, mime, size, is_confirmed FROM files WHERE id = '${testState.uploadedFileId}';`);

  console.log('\n-- Count all messages in test room:');
  console.log(`SELECT COUNT(*) as message_count FROM messages WHERE room_id = '${testState.newRoomId}';`);

  console.log('\n========================================\n');
}
