# Flutter Chat App PRD: NexusChat Client

**Document Version**: 2.0
**Target Developers**: Flutter Mobile/Web Team
**Backend**: NexusChat Engine (REST + WebSocket)
**Purpose**: Dummy testing application for chat engine validation

---

## 1. Project Overview

| Item | Description |
|------|-------------|
| **App Name** | NexusChat Client |
| **Target Platforms** | Mobile (iOS, Android) + Web |
| **Architecture** | BLoC (Business Logic Component) pattern |
| **State Management** | flutter_bloc |
| **Backend API** | NexusChat Engine |
| **Local Storage** | Isar (NoSQL) for offline caching |
| **Permission Handler** | permission_handler for Camera/Mic |

---

## 2. Feature Requirements (MVP)

### 2.1 Authentication

**Requirements:**
- Built-in login screen (email/password)
- JWT token storage (flutter_secure_storage)
- Auto-login on app start
- Logout functionality

**API Integration:**
```
POST /api/auth/login → { token, userId, name, tenantId }
GET /api/auth/verify → Verify token validity
```

### 2.2 Room Management

**Requirements:**
- List all rooms (group + direct)
- Create new group room
- Create direct chat (1-on-1)
- Room detail view
- Pull-to-refresh

**API Integration:**
```
GET /api/rooms?page=0&size=20
POST /api/rooms → { type, name, participantIds }
GET /api/rooms/{roomId}
```

### 2.3 Real-time Chat

**Requirements:**
- WebSocket connection for real-time updates
- Send/receive messages instantly
- Message bubbles (sent/received styling)
- Timestamp display
- Read receipts
- Message status indicators (sending, sent, failed)
- **Message Pagination** with infinite scroll
- **Local Persistence** for offline caching

**WebSocket Integration:**
```
Connect: ws://host/ws/chat?token=<JWT>
Send: { event: "SEND_MSG", traceId, data: { roomId, type, contentText, clientRef } }
Receive: { event: "NEW_MESSAGE", data: { id, senderId, senderName, contentText, createdAt } }
```

**Message Pagination (REST Fallback):**
```
GET /api/rooms/{roomId}/messages?page=0&size=50
Response: [{ id, roomId, senderId, type, contentText, contentMeta, createdAt, editedAt }]
```

**Local Persistence (Isar):**

```dart
// lib/core/models/message_model.dart
@collection
class MessageModel {
  Id id = Isar.autoIncrement;
  late String messageId;
  late String roomId;
  late String senderId;
  late MessageType type;
  String? contentText;
  String? contentMeta;
  late DateTime createdAt;
  DateTime? editedAt;
  MessageStatus status = MessageStatus.pending; // pending, sent, failed

  // Indexes for queries
  @Index()
  late String roomId;

  @Index()
  late DateTime createdAt;
}
```

**Pagination Strategy:**
1. Initial load: Fetch last 50 messages via REST API
2. Store in Isar for offline access
3. Load more on scroll: Increment `page` parameter
4. Optimistic UI: Show new messages immediately with `pending` status
5. Sync on reconnect: Compare local timestamps with server

**Offline Caching Flow:**
```
App Launch
    ↓
Load from Isar (show cached messages immediately)
    ↓
Fetch latest messages from API (page=0)
    ↓
Merge with cache (deduplicate by messageId)
    ↓
Persist merged results to Isar
```

### 2.4 Typing Indicators

**Requirements:**
- Show "X is typing..." in chat header
- Debounce typing events (max every 2s)
- Real-time typing status from other users

**WebSocket Events:**
```
Send: { event: "TYPING", data: { roomId, isTyping: true } }
Receive: { event: "TYPING", data: { userId, userName, isTyping: true } }
```

### 2.5 File Sharing

**Requirements:**
- Image picker (camera/gallery)
- Document picker
- Upload progress indicator
- File size validation
- Image preview in chat
- Download files

