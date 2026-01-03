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
  const wsUrl = data.wsUrl;
  const aliceToken = data.aliceToken;

  // ========================================================================
  // Test 1: WebSocket Connection
  // ========================================================================
  console.log('\n=== Test 1: WebSocket Connection ===');

  let connectionSuccessful = false;
  let connectionError = null;

  const res = ws.connect(wsUrl, {}, function (socket) {
    connectionSuccessful = true;
    console.log('WebSocket connected successfully!');

    // ========================================================================
    // Test 2: Handle OPEN event
    // ========================================================================
    socket.on('open', () => {
      console.log('WebSocket connection opened');

      // Send a test message
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
      console.log('Sent SEND_MSG event:', JSON.stringify(sendMessage));
    });

    // ========================================================================
    // Test 3: Handle MESSAGE event (NEW_MESSAGE)
    // ========================================================================
    socket.on('message', (message) => {
      const msg = JSON.parse(message);
      console.log('Received message:', JSON.stringify(msg, null, 2));

      // Store received messages
      receivedMessages.push(msg);

      // Check if it's a NEW_MESSAGE event
      if (msg.event === config.events.NEW_MESSAGE) {
        console.log('Received NEW_MESSAGE event');
        check(msg, {
          'NEW_MESSAGE - Has event': (m) => m.event === config.events.NEW_MESSAGE,
          'NEW_MESSAGE - Has traceId': (m) => m.traceId !== undefined,
          'NEW_MESSAGE - Has data': (m) => m.data !== undefined,
          'NEW_MESSAGE - Has senderId': (m) => m.data.senderId !== undefined,
          'NEW_MESSAGE - Has contentText': (m) => m.data.contentText !== undefined,
          'NEW_MESSAGE - Has roomId': (m) => m.data.roomId !== undefined,
        });
      }

      // Check for PRESENCE event
      if (msg.event === config.events.PRESENCE) {
        console.log('Received PRESENCE event');
        check(msg, {
          'PRESENCE - Has event': (m) => m.event === config.events.PRESENCE,
          'PRESENCE - Has userId': (m) => m.data.userId !== undefined,
          'PRESENCE - Has status': (m) => m.data.status !== undefined,
        });
      }

      // Check for ACK response
      if (msg.event === config.events.ACK) {
        console.log('Received ACK event');
        check(msg, {
          'ACK - Has event': (m) => m.event === config.events.ACK,
          'ACK - Has traceId': (m) => m.traceId !== undefined,
        });
      }
    });

    // ========================================================================
    // Test 4: Send TYPING indicator
    // ========================================================================
    setTimeout(() => {
      const typingMessage = {
        event: config.events.TYPING,
        traceId: generateUUID(),
        data: {
          roomId: config.rooms.engineering,
          isTyping: true
        }
      };

      socket.send(JSON.stringify(typingMessage));
      console.log('Sent TYPING event:', JSON.stringify(typingMessage));

      check(typingMessage, {
        'TYPING - Has event': (m) => m.event === config.events.TYPING,
        'TYPING - Has roomId': (m) => m.data.roomId !== undefined,
        'TYPING - Has isTyping': (m) => m.data.isTyping === true,
      });

      // Send typing stop after 2 seconds
      setTimeout(() => {
        const stopTypingMessage = {
          event: config.events.TYPING,
          traceId: generateUUID(),
          data: {
            roomId: config.rooms.engineering,
            isTyping: false
          }
        };

        socket.send(JSON.stringify(stopTypingMessage));
        console.log('Sent TYPING (stop) event');
      }, 2000);
    }, 1000);

    // ========================================================================
    // Test 5: Send WebRTC Signal
    // ========================================================================
    setTimeout(() => {
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
      console.log('Sent SIGNAL_SDP event');

      check(signalMessage, {
        'SIGNAL_SDP - Has event': (m) => m.event === config.events.SIGNAL_SDP,
        'SIGNAL_SDP - Has targetId': (m) => m.data.targetId === config.users.bob.id,
        'SIGNAL_SDP - Has type': (m) => m.data.type === 'offer',
        'SIGNAL_SDP - Has sdp': (m) => m.data.sdp !== undefined && m.data.sdp.length > 0,
      });
    }, 3000);

    // ========================================================================
    // Test 6: Send multiple messages rapidly
    // ========================================================================
    setTimeout(() => {
      console.log('Sending multiple messages rapidly...');

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

      console.log('Sent 5 rapid messages');
    }, 5000);

    // ========================================================================
    // Test 7: Send ACK for message
    // ========================================================================
    setTimeout(() => {
      if (receivedMessages.length > 0) {
        const lastMessage = receivedMessages[receivedMessages.length - 1];
        if (lastMessage.event === config.events.NEW_MESSAGE && lastMessage.data.id) {
          const ackMessage = {
            event: config.events.ACK,
            traceId: generateUUID(),
            data: {
              messageId: lastMessage.data.id
            }
          };

          socket.send(JSON.stringify(ackMessage));
          console.log('Sent ACK for message:', lastMessage.data.id);
        }
      }
    }, 7000);

    // ========================================================================
    // Test 8: Close connection after tests
    // ========================================================================
    setTimeout(() => {
      console.log('Closing WebSocket connection...');
      socket.close();
    }, 10000);
  });

  // ========================================================================
  // Test 9: Verify Connection Success
  // ========================================================================
  check(res, {
    'WebSocket - Connection successful': () => connectionSuccessful,
    'WebSocket - Status is 101': (r) => r.status === 101,
  });

  if (!connectionSuccessful) {
    console.log('WebSocket connection failed!');
    console.log('Response:', JSON.stringify(res, null, 2));
  }

  // Wait for tests to complete
  sleep(12);

  // ========================================================================
  // Test 10: Verify Received Messages
  // ========================================================================
  console.log('\n=== Test 10: Verify Received Messages ===');
  console.log('Total messages received:', receivedMessages.length);

  check(receivedMessages, {
    'WebSocket - Received at least 1 message': () => receivedMessages.length > 0,
    'WebSocket - Received NEW_MESSAGE event': () => {
      return receivedMessages.some(m => m.event === config.events.NEW_MESSAGE);
    },
    'WebSocket - Received PRESENCE event': () => {
      return receivedMessages.some(m => m.event === config.events.PRESENCE);
    },
  });

  // Print summary
  console.log('\n=== WebSocket Test Summary ===');
  console.log('Connection successful:', connectionSuccessful);
  console.log('Messages received:', receivedMessages.length);
  console.log('Event types received:', [...new Set(receivedMessages.map(m => m.event))]);
}

export function teardown(data) {
  console.log('\n=== Test Teardown ===');
  console.log('WebSocket tests completed!');
  console.log('\nVerify messages in database:');
  console.log(`SELECT * FROM messages WHERE room_id = '${config.rooms.engineering}' ORDER BY created_at DESC LIMIT 10;`);
}
