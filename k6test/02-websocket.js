// K6 Test: WebSocket Integration Test
// Tests WebSocket connectivity and events for NexusChat Engine
// Usage: k6 run 02-websocket.js

import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { config, generateUUID, getToken } from './config.js';

// Test configuration
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

let aliceToken;
let bobToken;
let wsUrl;

export function setup() {
  // Get tokens for test users
  aliceToken = getToken(config.users.alice);
  bobToken = getToken(config.users.bob);

  // Construct WebSocket URL with token
  wsUrl = `${config.wsUrl}?token=${aliceToken}`;

  console.log('Alice token:', aliceToken.substring(0, 20) + '...');
  console.log('WebSocket URL:', wsUrl.replace(aliceToken, '<token>'));

  return {
    aliceToken,
    bobToken,
    wsUrl
  };
}

// Store received messages for verification
const receivedMessages = [];

export default function(data) {
  // Clear messages for clean test run
  receivedMessages.length = 0;

  const wsUrl = data.wsUrl;
  let wsClosed = false;

  // ========================================================================
  // WebSocket Connection Test
  // ========================================================================
  console.log('\n=== WebSocket Integration Test ===');

  const res = ws.connect(wsUrl, {}, function (socket) {
    console.log('WebSocket connected successfully!');

    // ========================================================================
    // Event: Connection opened - Send all test messages
    // ========================================================================
    socket.on('open', () => {
      console.log('[OPEN] WebSocket connection opened');

      // Test 1: Send initial text message
      const sendMessage = {
        event: config.events.SEND_MSG,
        traceId: generateUUID(),
        data: {
          roomId: config.rooms.engineering,
          type: config.messageTypes.TEXT,
          contentText: 'Hello from K6 WebSocket test! ' + Date.now(),
          contentMeta: null,
          clientRef: generateUUID()
        }
      };
      socket.send(JSON.stringify(sendMessage));
      console.log('[SENT] SEND_MSG event');

      // Test 2: Send TYPING indicator (immediately)
      const typingMessage = {
        event: config.events.TYPING,
        traceId: generateUUID(),
        data: {
          roomId: config.rooms.engineering,
          isTyping: true
        }
      };
      socket.send(JSON.stringify(typingMessage));
      console.log('[SENT] TYPING event (start)');

      // Test 3: Send WebRTC Signal
      const signalMessage = {
        event: config.events.SIGNAL_SDP,
        traceId: generateUUID(),
        data: {
          targetId: config.users.bob.id,
          type: 'offer',
          sdp: 'v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:abc123\r\na=ice-pwd:def456\r\na=fingerprint:sha-256 AA:BB:CC:DD\r\na=setup:actpass\r\na=mid:audio\r\na=sendonly\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\n'
        }
      };
      socket.send(JSON.stringify(signalMessage));
      console.log('[SENT] SIGNAL_SDP event');

      // Test 4: Send multiple messages rapidly (immediately)
      console.log('[SENT] Sending 5 rapid messages...');
      for (let i = 0; i < 5; i++) {
        const rapidMessage = {
          event: config.events.SEND_MSG,
          traceId: generateUUID(),
          data: {
            roomId: config.rooms.engineering,
            type: config.messageTypes.TEXT,
            contentText: `Rapid message ${i + 1} - ${Date.now()}`,
            contentMeta: null,
            clientRef: generateUUID()
          }
        };
        socket.send(JSON.stringify(rapidMessage));
      }
      console.log('[SENT] 5 rapid messages sent');
    });

    // ========================================================================
    // Event: Receive messages
    // ========================================================================
    socket.on('message', (message) => {
      const msg = JSON.parse(message);
      console.log('[RECEIVED]', msg.event);

      // Store received messages
      receivedMessages.push(msg);

      // Validate PRESENCE event
      if (msg.event === 'PRESENCE') {
        check(msg, {
          'PRESENCE - Has event': (m) => m.event === 'PRESENCE',
          'PRESENCE - Has traceId': (m) => m.traceId !== undefined,
          'PRESENCE - Has data': (m) => m.data !== undefined,
          'PRESENCE - Has userId': (m) => m.data.userId !== undefined,
          'PRESENCE - Has userName': (m) => m.data.userName !== undefined,
          'PRESENCE - Has status': (m) => m.data.status !== undefined,
          'PRESENCE - Has roomId': (m) => m.data.roomId !== undefined,
        });
        console.log('  User:', msg.data.userName, 'is now', msg.data.status, 'in room', msg.data.roomId);
      }

      // Validate NEW_MESSAGE event
      if (msg.event === config.events.NEW_MESSAGE) {
        check(msg, {
          'NEW_MESSAGE - Has event': (m) => m.event === config.events.NEW_MESSAGE,
          'NEW_MESSAGE - Has traceId': (m) => m.traceId !== undefined,
          'NEW_MESSAGE - Has data': (m) => m.data !== undefined,
          'NEW_MESSAGE - Has senderId': (m) => m.data.senderId !== undefined,
          'NEW_MESSAGE - Has contentText': (m) => m.data.contentText !== undefined,
          'NEW_MESSAGE - Has roomId': (m) => m.data.roomId !== undefined,
        });
      }

      // Close after receiving at least 5 messages
      if (receivedMessages.length >= 5 && !wsClosed) {
        wsClosed = true;
        console.log('[CLOSE] Closing connection after receiving', receivedMessages.length, 'messages');
        socket.close(1000, 'Test complete');
      }
    });

    // ========================================================================
    // Event: Connection closed
    // ========================================================================
    socket.on('close', () => {
      console.log('[CLOSED] WebSocket connection closed successfully');
    });

    // ========================================================================
    // Event: Error
    // ========================================================================
    socket.on('error', (error) => {
      console.log('[ERROR] WebSocket error:', error);
    });
  });

  // ========================================================================
  // Verify Connection Success
  // ========================================================================
  check(res, {
    'WebSocket - Connection successful': (r) => r.status === 101,
    'WebSocket - Status is 101': (r) => r.status === 101,
  });

  // Wait for async operations to complete
  sleep(5);

  // ========================================================================
  // Verify Received Messages
  // ========================================================================
  console.log('\n=== Verification ===');
  console.log('Total messages received:', receivedMessages.length);

  check(receivedMessages, {
    'WebSocket - Received at least 1 message': () => receivedMessages.length > 0,
    'WebSocket - Received NEW_MESSAGE event': () => {
      return receivedMessages.some(m => m.event === config.events.NEW_MESSAGE);
    },
    'WebSocket - Received PRESENCE event': () => {
      return receivedMessages.some(m => m.event === 'PRESENCE');
    },
  });

  console.log('Event types received:', [...new Set(receivedMessages.map(m => m.event))]);
  console.log('WebSocket closed:', wsClosed);
  console.log('\n✅ WebSocket Test Complete!');
}

export function teardown(data) {
  console.log('\n=== Test Teardown ===');
  console.log('Verify messages in database:');
  console.log(`SELECT * FROM messages WHERE room_id = '${config.rooms.engineering}' ORDER BY created_at DESC LIMIT 10;`);
}