**API Flow:**
```
1. GET /api/files/upload-url?fileName=...&fileSize=...&contentType=...
   → { fileId, uploadUrl, objectKey }

2. PUT {uploadUrl} (direct to MinIO)

3. POST /api/files/{fileId}/confirm

4. POST /api/messages → { type: "FILE", contentMeta: { fileId } }
```

**Security & Validation:**

```dart
// lib/core/utils/file_validator.dart
class FileValidator {
  static const int maxFileSize = 10 * 1024 * 1024; // 10MB
  static const allowedImageExtensions = ['.jpg', '.jpeg', '.png'];
  static const allowedDocumentExtensions = ['.pdf', '.docx'];
  static const allowedVideoExtensions = ['.mp4'];

  static ValidationResult validateFile(File file) {
    // Check file size
    if (file.lengthSync() > maxFileSize) {
      return ValidationResult.error('File size exceeds 10MB limit');
    }

    // Check extension
    final extension = '.' + file.path.split('.').last.toLowerCase();
    final allAllowed = [
      ...allowedImageExtensions,
      ...allowedDocumentExtensions,
      ...allowedVideoExtensions,
    ];

    if (!allAllowed.contains(extension)) {
      return ValidationResult.error('File type not allowed');
    }

    return ValidationResult.success();
  }
}
```

**File Type Whitelist:**
| Category | Extensions |
|----------|------------|
| Images | `.jpg`, `.jpeg`, `.png` |
| Documents | `.pdf`, `.docx` |
| Videos | `.mp4` |

### 2.6 Voice/Video Call (WebRTC)

**Requirements:**
- Voice call
- Video call
- Mute/unmute microphone
- Camera on/off
- Speaker toggle
- Call duration timer
- End call
- **Permission Handling** (Camera/Microphone)

**Permission Handling (permission_handler):**

```dart
// lib/core/services/permission_service.dart
class PermissionService {
  Future<bool> requestCameraPermission() async {
    final status = await Permission.camera.request();
    return status.isGranted;
  }

  Future<bool> requestMicrophonePermission() async {
    final status = await Permission.microphone.request();
    return status.isGranted;
  }

  Future<bool> requestCallPermissions() async {
    final cameraStatus = await requestCameraPermission();
    final micStatus = await requestMicrophonePermission();
    return cameraStatus && micStatus;
  }

  Future<bool> hasCallPermissions() async {
    final camera = await Permission.camera.status;
    final mic = await Permission.microphone.status;
    return camera.isGranted && mic.isGranted;
  }

  void openAppSettings() {
    openAppSettings();
  }
}
```

**Call State Machine:**

```dart
// lib/core/models/call_state.dart
enum CallStatus {
  ringing,    // Incoming call received
  accepted,   // Call accepted by peer
  rejected,   // Call rejected by peer
  busy,       // Peer is in another call
  ended,      // Call ended
}

enum CallType {
  audioOnly,
  video,
}
```

**WebRTC Signaling Flow (Full):**

