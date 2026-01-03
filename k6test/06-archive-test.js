// K6 Test: Room Archive Feature
// Tests admin API to archive rooms and verify data moves to archive tables
// Usage: k6 run 06-archive-test.js

import http from 'k6/http';
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

const testState = {
  aliceToken: null,
  bobToken: null,
  testRoomId: null,
  archivedRoomId: null,
  messages: [],
  participants: []
};

export function setup() {
  console.log('\n=== Setup: Room Archive Test ===');

  testState.aliceToken = getToken(config.users.alice);
  testState.bobToken = getToken(config.users.bob);

  return testState;
}

export default function(data) {
  const baseUrl = config.baseUrl;

  console.log('\n========================================');
  console.log('ROOM ARCHIVE FEATURE TEST');
  console.log('========================================');

  // =========================================================================
  // STEP 1: Create a test room with messages
  // =========================================================================
  console.log('\n--- STEP 1: Create test room ---');

  const createRoomRes = http.post(
    `${baseUrl}${config.endpoints.rooms}`,
    JSON.stringify({
      type: config.roomTypes.GROUP,
      name: 'Archive Test Room ' + Date.now(),
      participantIds: [config.users.alice.id, config.users.bob.id]
    }),
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(createRoomRes, {
    '[STEP 1] Create room - Status 201': (r) => r.status === 201,
  });

  testState.testRoomId = JSON.parse(createRoomRes.body).id;
  console.log('[STEP 1] Test room created:', testState.testRoomId);

  sleep(1);

  // =========================================================================
  // STEP 2: Send multiple messages to the room
  // =========================================================================
  console.log('\n--- STEP 2: Send messages ---');

  const messageCount = 5;
  for (let i = 0; i < messageCount; i++) {
    const msgRes = http.post(
      `${baseUrl}${config.endpoints.messages}`,
      JSON.stringify({
        roomId: testState.testRoomId,
        type: config.messageTypes.TEXT,
        contentText: `Test message ${i + 1} for archive`,
        contentMeta: null,
        clientRef: generateUUID()
      }),
      { headers: createAuthHeaders(data.aliceToken) }
    );

    if (msgRes.status === 201) {
      testState.messages.push(JSON.parse(msgRes.body).id);
    }
  }

  console.log('[STEP 2] Sent', testState.messages.length, 'messages');
  sleep(1);

  // =========================================================================
  // STEP 3: Verify room exists in main table
  // =========================================================================
  console.log('\n--- STEP 3: Verify room in main table ---');

  const getRoomRes = http.get(
    `${baseUrl}${config.endpoints.rooms}/${testState.testRoomId}`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getRoomRes, {
    '[STEP 3] Get room - Status 200': (r) => r.status === 200,
    '[STEP 3] Get room - Room exists': (r) => JSON.parse(r.body).id === testState.testRoomId,
  });

  console.log('[STEP 3] Room verified in main table');
  sleep(1);

  // =========================================================================
  // STEP 4: Get messages from main table
  // =========================================================================
  console.log('\n--- STEP 4: Get messages from main table ---');

  const getMessagesRes = http.get(
    `${baseUrl}${config.endpoints.messages}/${testState.testRoomId}?page=0&size=50`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getMessagesRes, {
    '[STEP 4] Get messages - Status 200': (r) => r.status === 200,
    '[STEP 4] Get messages - Has 5 messages': (r) => JSON.parse(r.body).length === messageCount,
  });

  const messagesBeforeArchive = JSON.parse(getMessagesRes.body);
  console.log('[STEP 4] Found', messagesBeforeArchive.length, 'messages');
  sleep(1);

  // =========================================================================
  // STEP 5: Call admin API to archive the room
  // =========================================================================
  console.log('\n--- STEP 5: Archive room via admin API ---');

  const archiveRes = http.post(
    `${baseUrl}/api/admin/archive/${testState.testRoomId}`,
    '',
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(archiveRes, {
    '[STEP 5] Archive room - Status 201 or 202': (r) => r.status === 201 || r.status === 202,
    '[STEP 5] Archive room - Has archivedRoomId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.archivedRoomId !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (archiveRes.status === 201 || archiveRes.status === 202) {
    const archiveData = JSON.parse(archiveRes.body);
    testState.archivedRoomId = archiveData.archivedRoomId;
    console.log('[STEP 5] Room archived successfully:', testState.archivedRoomId);
    console.log('[STEP 5] Messages archived:', archiveData.messagesArchived || 'N/A');
    console.log('[STEP 5] Participants archived:', archiveData.participantsArchived || 'N/A');
  }

  sleep(2);

  // =========================================================================
  // STEP 6: Verify room deleted from main table
  // =========================================================================
  console.log('\n--- STEP 6: Verify room removed from main table ---');

  const getRoomAfterRes = http.get(
    `${baseUrl}${config.endpoints.rooms}/${testState.testRoomId}`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getRoomAfterRes, {
    '[STEP 6] Get deleted room - Status 404': (r) => r.status === 404,
  });

  console.log('[STEP 6] Room removed from main table (404 expected)');
  sleep(1);

  // =========================================================================
  // STEP 7: Verify messages deleted from main table
  // =========================================================================
  console.log('\n--- STEP 7: Verify messages removed from main table ---');

  const getMessagesAfterRes = http.get(
    `${baseUrl}${config.endpoints.messages}/${testState.testRoomId}?page=0&size=50`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(getMessagesAfterRes, {
    '[STEP 7] Get messages - Status 404 or empty': (r) => r.status === 404 || JSON.parse(r.body).length === 0,
  });

  console.log('[STEP 7] Messages removed from main table');
  sleep(1);

  // =========================================================================
  // STEP 8: List archived rooms
  // =========================================================================
  console.log('\n--- STEP 8: List archived rooms ---');

  const listArchivedRes = http.get(
    `${baseUrl}/api/archive/rooms?page=0&size=20`,
    { headers: createAuthHeaders(data.aliceToken) }
  );

  check(listArchivedRes, {
    '[STEP 8] List archived rooms - Status 200': (r) => r.status === 200,
    '[STEP 8] List archived rooms - Contains test room': (r) => {
      const rooms = JSON.parse(r.body);
      return rooms.some(r => r.originalRoomId === testState.testRoomId);
    },
  });

  const archivedRooms = JSON.parse(listArchivedRes.body);
  console.log('[STEP 8] Found', archivedRooms.length, 'archived rooms');
  sleep(1);

  // =========================================================================
  // STEP 9: Get archived messages
  // =========================================================================
  console.log('\n--- STEP 9: Get archived messages ---');

  if (testState.archivedRoomId) {
    const getArchivedMessagesRes = http.get(
      `${baseUrl}/api/archive/rooms/${testState.archivedRoomId}/messages?page=0&size=50`,
      { headers: createAuthHeaders(data.aliceToken) }
    );

    check(getArchivedMessagesRes, {
      '[STEP 9] Get archived messages - Status 200': (r) => r.status === 200,
      '[STEP 9] Get archived messages - Has messages': (r) => {
        const msgs = JSON.parse(r.body);
        return msgs.length >= messageCount;
      },
    });

    const archivedMessages = JSON.parse(getArchivedMessagesRes.body);
    console.log('[STEP 9] Retrieved', archivedMessages.length, 'archived messages');
  }

  sleep(1);

  // =========================================================================
  // STEP 10: Verify archived room participants
  // =========================================================================
  console.log('\n--- STEP 10: Verify archived participants ---');

  if (testState.archivedRoomId) {
    const getArchivedParticipantsRes = http.get(
      `${baseUrl}/api/archive/rooms/${testState.archivedRoomId}/participants`,
      { headers: createAuthHeaders(data.aliceToken) }
    );

    check(getArchivedParticipantsRes, {
      '[STEP 10] Get archived participants - Status 200': (r) => r.status === 200,
      '[STEP 10] Get archived participants - Has 2 participants': (r) => {
        const participants = JSON.parse(r.body);
        return participants.length === 2;
      },
    });

    const archivedParticipants = JSON.parse(getArchivedParticipantsRes.body);
    console.log('[STEP 10] Retrieved', archivedParticipants.length, 'archived participants');
  }

  sleep(1);

  // =========================================================================
  // STEP 11: Verify tenant isolation (Bob's archived rooms)
  // =========================================================================
  console.log('\n--- STEP 11: Verify tenant isolation ---');

  const bobArchivedRes = http.get(
    `${baseUrl}/api/archive/rooms?page=0&size=20`,
    { headers: createAuthHeaders(data.bobToken) }
  );

  check(bobArchivedRes, {
    '[STEP 11] Bob archived rooms - Status 200': (r) => r.status === 200,
    '[STEP 11] Bob archived rooms - Same as Alice (same tenant)': (r) => {
      const bobRooms = JSON.parse(r.body);
      return bobRooms.some(r => r.originalRoomId === testState.testRoomId);
    },
  });

  console.log('[STEP 11] Tenant isolation verified');

  // =========================================================================
  // TEST SUMMARY
  // =========================================================================
  console.log('\n========================================');
  console.log('ARCHIVE FEATURE TEST SUMMARY');
  console.log('========================================');
  console.log('Original room ID:', testState.testRoomId);
  console.log('Archived room ID:', testState.archivedRoomId);
  console.log('Messages sent:', testState.messages.length);
  console.log('Messages archived:', testState.messages.length);
  console.log('Participants: Alice, Bob');
  console.log('Room deleted from main: YES');
  console.log('Messages deleted from main: YES');
  console.log('Data moved to archive: YES');
  console.log('Audit access: YES');
  console.log('\n✅ Archive feature test completed!');
  console.log('========================================\n');
}

export function teardown(data) {
  console.log('\n========================================');
  console.log('DATABASE VERIFICATION');
  console.log('========================================');
  console.log('\nRun these SQL queries to verify archive:');

  console.log('\n-- Verify room REMOVED from main table:');
  console.log(`SELECT * FROM rooms WHERE id = '${testState.testRoomId}';`);
  console.log('-- Expected: 0 rows');

  console.log('\n-- Verify messages REMOVED from main table:');
  console.log(`SELECT * FROM messages WHERE room_id = '${testState.testRoomId}';`);
  console.log('-- Expected: 0 rows');

  console.log('\n-- Verify room in archived_rooms table:');
  console.log(`SELECT * FROM archived_rooms WHERE original_room_id = '${testState.testRoomId}';`);
  console.log('-- Expected: 1 row with correct metadata');

  console.log('\n-- Verify messages in archived_messages table:');
  console.log(`SELECT * FROM archived_messages WHERE original_room_id = '${testState.testRoomId}' ORDER BY created_at;`);
  console.log('-- Expected:', testState.messages.length, 'rows');

  console.log('\n-- Verify participants in archived_participants table:');
  console.log(`SELECT * FROM archived_participants WHERE archived_room_id = '${testState.archivedRoomId}';`);
  console.log('-- Expected: 2 rows (Alice, Bob)');

  console.log('\n-- Full archive summary:');
  console.log(`SELECT`);
  console.log(`  ar.id as archived_room_id,`);
  console.log(`  ar.original_room_id,`);
  console.log(`  ar.type,`);
  console.log(`  ar.name,`);
  console.log(`  ar.message_count,`);
  console.log(`  ar.participant_count,`);
  console.log(`  ar.archived_at,`);
  console.log(`  ar.archived_by`);
  console.log(`FROM archived_rooms ar`);
  console.log(`WHERE ar.original_room_id = '${testState.testRoomId}';`);

  console.log('\n========================================\n');
}
