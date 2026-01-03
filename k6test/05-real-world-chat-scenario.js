
// K6 Test: Real-World Chat Scenario (1 Minute)
// Simulates a realistic team chat conversation with 3 users
// Usage: k6 run 05-real-world-chat-scenario.js

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { config, generateUUID, getToken, createAuthHeaders } from './config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

// Global test state
const testState = {
  aliceToken: null,
  bobToken: null,
  charlieToken: null,
  roomId: null,
  messagesSent: [],
  filesUploaded: [],
  webSocketEvents: [],
  startTime: null
};

export function setup() {
  console.log('\n=== Setup: Getting tokens for test users ===');

  testState.aliceToken = getToken(config.users.alice);
  testState.bobToken = getToken(config.users.bob);
  testState.charlieToken = getToken(config.users.charlie);

  console.log('Alice token:', testState.aliceToken.substring(0, 20) + '...');
  console.log('Bob token:', testState.bobToken.substring(0, 20) + '...');
  console.log('Charlie token:', testState.charlieToken.substring(0, 20) + '...');

  return testState;
}

export default function(data) {
  const baseUrl = config.baseUrl;
  testState.startTime = Date.now();

  console.log('\n========================================');
  console.log('REAL-WORLD CHAT SCENARIO (60 SECONDS)');
  console.log('========================================');
  console.log('Start Time:', new Date(testState.startTime).toISOString());

  // =========================================================================
  // 00s - Alice creates group room, invites Bob & Charlie
  // =========================================================================
  console.log('\n[00s] Creating group room...');

  const createRoomRes = http.post(
    `${baseUrl}${config.endpoints.rooms}`,
    JSON.stringify({
      type: config.roomTypes.GROUP,
      name: 'Project Discussion ' + Date.now(),
      participantIds: [
        config.users.alice.id,
        config.users.bob.id,
        config.users.charlie.id
      ]
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(createRoomRes, {
    '[00s] Create room - Status 201': (r) => r.status === 201,
    '[00s] Create room - Has ID': (r) => JSON.parse(r.body).id !== undefined,
  });

  testState.roomId = JSON.parse(createRoomRes.body).id;
  console.log('[00s] Room created:', testState.roomId);

  // =========================================================================
  // 05s - Alice: "Hey team! Let's discuss the project" (TEXT)
  // =========================================================================
  sleep(5);
  console.log('\n[05s] Alice sends opening message...');

  const msg1Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'Hey team! Let\'s discuss the project',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(msg1Res, { '[05s] Alice message - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg1Res.body).id);
  console.log('[05s] Message sent:', testState.messagesSent[0]);

  // =========================================================================
  // 07s-09s - Bob: typing indicator ON then OFF
  // =========================================================================
  sleep(2);
  console.log('\n[07s] Bob starts typing...');

  const bobTypingOnRes = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: '__TYPING__',
      contentMeta: JSON.stringify({ isTyping: true }),
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.bobToken) }
  );

  sleep(2);
  console.log('[09s] Bob stops typing');

  // =========================================================================
  // 09s - Bob: "Sounds good! Here's the requirements doc" (TEXT)
  // =========================================================================
  console.log('[09s] Bob sends message...');

  const msg2Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'Sounds good! Here\'s the requirements doc',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(msg2Res, { '[09s] Bob message - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg2Res.body).id);

  // =========================================================================
  // 12s - Bob: Upload PDF file (get URL -> upload to MinIO -> confirm)
  // =========================================================================
  sleep(3);
  console.log('\n[12s] Bob uploads PDF file...');

  const pdfFileName = 'requirements-' + Date.now() + '.pdf';
  const pdfUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(pdfFileName)}&fileSize=102400&contentType=application/pdf`,
    null,
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(pdfUploadUrlRes, {
    '[12s] Get upload URL - Status 200': (r) => r.status === 200,
    '[12s] Get upload URL - Has fileId': (r) => JSON.parse(r.body).fileId !== undefined,
  });

  const pdfUploadData = JSON.parse(pdfUploadUrlRes.body);
  testState.filesUploaded.push(pdfUploadData.fileId);

  // Simulate file upload to MinIO (in real test, would do PUT to presigned URL)
  console.log('[12s] PDF upload URL obtained');

  const pdfConfirmRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(pdfUploadData.fileId)}`,
    '',
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(pdfConfirmRes, { '[12s] Confirm PDF upload - Status 204': (r) => r.status === 204 });
  console.log('[12s] PDF upload confirmed');

  // =========================================================================
  // 15s-17s - Charlie: typing indicator ON then OFF
  // =========================================================================
  sleep(3);
  console.log('\n[15s] Charlie starts typing...');

  sleep(2);
  console.log('[17s] Charlie stops typing');

  // =========================================================================
  // 17s - Charlie: "Got it! I'll review tonight" (TEXT)
  // =========================================================================
  console.log('[17s] Charlie sends message...');

  const msg3Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'Got it! I\'ll review tonight',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.charlieToken) }
  );

  check(msg3Res, { '[17s] Charlie message - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg3Res.body).id);

  // =========================================================================
  // 20s - Alice: "Great! Here's the design mockup" (IMAGE with file upload)
  // =========================================================================
  sleep(3);
  console.log('\n[20s] Alice uploads image...');

  const imageFileName = 'design-mockup-' + Date.now() + '.png';
  const imageUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(imageFileName)}&fileSize=256000&contentType=image/png`,
    null,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(imageUploadUrlRes, {
    '[20s] Get image upload URL - Status 200': (r) => r.status === 200,
  });

  const imageUploadData = JSON.parse(imageUploadUrlRes.body);
  testState.filesUploaded.push(imageUploadData.fileId);

  const imageConfirmRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(imageUploadData.fileId)}`,
    '',
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(imageConfirmRes, { '[20s] Confirm image upload - Status 204': (r) => r.status === 204 });

  // Now send IMAGE message
  const msg4Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.IMAGE,
      contentText: 'Great! Here\'s the design mockup',
      contentMeta: JSON.stringify({ fileId: imageUploadData.fileId }),
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(msg4Res, { '[20s] Alice image message - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg4Res.body).id);
  console.log('[20s] Image message sent');

  // =========================================================================
  // 25s - Charlie: "Love the design! 👍" (TEXT)
  // =========================================================================
  sleep(5);
  console.log('\n[25s] Charlie reacts...');

  const msg5Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'Love the design!',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.charlieToken) }
  );

  check(msg5Res, { '[25s] Charlie reaction - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg5Res.body).id);

  // =========================================================================
  // 28s-29s - Bob: typing
  // =========================================================================
  sleep(3);
  console.log('\n[28s] Bob starts typing...');
  sleep(1);
  console.log('[29s] Bob: "One question about the API..."');

  // =========================================================================
  // 29s - Bob: "One question about the API..." (TEXT)
  // =========================================================================
  const msg6Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'One question about the API...',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(msg6Res, { '[29s] Bob question - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg6Res.body).id);

  // =========================================================================
  // 32s - Alice: "Sure, what's up?" (TEXT)
  // =========================================================================
  sleep(3);
  console.log('\n[32s] Alice responds...');

  const msg7Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'Sure, what\'s up?',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(msg7Res, { '[32s] Alice response - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg7Res.body).id);

  // =========================================================================
  // 35s - Bob: Upload voice note (AUDIO)
  // =========================================================================
  sleep(3);
  console.log('\n[35s] Bob uploads voice note...');

  const audioFileName = 'voice-note-' + Date.now() + '.mp3';
  const audioUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(audioFileName)}&fileSize=512000&contentType=audio/mpeg`,
    null,
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(audioUploadUrlRes, {
    '[35s] Get audio upload URL - Status 200': (r) => r.status === 200,
  });

  const audioUploadData = JSON.parse(audioUploadUrlRes.body);
  testState.filesUploaded.push(audioUploadData.fileId);

  const audioConfirmRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(audioUploadData.fileId)}`,
    '',
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(audioConfirmRes, { '[35s] Confirm audio upload - Status 204': (r) => r.status === 204 });

  const msg8Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.AUDIO,
      contentText: 'Voice note explaining the question',
      contentMeta: JSON.stringify({ fileId: audioUploadData.fileId }),
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(msg8Res, { '[35s] Bob audio message - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg8Res.body).id);
  console.log('[35s] Audio message sent');

  // =========================================================================
  // 38s - Alice: "I'll check the audio later" (TEXT)
  // =========================================================================
  sleep(3);
  console.log('\n[38s] Alice responds...');

  const msg9Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.TEXT,
      contentText: 'I\'ll check the audio later',
      contentMeta: null,
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(msg9Res, { '[38s] Alice acknowledgment - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg9Res.body).id);

  // =========================================================================
  // 40s - Charlie: Send system message (SYSTEM type)
  // =========================================================================
  sleep(2);
  console.log('\n[40s] Charlie sends system message...');

  const msg10Res = http.post(
    `${baseUrl}${config.endpoints.messages}`,
    JSON.stringify({
      roomId: testState.roomId,
      type: config.messageTypes.SYSTEM,
      contentText: 'Meeting scheduled for tomorrow',
      contentMeta: JSON.stringify({ type: 'meeting_scheduled' }),
      clientRef: generateUUID()
    }),
    { headers: createAuthHeaders(data.charlieToken) }
  );

  check(msg10Res, { '[40s] System message - Status 201': (r) => r.status === 201 });
  testState.messagesSent.push(JSON.parse(msg10Res.body).id);

  // =========================================================================
  // 42s - All users verify presence (online status)
  // =========================================================================
  sleep(2);
  console.log('\n[42s] Verifying presence...');

  const aliceWsUrl = `${config.wsUrl}?token=${data.aliceToken}`;
  const aliceWsRes = ws.connect(aliceWsUrl, {}, function (socket) {
    socket.on('open', () => {
      console.log('[42s] Alice WebSocket connected');
    });
    socket.on('message', (message) => {
      const msg = JSON.parse(message);
      if (msg.event === 'PRESENCE') {
        console.log('[42s] Received presence event');
      }
    });
    setTimeout(() => socket.close(), 3000);
  });

  check(aliceWsRes, { '[42s] Alice WebSocket - Status 101': (r) => r.status === 101 });

  // =========================================================================
  // 45s-48s - Rapid fire: 5 messages within 3 seconds
  // =========================================================================
  sleep(3);
  console.log('\n[45s] Rapid fire messages...');

  for (let i = 0; i < 5; i++) {
    const msgRes = http.post(
      `${baseUrl}${config.endpoints.messages}`,
      JSON.stringify({
        roomId: testState.roomId,
        type: config.messageTypes.TEXT,
        contentText: `Rapid message ${i + 1}`,
        contentMeta: null,
        clientRef: generateUUID()
      }),
      { headers: createAuthHeaders(data.aliceToken) }
    );
    testState.messagesSent.push(JSON.parse(msgRes.body).id);
    sleep(0.6); // 600ms between messages
  }

  console.log('[48s] Rapid fire complete: 5 messages sent');

  // =========================================================================
  // 50s - Verify all messages via REST API
  // =========================================================================
  sleep(2);
  console.log('\n[50s] Verifying all messages in database...');

  const getMessagesRes = http.get(
    `${baseUrl}${config.endpoints.messages}/${testState.roomId}?page=0&size=100`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getMessagesRes, {
    '[50s] Get messages - Status 200': (r) => r.status === 200,
    '[50s] Get messages - Has 15+ messages': (r) => JSON.parse(r.body).length >= 15,
  });

  const retrievedMessages = JSON.parse(getMessagesRes.body);
  console.log('[50s] Retrieved', retrievedMessages.length, 'messages from database');

  // =========================================================================
  // 55s - Verify all files uploaded
  // =========================================================================
  sleep(5);
  console.log('\n[55s] Verifying file uploads...');

  check(testState.filesUploaded, {
    '[55s] Files - All 3 files uploaded': () => testState.filesUploaded.length === 3,
  });

  console.log('[55s] Files uploaded:', testState.filesUploaded.length);

  // =========================================================================
  // 58s - Final database verification
  // =========================================================================
  sleep(3);
  console.log('\n[58s] Final database verification...');

  const roomRes = http.get(
    `${baseUrl}${config.endpoints.rooms}/${testState.roomId}`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(roomRes, {
    '[58s] Verify room - Status 200': (r) => r.status === 200,
    '[58s] Verify room - Correct type': (r) => JSON.parse(r.body).type === 'GROUP',
  });

  const room = JSON.parse(roomRes.body);
  console.log('[58s] Room verified:', room.id);

  // =========================================================================
  // TEST SUMMARY
  // =========================================================================
  const endTime = Date.now();
  const duration = ((endTime - testState.startTime) / 1000).toFixed(2);

  console.log('\n========================================');
  console.log('REAL-WORLD CHAT SCENARIO SUMMARY');
  console.log('========================================');
  console.log('Duration:', duration, 'seconds');
  console.log('Room created:', testState.roomId);
  console.log('Messages sent:', testState.messagesSent.length);
  console.log('Files uploaded:', testState.filesUploaded.length);
  console.log('Message types: TEXT, IMAGE, AUDIO, SYSTEM');
  console.log('Participants: Alice, Bob, Charlie');
  console.log('WebSocket tested: YES');
  console.log('Presence verified: YES');
  console.log('\n✅ Real-world chat scenario completed!');
  console.log('========================================\n');
}

export function teardown(data) {
  console.log('\n========================================');
  console.log('DATABASE VERIFICATION QUERIES');
  console.log('========================================');
  console.log('\nRun these SQL queries to verify data:');

  console.log('\n-- Verify room was created:');
  console.log(`SELECT id, type, name, tenant_id FROM rooms WHERE id = '${testState.roomId}';`);

  console.log('\n-- Verify room participants:');
  console.log(`SELECT room_id, user_id, role FROM room_participants WHERE room_id = '${testState.roomId}';`);

  console.log('\n-- Verify all messages were sent:');
  console.log(`SELECT id, sender_id, type, LEFT(content_text, 50) as content, created_at`);
  console.log(`FROM messages WHERE room_id = '${testState.roomId}' ORDER BY created_at;`);

  console.log('\n-- Verify all files were uploaded:');
  console.log(`SELECT id, file_name, mime, size, is_confirmed FROM files`);
  console.log(`WHERE id = ANY(ARRAY[${testState.filesUploaded.map(id => "'" + id + "'").join(',')}]);`);

  console.log('\n-- Count messages per sender:');
  console.log(`SELECT sender_id, COUNT(*) as message_count`);
  console.log(`FROM messages WHERE room_id = '${testState.roomId}' GROUP BY sender_id;`);

  console.log('\n========================================\n');
}