```
┌─────────────┐                    ┌─────────────┐
│   Caller    │                    │   Callee    │
└─────────────┘                    └─────────────┘
       │                                  │
       │ 1. Request Permissions           │
       │    (Camera/Mic)                  │
       ├──────────────────────────────────┤
       │                                  │
       │ 2. Create RTCPeerConnection       │
       │    (with iceServers)              │
       ├──────────────────────────────────┤
       │                                  │
       │ 3. Set local description (offer) │
       ├──────────────────────────────────┤
       │                                  │
       │ 4. SIGNAL_SDP (offer) ────────────┤
       │    WS: { event: "SIGNAL_SDP",    │
       │         data: { targetId,        │
       │                  type: "offer",  │
       │                  sdp: "..." } }  │
       │                                  │
       │                                  │ 5. Receive offer
       │                                  │    State: RINGING
       │                                  │    Show incoming call UI
       │                                  │
       │                                  │ 6. User accepts
       │                                  ├──────────────────────────┤
       │                                  │ 7. Create answer
       │                                  │    Set remote description
       │                                  │    Create answer
       │                                  │    Set local description
       │                                  ├──────────────────────────┤
       │                                  │ 8. SIGNAL_SDP (answer)
       │ 9. SIGNAL_SDP (answer) ◄──────────┤    WS: { event: "SIGNAL_SDP",
       │    Set remote description        │         data: { targetId,
       ├──────────────────────────────────┤                  type: "answer",
       │                                  │                  sdp: "..." } }
       │ 10. ICE Gathering Start          │
       ├──────────────────────────────────┤
       │                                  │ 11. ICE Gathering Start
       │ 12. For each ICE candidate:      │
       │    SIGNAL_ICE ────────────────────┤
       │    WS: { event: "SIGNAL_ICE",    │
       │         data: { targetId,        │
       │                  candidate,      │
       │                  sdpMid,         │
       │                  sdpMLineIndex } }│
       │                                  │
       │                                  │ 13. Receive ICE candidates
       │                                  │     Add to peer connection
       │                                  │
       │ 14. For each ICE candidate:      │
       │    SIGNAL_ICE ◄──────────────────┤
       │    Add to peer connection        │
       ├──────────────────────────────────┤
       │                                  │
       │ 15. ICE Connection Complete      │ 16. ICE Connection Complete
       │     State: ACCEPTED              │     State: ACCEPTED
       │     Start media streams         │     Start media streams
       │     Show call UI                 │     Show call UI
       │                                  │
       │ 17. Call End                     │ 18. Call End
       │     Close peer connection        │     Close peer connection
       │     State: ENDED                 │     State: ENDED
       │                                  │
```

**WebSocket Events:**

**SIGNAL_SDP (SDP Offer/Answer):**
```dart
// Send offer
final offer = await peerConnection.createOffer();
await peerConnection.setLocalDescription(offer);

socket.emit('SIGNAL_SDP', {
  'targetId': targetUserId,
  'type': 'offer',
  'sdp': offer.sdp,
});

// Receive offer/answer
socket.on('SIGNAL_SDP', (data) async {
  final sdp = RTCSessionDescription(data['sdp'], data['type']);
  await peerConnection.setRemoteDescription(sdp);

  if (data['type'] == 'offer') {
    final answer = await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(answer);

    socket.emit('SIGNAL_SDP', {
      'targetId': data['senderId'],
      'type': 'answer',
      'sdp': answer.sdp,
    });
  }
});
```

**SIGNAL_ICE (ICE Candidate Exchange):**
```dart
// Listen for ICE candidates
peerConnection.onIceCandidate = (candidate) {
  socket.emit('SIGNAL_ICE', {
    'targetId': targetUserId,
    'candidate': candidate.candidate,
    'sdpMid': candidate.sdpMid,
    'sdpMLineIndex': candidate.sdpMLineIndex,
  });
};

// Receive ICE candidates
socket.on('SIGNAL_ICE', (data) {
  final candidate = RTCIceCandidate(
    data['candidate'],
    data['sdpMid'],
    data['sdpMLineIndex'],
  );
  await peerConnection.addCandidate(candidate);
});
```

**CALL_REJECTED (Busy State):**
```dart
socket.on('CALL_REJECTED', (data) {
  final status = data['status']; // "busy"
  final reason = data['reason'];

  switch (status) {
    case 'busy':
      // Update call state to BUSY
      // Show "User is in another call" message
      // Clean up peer connection
      break;
    case 'rejected':
      // Call was manually rejected
      break;
  }
});
```

