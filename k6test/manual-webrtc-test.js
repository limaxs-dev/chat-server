// Manual WebRTC Signaling Test
// This script simulates two WebSocket clients to test WebRTC signaling end-to-end
// Usage: node manual-webrtc-test.js

const WebSocket = require('ws');
const jwt = require('jsonwebtoken');
const fs = require('fs');

// Read private key for signing JWTs
const privateKey = fs.readFileSync('src/main/resources/keys/private-key.pem', 'utf8');

// User IDs from test data
const ALICE_ID = '550e8400-e29b-41d4-a716-446655440000';
const BOB_ID = '650e8400-e29b-41d4-a716-446655440000';
const TENANT_ID = 'tenant-001';

// Generate JWT token for a user
function generateToken(userId, tenantId, name) {
  return jwt.sign(
    {
      sub: userId,
      iss: tenantId,
      tenantId: tenantId,
      name: name
    },
    privateKey,
    { algorithm: 'RS256', expiresIn: '1h' }
  );
}

// Connect a WebSocket client
function connectClient(userId, tenantId, name) {
  const token = generateToken(userId, tenantId, name);
  const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

  ws.on('open', () => {
    console.log(`${name}: Connected`);
  });

  ws.on('message', (data) => {
    const msg = JSON.parse(data);
    console.log(`${name}: Received ${msg.event}`, msg.data ? `- target: ${msg.data.targetId || msg.data.userId || 'N/A'}` : '');
  });

  ws.on('error', (error) => {
    console.error(`${name}: Error`, error.message);
  });

  ws.on('close', () => {
    console.log(`${name}: Disconnected`);
  });

  return ws;
}

// Run the test
async function runTest() {
  console.log('=== Manual WebRTC Signaling Test ===\n');

  // Connect both clients
  const alice = connectClient(ALICE_ID, TENANT_ID, 'Alice');
  const bob = connectClient(BOB_ID, TENANT_ID, 'Bob');

  // Wait for connections to establish
  await new Promise(r => setTimeout(r, 2000));

  console.log('\n--- Test 1: Alice sends SDP Offer to Bob ---');
  alice.send(JSON.stringify({
    event: 'SIGNAL_SDP',
    traceId: crypto.randomUUID(),
    data: {
      targetId: BOB_ID,
      type: 'offer',
      sdp: 'v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:abc123\r\na=ice-pwd:def456\r\na=fingerprint:sha-256 AA:BB:CC:DD\r\na=setup:actpass\r\na=mid:audio\r\na=sendonly\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\n'
    }
  }));

  // Wait for Bob to receive and respond
  await new Promise(r => setTimeout(r, 2000));

  console.log('\n--- Test 2: Bob sends SDP Answer to Alice ---');
  bob.send(JSON.stringify({
    event: 'SIGNAL_SDP',
    traceId: crypto.randomUUID(),
    data: {
      targetId: ALICE_ID,
      type: 'answer',
      sdp: 'v=0\r\no=- 987654321 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\nc=IN IP4 0.0.0.0\r\na=rtcp:9 IN IP4 0.0.0.0\r\na=ice-ufrag:xyz789\r\na=ice-pwd:uvw012\r\na=fingerprint:sha-256 11:22:33:44\r\na=setup:active\r\na=mid:audio\r\na=recvonly\r\na=rtcp-mux\r\na=rtpmap:111 opus/48000/2\r\n'
    }
  }));

  // Wait for Alice to receive
  await new Promise(r => setTimeout(r, 2000));

  console.log('\n--- Test 3: Bob sends ICE candidates to Alice ---');
  bob.send(JSON.stringify({
    event: 'SIGNAL_ICE',
    traceId: crypto.randomUUID(),
    data: {
      targetId: ALICE_ID,
      candidate: 'candidate:1 1 UDP 2130706431 192.168.1.100 54400 typ host',
      sdpMid: 'audio',
      sdpMLineIndex: 0
    }
  }));

  // Wait for Alice to receive
  await new Promise(r => setTimeout(r, 2000));

  console.log('\n--- Test Complete ---');

  // Close connections
  alice.close();
  bob.close();

  // Wait for clean disconnect
  await new Promise(r => setTimeout(r, 1000));

  console.log('\nExpected results:');
  console.log('- Alice should receive: PRESENCE (online), SIGNAL_SDP (answer from Bob), SIGNAL_ICE');
  console.log('- Bob should receive: PRESENCE (online), SIGNAL_SDP (offer from Alice)');
  console.log('- Check Redis: signal:user:{aliceId} and signal:user:{bobId} channels');
  console.log('- Check Redis: user:call:{aliceId} should be "busy" (TTL 300s)');
}

runTest().catch(console.error);
