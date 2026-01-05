// K6 Test: REST API Integration Test
// Tests all REST endpoints for NexusChat Engine
// Usage: k6 run 01-rest-api.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { config, generateUUID, getToken, createAuthHeaders } from './config.js';

// Test configuration
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
  },
};

// Pre-test: Get JWT token for Alice
let aliceToken;
let bobToken;
let newRoomId;
let newMessageId;

export function setup() {
  // Get tokens for test users
  aliceToken = getToken(config.users.alice);
  bobToken = getToken(config.users.bob);

  console.log('Alice token:', aliceToken.substring(0, 20) + '...');
  console.log('Bob token:', bobToken.substring(0, 20) + '...');

  return {
    aliceToken,
    bobToken
  };
}

export default function(data) {
  const baseUrl = config.baseUrl;
  const aliceToken = data.aliceToken;
  const bobToken = data.bobToken;

  // ========================================================================
  // Test 1: Get JWT Token (Authentication)
  // ========================================================================
  console.log('\n=== Test 1: Get JWT Token ===');

  const tokenUrl = `${baseUrl}${config.endpoints.token}/${config.users.alice.id}`;
  const tokenParams = `name=${config.users.alice.name}`;
  const tokenRes = http.get(`${tokenUrl}?${tokenParams}`);

  check(tokenRes, {
    'GET /api/dev/token/{userId} - Status 200': (r) => r.status === 200,
    'GET /api/dev/token/{userId} - Has token': (r) => JSON.parse(r.body).token !== undefined,
    'GET /api/dev/token/{userId} - Has userId': (r) => JSON.parse(r.body).userId === config.users.alice.id,
    'GET /api/dev/token/{userId} - Has name': (r) => JSON.parse(r.body).name === config.users.alice.name,
  });

  const tokenData = JSON.parse(tokenRes.body);
  console.log('Token response:', JSON.stringify(tokenData, null, 2));

  sleep(1);

  // ========================================================================
  // Test 2: List Rooms
  // ========================================================================
  console.log('\n=== Test 2: List Rooms ===');

  const listRoomsRes = http.get(
    `${baseUrl}${config.endpoints.frontRooms}?page=0&size=20`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(listRoomsRes, {
    'GET /api/front/rooms - Status 200': (r) => r.status === 200,
    'GET /api/front/rooms - Returns array': (r) => Array.isArray(JSON.parse(r.body)),
    'GET /api/front/rooms - Has at least 1 room': (r) => JSON.parse(r.body).length >= 1,
    'GET /api/front/rooms - Room has id': (r) => JSON.parse(r.body)[0].id !== undefined,
    'GET /api/front/rooms - Room has type': (r) => JSON.parse(r.body)[0].type !== undefined,
  });

  const rooms = JSON.parse(listRoomsRes.body);
  console.log('Rooms count:', rooms.length);
  console.log('First room:', JSON.stringify(rooms[0], null, 2));

  sleep(1);

  // ========================================================================
  // Test 3: Get Room by ID
  // ========================================================================
  console.log('\n=== Test 3: Get Room by ID ===');

  const existingRoomId = config.rooms.engineering;
  const getRoomRes = http.get(
    `${baseUrl}${config.endpoints.frontRooms}/${existingRoomId}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(getRoomRes, {
    'GET /api/front/rooms/{id} - Status 200': (r) => r.status === 200,
    'GET /api/front/rooms/{id} - Has id': (r) => JSON.parse(r.body).id === existingRoomId,
    'GET /api/front/rooms/{id} - Has type': (r) => JSON.parse(r.body).type === 'GROUP',
    'GET /api/front/rooms/{id} - Has name': (r) => JSON.parse(r.body).name === 'Engineering Team',
  });

  const roomData = JSON.parse(getRoomRes.body);
  console.log('Room data:', JSON.stringify(roomData, null, 2));

  sleep(1);

  // ========================================================================
  // Test 4: Create Room (GROUP)
  // ========================================================================
  console.log('\n=== Test 4: Create Room (GROUP) ===');

  const createRoomPayload = JSON.stringify({
    type: config.roomTypes.GROUP,
    name: 'K6 Test Room ' + Date.now(),
    participantIds: [
      config.users.alice.id,
      config.users.bob.id
    ]
  });

  const createRoomRes = http.post(
    `${baseUrl}${config.endpoints.backRooms}`,
    createRoomPayload,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(createRoomRes, {
    'POST /api/back/rooms - Status 201': (r) => r.status === 201,
    'POST /api/back/rooms - Has id': (r) => JSON.parse(r.body).id !== undefined,
    'POST /api/back/rooms - Type is GROUP': (r) => JSON.parse(r.body).type === 'GROUP',
    'POST /api/back/rooms - Has name': (r) => JSON.parse(r.body).name !== undefined,
  });

  const createdRoom = JSON.parse(createRoomRes.body);
  newRoomId = createdRoom.id;
  console.log('Created room:', JSON.stringify(createdRoom, null, 2));

  sleep(1);

  // ========================================================================
  // Test 5: Create Room (DIRECT)
  // ========================================================================
  console.log('\n=== Test 5: Create Room (DIRECT) ===');

  const createDirectRoomPayload = JSON.stringify({
    type: config.roomTypes.DIRECT,
    participantIds: [
      config.users.alice.id,
      config.users.charlie.id
    ]
  });

  const createDirectRoomRes = http.post(
    `${baseUrl}${config.endpoints.backRooms}`,
    createDirectRoomPayload,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(createDirectRoomRes, {
    'POST /api/back/rooms (DIRECT) - Status 201': (r) => r.status === 201,
    'POST /api/back/rooms (DIRECT) - Type is DIRECT': (r) => JSON.parse(r.body).type === 'DIRECT',
  });

  console.log('Created direct room:', JSON.stringify(JSON.parse(createDirectRoomRes.body), null, 2));

  sleep(1);

  // ========================================================================
  // Test 6: Get Messages
  // ========================================================================
  console.log('\n=== Test 6: Get Messages ===');

  const getMessagesRes = http.get(
    `${baseUrl}${config.endpoints.frontMessages}/${config.rooms.engineering}?page=0&size=50`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(getMessagesRes, {
    'GET /api/front/messages/{roomId} - Status 200': (r) => r.status === 200,
    'GET /api/front/messages/{roomId} - Returns array': (r) => Array.isArray(JSON.parse(r.body)),
    'GET /api/front/messages/{roomId} - Message has id': (r) => {
      const messages = JSON.parse(r.body);
      return messages.length > 0 && messages[0].id !== undefined;
    },
    'GET /api/front/messages/{roomId} - Message has senderId': (r) => {
      const messages = JSON.parse(r.body);
      return messages.length > 0 && messages[0].senderId !== undefined;
    },
  });

  const messages = JSON.parse(getMessagesRes.body);
  console.log('Messages count:', messages.length);
  if (messages.length > 0) {
    console.log('First message:', JSON.stringify(messages[0], null, 2));
  }

  sleep(1);

  // ========================================================================
  // Test 7: Send Message (TEXT)
  // ========================================================================
  console.log('\n=== Test 7: Send Message (TEXT) ===');

  const clientRef = generateUUID();
  const sendMessagePayload = JSON.stringify({
    roomId: config.rooms.engineering,
    type: config.messageTypes.TEXT,
    contentText: 'Hello from K6 REST API test! ' + Date.now(),
    contentMeta: null,
    clientRef: clientRef
  });

  const sendMessageRes = http.post(
    `${baseUrl}${config.endpoints.backMessages}`,
    sendMessagePayload,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(sendMessageRes, {
    'POST /api/back/messages - Status 201': (r) => r.status === 201,
    'POST /api/back/messages - Has id': (r) => JSON.parse(r.body).id !== undefined,
    'POST /api/back/messages - Type is TEXT': (r) => JSON.parse(r.body).type === 'TEXT',
    'POST /api/back/messages - Has contentText': (r) => JSON.parse(r.body).contentText !== undefined,
    'POST /api/back/messages - Has createdAt': (r) => JSON.parse(r.body).createdAt !== undefined,
  });

  const sentMessage = JSON.parse(sendMessageRes.body);
  newMessageId = sentMessage.id;
  console.log('Sent message:', JSON.stringify(sentMessage, null, 2));

  sleep(1);

  // ========================================================================
  // Test 8: Send Message to Newly Created Room
  // ========================================================================
  console.log('\n=== Test 8: Send Message to Newly Created Room ===');

  const sendMessageToNewRoomPayload = JSON.stringify({
    roomId: newRoomId,
    type: config.messageTypes.TEXT,
    contentText: 'First message in new room! ' + Date.now(),
    contentMeta: null,
    clientRef: generateUUID()
  });

  const sendMessageToNewRoomRes = http.post(
    `${baseUrl}${config.endpoints.backMessages}`,
    sendMessageToNewRoomPayload,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(sendMessageToNewRoomRes, {
    'POST /api/back/messages (new room) - Status 201': (r) => r.status === 201,
    'POST /api/back/messages (new room) - Correct roomId': (r) => JSON.parse(r.body).roomId === newRoomId,
  });

  console.log('Sent message to new room:', JSON.stringify(JSON.parse(sendMessageToNewRoomRes.body), null, 2));

  sleep(1);

  // ========================================================================
  // Test 9: Verify Idempotency (Same clientRef)
  // ========================================================================
  console.log('\n=== Test 9: Verify Idempotency (Same clientRef) ===');

  const sameClientRefPayload = JSON.stringify({
    roomId: config.rooms.engineering,
    type: config.messageTypes.TEXT,
    contentText: 'This should only be sent once!',
    contentMeta: null,
    clientRef: clientRef // Same clientRef as before
  });

  const sameClientRefRes = http.post(
    `${baseUrl}${config.endpoints.backMessages}`,
    sameClientRefPayload,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(sameClientRefRes, {
    'POST /api/back/messages (idempotent) - Status 409 or 200': (r) => r.status === 409 || r.status === 200,
    'POST /api/back/messages (idempotent) - Returns same message': (r) => {
      const body = JSON.parse(r.body);
      return body.id === newMessageId;
    },
  });

  console.log('Idempotency check response:', sameClientRefRes.status, JSON.parse(sameClientRefRes.body));

  sleep(1);

  // ========================================================================
  // Test 10: Pagination Test
  // ========================================================================
  console.log('\n=== Test 10: Pagination Test ===');

  const page1Res = http.get(
    `${baseUrl}${config.endpoints.frontMessages}/${config.rooms.engineering}?page=0&size=2`,
    { headers: createAuthHeaders(aliceToken) }
  );

  const page2Res = http.get(
    `${baseUrl}${config.endpoints.frontMessages}/${config.rooms.engineering}?page=1&size=2`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(page1Res, {
    'GET /api/front/messages (page 1) - Status 200': (r) => r.status === 200,
    'GET /api/front/messages (page 1) - Has 2 messages': (r) => JSON.parse(r.body).length <= 2,
  });

  check(page2Res, {
    'GET /api/front/messages (page 2) - Status 200': (r) => r.status === 200,
  });

  console.log('Page 1 messages:', JSON.parse(page1Res.body).length);
  console.log('Page 2 messages:', JSON.parse(page2Res.body).length);

  sleep(1);

  // ========================================================================
  // Test 11: Unauthorized Access (No Token)
  // ========================================================================
  console.log('\n=== Test 11: Unauthorized Access (No Token) ===');

  const noTokenRes = http.get(`${baseUrl}${config.endpoints.frontRooms}`);

  check(noTokenRes, {
    'GET /api/front/rooms (no token) - Status 401': (r) => r.status === 401,
  });

  console.log('No token response:', noTokenRes.status);

  sleep(1);

  // ========================================================================
  // Test 12: Invalid Room ID
  // ========================================================================
  console.log('\n=== Test 12: Invalid Room ID ===');

  const invalidRoomId = generateUUID();
  const invalidRoomRes = http.get(
    `${baseUrl}${config.endpoints.frontRooms}/${invalidRoomId}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(invalidRoomRes, {
    'GET /api/front/rooms/{invalidId} - Status 404': (r) => r.status === 404,
  });

  console.log('Invalid room response:', invalidRoomRes.status);

  // ========================================================================
  // Summary
  // ========================================================================
  console.log('\n=== REST API Test Summary ===');
  console.log('New room created:', newRoomId);
  console.log('New message created:', newMessageId);
  console.log('All tests completed!');
}

// Export data for verification
export function teardown(data) {
  console.log('\n=== Test Teardown ===');
  console.log('Test completed. Verify data in database:');
  console.log('- New room ID:', newRoomId);
  console.log('- New message ID:', newMessageId);
  console.log('\nSQL queries for verification:');
  console.log(`SELECT * FROM rooms WHERE id = '${newRoomId}';`);
  console.log(`SELECT * FROM messages WHERE id = '${newMessageId}';`);
  console.log(`SELECT * FROM room_participants WHERE room_id = '${newRoomId}';`);
}