**Call State Transitions:**
```
IDLE → RINGING:    Incoming offer received
IDLE → ACCEPTED:   Outgoing call, ICE connected
RINGING → ACCEPTED: User accepted, ICE connected
RINGING → REJECTED: User rejected call
RINGING → BUSY:     Server rejected (peer busy)
ACCEPTED → ENDED:   Call ended by either party
REJECTED → IDLE:    Cleanup completed
BUSY → IDLE:        Cleanup completed
ENDED → IDLE:       Cleanup completed
```

### 2.7 Presence System

**Requirements:**
- Show online/offline status
- Update status in real-time

**WebSocket Events:**
```
Receive: { event: "PRESENCE", data: { userId, userName, status: "online" } }
```

---

## 3. UI/UX Requirements

### 3.1 Screens

| Screen | Description |
|--------|-------------|
| Splash | Loading screen, check auth |
| Login | Email/password form |
| Register | New user registration |
| Room List | List of all rooms |
| Create Room | Form to create group/direct room |
| Chat | Main chat interface |
| Call | Active call UI |

### 3.2 Widgets

| Widget | Description |
|--------|-------------|
| MessageBubble | Chat message bubble |
| TypingIndicator | Typing animation |
| FilePreview | File attachment preview |
| RoomListItem | Room list item |
| OnlineStatus | User status indicator |

---

## 4. Technical Requirements

### 4.1 Dependencies

```yaml
# State Management
flutter_bloc: ^8.1.0       # BLoC pattern
equatable: ^2.0.5           # Value equality

# Networking
http: ^1.1.0                # REST API
web_socket_channel: ^2.4.0  # WebSocket

# Storage
flutter_secure_storage: ^9.0.0  # Token storage (encrypted)
isar: ^3.1.0                # Local NoSQL database
isar_flutter_libs: ^3.1.0   # Required for Isar
path_provider: ^2.1.0       # File system paths

# File Handling
image_picker: ^1.0.0        # Image picker (camera/gallery)
file_picker: ^6.1.0         # Document picker
cached_network_image: ^3.3.0 # Image caching

# WebRTC
flutter_webrtc: ^0.9.0      # WebRTC implementation

# Permissions
permission_handler: ^11.0.0 # Camera/Microphone permissions

# Utilities
json_annotation: ^4.8.0     # JSON serialization
freezed_annotation: ^2.4.0  # Code generation (optional)
intl: ^0.18.0               # Date formatting
```

**Dev Dependencies:**
```yaml
build_runner: ^2.4.0        # Code generation
isar_generator: ^3.1.0      # Isar code generation
json_serializable: ^6.7.0   # JSON code gen
flutter_test:
  flutter:
    sdk: flutter
mockito: ^5.4.0            # Mocking for tests
```

### 4.2 Configuration

```dart
// lib/core/config/app_config.dart
class AppConfig {
  // Base URLs
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const String wsUrl = String.fromEnvironment(
    'WS_BASE_URL',
    defaultValue: 'ws://localhost:8080/ws/chat',
  );

  // WebRTC Configuration (fetched from API on app start)
  static List<IceServerConfig> iceServers = [
    // Fallback to Google STUN if API fetch fails
    IceServerConfig(
      urls: ['stun:stun1.l.google.com:19302'],
    ),
    IceServerConfig(
      urls: ['stun:stun2.l.google.com:19302'],
    ),
  ];

  // API Endpoints
  static const String webrtcConfigEndpoint = '/api/config/webrtc';
  static const String messagesEndpoint = '/api/rooms';

  // WebSocket Configuration
  static const Duration wsConnectionTimeout = Duration(seconds: 10);
  static const Duration wsPingInterval = Duration(seconds: 30);

  // Pagination
  static const int defaultPageSize = 50;
  static const int maxPageSize = 100;

  // File Upload
  static const int maxFileSize = 10 * 1024 * 1024; // 10MB

  // Reconnection
  static const int maxReconnectAttempts = 10;
  static const Duration initialReconnectDelay = Duration(seconds: 1);
  static const Duration maxReconnectDelay = Duration(seconds: 60);
}

// lib/core/models/ice_server_config.dart
class IceServerConfig {
  final List<String> urls;
  final String? username;
  final String? credential;

  const IceServerConfig({
    required this.urls,
    this.username,
    this.credential,
  });

  // Convert to flutter_webrtc format
  Map<String, dynamic> toMap() {
    final map = <String, dynamic>{
      'urls': urls,
    };
    if (username != null) map['username'] = username;
    if (credential != null) map['credential'] = credential;
    return map;
  }

  factory IceServerConfig.fromJson(Map<String, dynamic> json) {
    return IceServerConfig(
      urls: List<String>.from(json['urls'] as List),
      username: json['username'] as String?,
      credential: json['credential'] as String?,
    );
  }
}
```

**App Initialization Flow:**

```dart
// lib/main.dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Isar
  final dir = await getApplicationDocumentsDirectory();
  await Isar.open(
    [MessageModelSchema, RoomModelSchema],
    directory: dir.path,
  );

  // Fetch remote configuration
  await AppConfig.initialize();

  runApp(MyApp());
}

// lib/core/config/app_config.dart (extension)
abstract class AppConfig {
  static Future<void> initialize() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl$webrtcConfigEndpoint'),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final iceServersList = data['iceServers'] as List;
        iceServers = iceServersList
            .map((json) => IceServerConfig.fromJson(json as Map<String, dynamic>))
            .toList();
      }
    } catch (e) {
      // Use fallback iceServers if API fails
      debugPrint('Failed to fetch WebRTC config: $e');
    }
  }
}
```

### 4.3 WebSocket Auto-Reconnect

**Exponential Backoff Strategy:**

```dart
// lib/core/services/websocket_service.dart
class ChatWebSocketService {
  WebSocketChannel? _channel;
  Timer? _reconnectTimer;
  int _reconnectAttempts = 0;
  StreamSubscription? _streamSubscription;

  // Exponential backoff: delay = min(initial * 2^attempts, max)
  Duration _getReconnectDelay() {
    final exponential = AppConfig.initialReconnectDelay.inMilliseconds *
        pow(2, _reconnectAttempts).toInt();
    final capped = min(
      exponential,
      AppConfig.maxReconnectDelay.inMilliseconds,
    );
    return Duration(milliseconds: capped);
  }

  Future<void> connect(String token) async {
    await disconnect();

    final uri = Uri.parse('${AppConfig.wsUrl}?token=$token');
    _channel = WebSocketChannel.connect(uri);

    _streamSubscription = _channel!.stream.listen(
      _onMessage,
      onError: _onError,
      onDone: _onDone,
    );

    _reconnectAttempts = 0; // Reset on successful connect
  }

  void _onDone() {
    // Connection closed, attempt reconnect
    if (_reconnectAttempts < AppConfig.maxReconnectAttempts) {
      _reconnectAttempts++;
      final delay = _getReconnectDelay();

      debugPrint('WebSocket closed. Reconnecting in ${delay.inSeconds}s '
          '(attempt $_reconnectAttempts)');

      _reconnectTimer = Timer(delay, () => connect(_cachedToken));
    } else {
      debugPrint('Max reconnect attempts reached');
      // Notify UI of permanent disconnect
    }
  }

  void _onError(error) {
    debugPrint('WebSocket error: $error');
    // Will trigger onDone, which will attempt reconnect
  }

  Future<void> disconnect() async {
    _reconnectTimer?.cancel();
    await _streamSubscription?.cancel();
    await _channel?.sink.close();
    _channel = null;
  }

  // Manual reconnect (e.g., after network change)
  Future<void> reconnect() async {
    _reconnectAttempts = 0;
    await connect(_cachedToken);
  }
}
```

**Network State Awareness:**

```dart
// lib/core/services/connectivity_service.dart
class ConnectivityService {
  final Connectivity _connectivity = Connectivity();

  Stream<ConnectivityResult> get onConnectivityChanged =>
      _connectivity.onConnectivityChanged;

  Future<bool> get hasConnection async {
    final result = await _connectivity.checkConnectivity();
    return result != ConnectivityResult.none;
  }
}

// Integration with WebSocket
connectivityService.onConnectivityChanged.listen((result) {
  if (result != ConnectivityResult.none) {
    // Network restored, trigger immediate reconnect
    webSocketService.reconnect();
  }
});
```

---

## 5. Integration Examples

### 5.1 API Service

```dart
// lib/core/services/api_service.dart
class ChatApiService {
  final String _baseUrl;
  final String _token;

  ChatApiService({
    required String baseUrl,
    required String token,
  })  : _baseUrl = baseUrl,
        _token = token;

  Map<String, String> get _headers => {
        'Authorization': 'Bearer $_token',
        'Content-Type': 'application/json',
      };

  Future<List<Room>> getRooms({int page = 0, int size = 20}) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/api/rooms?page=$page&size=$size'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> json = jsonDecode(response.body);
      return json.map((e) => Room.fromJson(e)).toList();
    } else {
      throw ApiException('Failed to load rooms: ${response.statusCode}');
    }
  }

  Future<List<Message>> getRoomMessages(
    String roomId, {
    int page = 0,
    int size = 50,
  }) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/api/rooms/$roomId/messages?page=$page&size=$size'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> json = jsonDecode(response.body);
      return json.map((e) => Message.fromJson(e)).toList();
    } else {
      throw ApiException('Failed to load messages: ${response.statusCode}');
    }
  }

  Future<List<IceServerConfig>> getWebRtcConfig() async {
    final response = await http.get(
      Uri.parse('$_baseUrl/api/config/webrtc'),
      headers: _headers,
    );

    if (response.statusCode == 200) {
      final json = jsonDecode(response.body) as Map<String, dynamic>;
      final List<dynamic> servers = json['iceServers'] as List;
      return servers.map((e) => IceServerConfig.fromJson(e)).toList();
    } else {
      throw ApiException('Failed to load WebRTC config');
    }
  }
}
```

### 5.2 WebSocket Service

```dart
// lib/core/services/websocket_service.dart
class ChatWebSocketService {
  WebSocketChannel? _channel;
  final StreamController<ChatEvent> _eventController = StreamController.broadcast();
  String? _cachedToken;

  Stream<ChatEvent> get events => _eventController.stream;

  Future<void> connect(String token) async {
    _cachedToken = token;
    final uri = Uri.parse('${AppConfig.wsUrl}?token=$token');
    _channel = WebSocketChannel.connect(uri);

    _channel!.stream.listen(
      (data) {
        final json = jsonDecode(data as String) as Map<String, dynamic>;
        final event = ChatEvent.fromJson(json);
        _eventController.add(event);
      },
      onError: (error) => debugPrint('WebSocket error: $error'),
      onDone: () => debugPrint('WebSocket connection closed'),
    );
  }

  void sendEvent(String eventType, Map<String, dynamic> data) {
    final envelope = {
      'event': eventType,
      'traceId': const Uuid().v4(),
      'data': data,
    };

    _channel?.sink.add(jsonEncode(envelope));
  }

  void sendMessage({
    required String roomId,
    required MessageType type,
    String? contentText,
    String? contentMeta,
    String? clientRef,
  }) {
    sendEvent('SEND_MSG', {
      'roomId': roomId,
      'type': type.name,
      if (contentText != null) 'contentText': contentText,
      if (contentMeta != null) 'contentMeta': contentMeta,
      if (clientRef != null) 'clientRef': clientRef,
    });
  }

  void sendTyping({required String roomId, required bool isTyping}) {
    sendEvent('TYPING', {
      'roomId': roomId,
      'isTyping': isTyping,
    });
  }

  void sendSignalSdp({
    required String targetId,
    required String type,
    required String sdp,
  }) {
    sendEvent('SIGNAL_SDP', {
      'targetId': targetId,
      'type': type,
      'sdp': sdp,
    });
  }

  void sendSignalIce({
    required String targetId,
    required String candidate,
    String? sdpMid,
    required int sdpMLineIndex,
  }) {
    sendEvent('SIGNAL_ICE', {
      'targetId': targetId,
      'candidate': candidate,
      if (sdpMid != null) 'sdpMid': sdpMid,
      'sdpMLineIndex': sdpMLineIndex,
    });
  }

  Future<void> disconnect() async {
    await _channel?.sink.close();
    await _eventController.close();
  }
}

// lib/core/models/chat_event.dart
class ChatEvent {
  final String event;
  final String traceId;
  final Map<String, dynamic> data;

  ChatEvent({required this.event, required this.traceId, required this.data});

  factory ChatEvent.fromJson(Map<String, dynamic> json) {
    return ChatEvent(
      event: json['event'] as String,
      traceId: json['traceId'] as String,
      data: json['data'] as Map<String, dynamic>,
    );
  }
}
```

### 5.3 Local Persistence (Isar)

```dart
// lib/core/services/persistence_service.dart
class PersistenceService {
  late final Isar _isar;

  Future<void> initialize() async {
    final dir = await getApplicationDocumentsDirectory();
    _isar = await Isar.open(
      [MessageModelSchema, RoomModelSchema],
      directory: dir.path,
    );
  }

  // Messages
  Future<void> saveMessages(List<MessageModel> messages) async {
    await _isar.writeTxn(() async {
      for (final msg in messages) {
        await _isar.messageModels.putByMessageId(msg, onConflict: OnConflict.replace);
      }
    });
  }

  Future<List<MessageModel>> getMessages(
    String roomId, {
    int offset = 0,
    int limit = 50,
  }) async {
    return await _isar.messageModels
        .filter()
        .roomIdEqualTo(roomId)
        .sortByCreatedAtDesc()
        .offset(offset)
        .limit(limit)
        .findAll();
  }

  Future<void> saveMessage(MessageModel message) async {
    await _isar.writeTxn(() async {
      await _isar.messageModels.putByMessageId(message, onConflict: OnConflict.replace);
    });
  }

  Future<void> updateMessageStatus(String messageId, MessageStatus status) async {
    await _isar.writeTxn(() async {
      final msg = await _isar.messageModels.getByMessageId(messageId);
      if (msg != null) {
        msg.status = status;
        await _isar.messageModels.putByMessageId(msg);
      }
    });
  }
}
```

---

## 6. Testing Requirements

### 6.1 Unit Tests

- BLoC state transitions
- Service layer methods
- Utility functions (file validation, etc.)
- JSON serialization/deserialization

### 6.2 Widget Tests

- Screen rendering
- User interactions
- Form validation
- Navigation flows

### 6.3 Integration Tests

- API service with mock server
- WebSocket event handling
- Persistence operations

### 6.4 Golden Tests

- Message bubbles (sent/received)
- Room list items
- Call UI states

---

## 7. Deployment

| Platform | Build Command | Deployment Target |
|----------|---------------|-------------------|
| **Web** | `flutter build web` | Firebase Hosting / Vercel |
| **Android** | `flutter build apk` / `flutter build appbundle` | Google Play Store |
| **iOS** | `flutter build ios` | Apple App Store |

**Environment Configuration:**

```bash
# Development
flutter run --dart-define=API_BASE_URL=http://localhost:8080

# Production
flutter build apk --dart-define=API_BASE_URL=https://api.example.com
flutter build web --dart-define=API_BASE_URL=https://api.example.com
```

---

## 8. Deliverables

1. Source code (Flutter project)
2. Technical documentation (API integration guide)
3. Architecture diagrams
4. Deployment guide
5. Test report with coverage metrics
6. Demo video showing core features
