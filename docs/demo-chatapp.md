# NexusChat Demo App - Complete Implementation Guide

> Complete documentation for building a demo chat application that demonstrates all NexusChat Engine features

**Version**: 1.0
**Date**: 2026-01-04
**Backend**: NexusChat Engine (Quarkus + Java 21)
**Demo App**: Flutter (Web + Mobile)

---

## Table of Contents

1. [Overview](#1-overview)
2. [App Architecture](#2-app-architecture)
3. [Tech Stack](#3-tech-stack)
4. [Project Structure](#4-project-structure)
5. [Authentication System](#5-authentication-system)
6. [Chat Mode Features](#6-chat-mode-features)
7. [CMS Mode Features](#7-cms-mode-features)
8. [API Integration](#8-api-integration)
9. [WebSocket Integration](#9-websocket-integration)
10. [WebRTC Implementation](#10-webrtc-implementation)
11. [Implementation Phases](#11-implementation-phases)
12. [Demo Scenarios](#12-demo-scenarios)
13. [Testing Guide](#13-testing-guide)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Overview

### 1.1 Purpose

The NexusChat Demo App is a **dual-mode application** that demonstrates all capabilities of the NexusChat Engine backend:

- **Chat Mode**: User-facing chat application for real-time communication
- **CMS Mode**: Admin panel for backend operations and system monitoring

### 1.2 Key Features

**Chat Mode:**
- Room management (create, list, view rooms)
- Real-time messaging via WebSocket
- File upload/download (images, documents)
- Voice and video calls (WebRTC)
- Typing indicators
- Presence system (online/offline)
- Message pagination

**CMS Mode:**
- Dashboard with statistics
- Room management (archive, delete)
- Archive browser (view archived data)
- Token generator (for testing)
- System monitoring

### 1.3 Design Principles

1. **No Real Authentication**: Users pick a username and tenant ID directly
2. **Local Deployment Only**: Runs on localhost:8080
3. **Single Codebase**: Flutter app for web + mobile + desktop
4. **Demo-Focused**: Optimized for demonstrating backend capabilities

---

## 2. App Architecture

### 2.1 Application Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                      App Launch                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Identity Selector Screen                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Quick Presets:  [Alice] [Bob] [Carol] [Dave]           │   │
│  │                                                         │   │
│  │  Custom:                                                │   │
│  │  Name:     [________________________]                  │   │
│  │  Tenant:   [tenant-001____________]                   │   │
│  │                                                         │   │
│  │  Mode:     ○ Chat Mode  ○ CMS Mode                    │   │
│  │            [Start Demo →]                              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Token Generation                            │
│  GET /api/dev/token/{userId}?name={name}&tenant={tenantId}      │
│  → Store JWT in memory                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│     Chat Mode           │     │     CMS Mode            │
│  ┌───────────────────┐  │     │  ┌───────────────────┐  │
│  │ Room List        │  │     │  │ Dashboard         │  │
│  │ Chat View        │  │     │  │ Room Management   │  │
│  │ Call Screen      │  │     │  │ Archive Browser   │  │
│  └───────────────────┘  │     │  │ Token Generator   │  │
└─────────────────────────┘     │  └───────────────────┘  │
                               └─────────────────────────┘
```

### 2.2 State Management (BLoC Pattern)

```
┌─────────────────────┐
│   UI (Widgets)      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   BLoC (Logic)      │
│  ┌───────────────┐  │
│  │ State         │  │
│  │ Events        │  │
│  │ Transitions   │  │
│  └───────────────┘  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Services          │
│  ┌───────────────┐  │
│  │ API Service   │  │
│  │ WebSocket     │  │
│  │ Token Service │  │
│  └───────────────┘  │
└─────────────────────┘
```

---

## 3. Tech Stack

### 3.1 Dependencies

```yaml
# pubspec.yaml
name: nexuschat_demo
description: NexusChat Engine Demo App
version: 1.0.0

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  # State Management
  flutter_bloc: ^8.1.0
  equatable: ^2.0.5

  # Networking
  http: ^1.1.0
  web_socket_channel: ^2.4.0

  # WebRTC
  flutter_webrtc: ^0.9.0

  # File Handling
  file_picker: ^6.1.0
  image_picker: ^1.0.0
  cached_network_image: ^3.3.0

  # Utilities
  intl: ^0.18.0
  uuid: ^4.0.0

  # UI
  cupertino_icons: ^1.0.2

dev_dependencies:
  flutter_test:
    sdk: flutter
  build_runner: ^2.4.0
  json_serializable: ^6.7.0
  flutter_lints: ^3.0.0
```

### 3.2 Configuration

```dart
// lib/core/config/app_config.dart
class AppConfig {
  // API Configuration
  static const String baseUrl = 'http://localhost:8080';
  static const String wsUrl = 'ws://localhost:8080/ws/chat';

  // API Endpoints
  static const String roomsEndpoint = '/api/rooms';
  static const String messagesEndpoint = '/api/messages';
  static const String filesEndpoint = '/api/files';
  static const String configEndpoint = '/api/config/webrtc';
  static const String adminEndpoint = '/api/admin';
  static const String archiveEndpoint = '/api/archive';
  static const String devTokenEndpoint = '/api/dev/token';

  // WebSocket Configuration
  static const Duration wsConnectionTimeout = Duration(seconds: 10);
  static const int maxReconnectAttempts = 10;
  static const Duration initialReconnectDelay = Duration(seconds: 1);
  static const Duration maxReconnectDelay = Duration(seconds: 30);

  // Pagination
  static const int defaultPageSize = 50;
  static const int maxPageSize = 100;

  // File Upload
  static const int maxFileSize = 100 * 1024 * 1024; // 100MB
}
```

---

## 4. Project Structure

### 4.1 Directory Layout

```
nexuschat_demo/
├── lib/
│   ├── main.dart                          # App entry point
│   ├── app.dart                           # App routing & theme
│   │
│   ├── core/
│   │   ├── config/
│   │   │   └── app_config.dart            # API URLs & constants
│   │   │
│   │   ├── models/
│   │   │   ├── room.dart                  # Room data model
│   │   │   ├── message.dart               # Message data model
│   │   │   ├── chat_event.dart            # WebSocket event model
│   │   │   ├── file_metadata.dart         # File upload metadata
│   │   │   ├── archive_room.dart           # Archived room model
│   │   │   └── app_mode.dart              # AppMode enum
│   │   │
│   │   ├── services/
│   │   │   ├── api_service.dart           # REST API wrapper
│   │   │   ├── websocket_service.dart     # WebSocket connection
│   │   │   ├── token_service.dart         # JWT generation & storage
│   │   │   ├── file_service.dart          # File upload/download
│   │   │   └── webrtc_service.dart        # WebRTC call handling
│   │   │
│   │   ├── widgets/
│   │   │   ├── mode_switcher.dart         # Chat/CMS mode toggle
│   │   │   ├── loading_indicator.dart     # Loading spinner
│   │   │   ├── error_display.dart         # Error message display
│   │   │   └── message_bubble.dart         # Chat message bubble
│   │   │
│   │   └── utils/
│   │       ├── date_formatter.dart        # Date formatting
│   │       ├── file_validator.dart        # File validation
│   │       └── uuid_generator.dart        # UUID generation
│   │
│   └── features/
│       ├── auth/
│       │   └── identity_selector_screen.dart    # Identity selection
│       │
│       ├── chat/
│       │   ├── room_list/
│       │   │   ├── room_list_screen.dart        # Room list UI
│       │   │   ├── room_list_bloc.dart          # Room list BLoC
│       │   │   ├── room_list_state.dart         # Room list state
│       │   │   ├── room_list_event.dart         # Room list events
│       │   │   └── widgets/
│       │   │       ├── room_list_item.dart      # Room item widget
│       │   │       └── create_room_dialog.dart   # Create room dialog
│       │   │
│       │   ├── chat_view/
│       │   │   ├── chat_screen.dart             # Chat UI
│       │   │   ├── chat_bloc.dart               # Chat BLoC
│       │   │   ├── chat_state.dart              # Chat state
│       │   │   ├── chat_event.dart              # Chat events
│       │   │   └── widgets/
│       │   │       ├── message_list.dart         # Message list
│       │   │       ├── message_input.dart        # Text input
│       │   │       ├── typing_indicator.dart     # Typing indicator
│       │   │       └── attachment_button.dart    # File attachment
│       │   │
│       │   └── call/
│       │       ├── call_screen.dart             # Call UI
│       │       ├── call_bloc.dart               # Call BLoC
│       │       ├── call_state.dart              # Call state
│       │       ├── call_event.dart              # Call events
│       │       └── widgets/
│       │           ├── local_video_renderer.dart # Self video
│       │           ├── remote_video_renderer.dart # Remote video
│       │           └── call_controls.dart         # Mute/Camera/End buttons
│       │
│       └── cms/
│           ├── dashboard/
│           │   ├── dashboard_screen.dart        # Dashboard UI
│           │   ├── dashboard_bloc.dart          # Dashboard BLoC
│           │   ├── dashboard_state.dart         # Dashboard state
│           │   └── widgets/
│           │       ├── stat_card.dart            # Stat card widget
│           │       └── quick_actions.dart        # Quick action buttons
│           │
│           ├── rooms/
│           │   ├── cms_room_list_screen.dart     # Room management
│           │   ├── cms_room_list_bloc.dart       # Room list BLoC
│           │   └── widgets/
│           │       ├── room_detail_dialog.dart    # Room detail
│           │       └── archive_action_dialog.dart  # Archive confirm
│           │
│           ├── archive/
│           │   ├── archive_list_screen.dart      # Archive list
│           │   ├── archive_list_bloc.dart        # Archive BLoC
│           │   ├── archive_detail_screen.dart    # Archive detail
│           │   ├── archive_detail_bloc.dart      # Detail BLoC
│           │   └── widgets/
│           │       ├── archived_room_card.dart    # Archived room
│           │       └── archived_message_list.dart # Archived messages
│           │
│           └── dev_tools/
│               ├── token_generator_screen.dart   # Token generator UI
│               └── token_generator_bloc.dart     # Token gen BLoC
│
├── pubspec.yaml
├── README.md
└── analysis_options.yaml
```

### 4.2 File Naming Conventions

- **Screens**: `*_screen.dart`
- **BLoCs**: `*_bloc.dart`
- **States**: `*_state.dart`
- **Events**: `*_event.dart`
- **Widgets**: `*.dart` (lowercase with underscores)
- **Models**: `*.dart` (lowercase with underscores)
- **Services**: `*_service.dart`

---

## 5. Authentication System

### 5.1 Simplified Authentication Flow

Since this is a demo app, there is no real authentication. Users simply select their identity:

```
1. User opens app
2. User selects name (Alice, Bob, Carol) or enters custom name
3. User selects tenant ID (default: tenant-001)
4. App generates JWT token using /api/dev/token/{userId}
5. Token stored in memory (SharedPreferences)
6. User proceeds to selected mode (Chat or CMS)
```

### 5.2 Token Service Implementation

```dart
// lib/core/services/token_service.dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

class TokenService {
  static const String _tokenKey = 'auth_token';
  static const String _userIdKey = 'user_id';
  static const String _userNameKey = 'user_name';
  static const String _tenantIdKey = 'tenant_id';

  final String baseUrl;
  final http.Client _client;
  final Uuid _uuid = const Uuid();

  TokenService({required this.baseUrl, http.Client? client})
      : _client = client ?? http.Client();

  // Generate JWT token for demo user
  Future<TokenResult> generateIdentity(String name, String tenantId) async {
    try {
      final userId = _uuid.v4(); // Generate random UUID

      final uri = Uri.parse(
        '$baseUrl${AppConfig.devTokenEndpoint}/$userId',
      ).replace(
        queryParameters: {
          'name': name,
          'tenant': tenantId,
        },
      );

      final response = await _client.get(uri);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;

        final token = data['token'] as String;
        final userId = data['userId'] as String;
        final userName = data['name'] as String;
        final userTenantId = data['tenantId'] as String;

        // Store in SharedPreferences
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString(_tokenKey, token);
        await prefs.setString(_userIdKey, userId);
        await prefs.setString(_userNameKey, userName);
        await prefs.setString(_tenantIdKey, userTenantId);

        return TokenResult.success(
          token: token,
          userId: userId,
          name: userName,
          tenantId: userTenantId,
        );
      }

      return TokenResult.error(
        'Failed to generate token: ${response.statusCode}',
      );
    } catch (e) {
      return TokenResult.error('Error: $e');
    }
  }

  // Get stored token
  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  // Get current user info
  Future<UserInfo?> getCurrentUser() async {
    final prefs = await SharedPreferences.getInstance();
    final userId = prefs.getString(_userIdKey);
    final name = prefs.getString(_userNameKey);
    final tenantId = prefs.getString(_tenantIdKey);

    if (userId == null || name == null) return null;

    return UserInfo(
      userId: userId,
      name: name,
      tenantId: tenantId ?? 'tenant-001',
    );
  }

  // Clear stored data (logout)
  Future<void> clearIdentity() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userIdKey);
    await prefs.remove(_userNameKey);
    await prefs.remove(_tenantIdKey);
  }

  // Check if authenticated
  Future<bool> isAuthenticated() async {
    final token = await getToken();
    return token != null && token.isNotEmpty;
  }
}

// Data classes
class TokenResult {
  final String? token;
  final String? userId;
  final String? name;
  final String? tenantId;
  final String? error;

  TokenResult.success({
    required this.token,
    required this.userId,
    required this.name,
    required this.tenantId,
  }) : error = null;

  TokenResult.error(this.error)
      : token = null,
        userId = null,
        name = null,
        tenantId = null;

  bool get isSuccess => error == null;
}

class UserInfo {
  final String userId;
  final String name;
  final String tenantId;

  UserInfo({
    required this.userId,
    required this.name,
    required this.tenantId,
  });
}
```

### 5.3 Identity Selector Screen

```dart
// lib/features/auth/identity_selector_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../core/services/token_service.dart';
import '../../core/widgets/mode_switcher.dart';
import '../chat/room_list/room_list_screen.dart';
import '../cms/dashboard/dashboard_screen.dart';

class IdentitySelectorScreen extends StatefulWidget {
  const IdentitySelectorScreen({super.key});

  @override
  State<IdentitySelectorScreen> createState() => _IdentitySelectorScreenState();
}

class _IdentitySelectorScreenState extends State<IdentitySelectorScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController(text: 'Alice');
  final _tenantController = TextEditingController(text: 'tenant-001');

  AppMode _selectedMode = AppMode.chat;
  bool _isLoading = false;
  String? _errorMessage;

  final List<String> _presetNames = ['Alice', 'Bob', 'Carol', 'Dave'];

  @override
  void dispose() {
    _nameController.dispose();
    _tenantController.dispose();
    super.dispose();
  }

  Future<void> _startDemo() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    final tokenService = TokenService(baseUrl: AppConfig.baseUrl);
    final result = await tokenService.generateIdentity(
      _nameController.text.trim(),
      _tenantController.text.trim(),
    );

    setState(() {
      _isLoading = false;
    });

    if (result.isSuccess) {
      _navigateToMode();
    } else {
      setState(() {
        _errorMessage = result.error ?? 'Failed to start demo';
      });
    }
  }

  void _navigateToMode() {
    final screen = _selectedMode == AppMode.chat
        ? const RoomListScreen()
        : const DashboardScreen();

    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => screen),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Theme.of(context).primaryColor.withOpacity(0.1),
              Theme.of(context).colorScheme.secondary.withOpacity(0.1),
            ],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 400),
                child: Card(
                  elevation: 8,
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          // Title
                          Icon(
                            Icons.chat_bubble,
                            size: 64,
                            color: Theme.of(context).primaryColor,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'NexusChat Demo',
                            style: Theme.of(context).textTheme.headlineMedium,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Select your identity to start',
                            style: Theme.of(context).textTheme.bodyMedium,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 32),

                          // Quick Presets
                          Text(
                            'Quick Presets:',
                            style: Theme.of(context).textTheme.labelLarge,
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            children: _presetNames.map((name) {
                              return OutlinedButton(
                                onPressed: () {
                                  _nameController.text = name;
                                },
                                child: Text(name),
                              );
                            }).toList(),
                          ),
                          const SizedBox(height: 24),

                          // Custom Input
                          TextFormField(
                            controller: _nameController,
                            decoration: const InputDecoration(
                              labelText: 'Name',
                              prefixIcon: Icon(Icons.person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (value) {
                              if (value == null || value.trim().isEmpty) {
                                return 'Please enter a name';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _tenantController,
                            decoration: const InputDecoration(
                              labelText: 'Tenant ID',
                              prefixIcon: Icon(Icons.business),
                              border: OutlineInputBorder(),
                            ),
                            validator: (value) {
                              if (value == null || value.trim().isEmpty) {
                                return 'Please enter a tenant ID';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 24),

                          // Mode Selection
                          Text(
                            'Select Mode:',
                            style: Theme.of(context).textTheme.labelLarge,
                          ),
                          const SizedBox(height: 8),
                          SegmentedButton<AppMode>(
                            segments: const [
                              ButtonSegment(
                                value: AppMode.chat,
                                label: Text('💬 Chat'),
                                icon: Icon(Icons.chat),
                              ),
                              ButtonSegment(
                                value: AppMode.cms,
                                label: Text('⚙️ CMS'),
                                icon: Icon(Icons.admin_panel_settings),
                              ),
                            ],
                            selected: {_selectedMode},
                            onSelectionChanged: (Set<AppMode> newSelection) {
                              setState(() {
                                _selectedMode = newSelection.first;
                              });
                            },
                          ),
                          const SizedBox(height: 32),

                          // Start Button
                          FilledButton(
                            onPressed: _isLoading ? null : _startDemo,
                            style: FilledButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 16),
                            ),
                            child: _isLoading
                                ? const SizedBox(
                                    height: 20,
                                    width: 20,
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  )
                                : const Text('Start Demo'),
                          ),

                          // Error Message
                          if (_errorMessage != null) ...[
                            const SizedBox(height: 16),
                            Text(
                              _errorMessage!,
                              style: TextStyle(
                                color: Theme.of(context).colorScheme.error,
                              ),
                              textAlign: TextAlign.center,
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
```

---

## 6. Chat Mode Features

### 6.1 Room List Screen

```dart
// lib/features/chat/room_list/room_list_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:http/http.dart' as http;
import '../../../../core/services/token_service.dart';
import '../../../../core/config/app_config.dart';
import '../../../../core/models/room.dart';
import 'room_list_bloc.dart';
import 'widgets/room_list_item.dart';
import 'widgets/create_room_dialog.dart';

class RoomListScreen extends StatefulWidget {
  const RoomListScreen({super.key});

  @override
  State<RoomListScreen> createState() => _RoomListScreenState();
}

class _RoomListScreenState extends State<RoomListScreen> {
  final RoomListBloc _bloc = RoomListBloc(
    apiClient: http.Client(),
    tokenService: TokenService(baseUrl: AppConfig.baseUrl),
  );

  @override
  void initState() {
    super.initState();
    _bloc.add(LoadRooms());
  }

  @override
  void dispose() {
    _bloc.close();
    super.dispose();
  }

  Future<void> _refresh() async {
    _bloc.add(LoadRooms());
  }

  void _showCreateRoomDialog() {
    showDialog(
      context: context,
      builder: (context) => CreateRoomDialog(
        onCreate: (name, type, participantIds) {
          _bloc.add(CreateRoom(
            name: name,
            type: type,
            participantIds: participantIds,
          ));
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rooms'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _refresh,
          ),
        ],
      ),
      body: BlocBuilder<RoomListBloc, RoomListState>(
        bloc: _bloc,
        builder: (context, state) {
          if (state is RoomListLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is RoomListError) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.error, size: 48, color: Colors.red),
                  const SizedBox(height: 16),
                  Text(state.message),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _refresh,
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          if (state is RoomListLoaded) {
            final rooms = state.rooms;

            if (rooms.isEmpty) {
              return Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.chat_bubble_outline, size: 48),
                    const SizedBox(height: 16),
                    const Text('No rooms yet'),
                    const SizedBox(height: 8),
                    const Text('Create a room to start chatting'),
                  ],
                ),
              );
            }

            return RefreshIndicator(
              onRefresh: _refresh,
              child: ListView.builder(
                itemCount: rooms.length,
                itemBuilder: (context, index) {
                  return RoomListItem(
                    room: rooms[index],
                    onTap: () {
                      // Navigate to chat screen
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => ChatScreen(roomId: rooms[index].id),
                        ),
                      );
                    },
                  );
                },
              ),
            );
          }

          return const SizedBox.shrink();
        },
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showCreateRoomDialog,
        icon: const Icon(Icons.add),
        label: const Text('Create Room'),
      ),
    );
  }
}
```

### 6.2 Room List BLoC

```dart
// lib/features/chat/room_list/room_list_bloc.dart
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:http/http.dart' as http;
import '../../../../core/services/token_service.dart';
import '../../../../core/models/room.dart';
import 'room_list_event.dart';
import 'room_list_state.dart';

class RoomListBloc extends Bloc<RoomListEvent, RoomListState> {
  final http.Client _apiClient;
  final TokenService _tokenService;

  RoomListBloc({
    required http.Client apiClient,
    required TokenService tokenService,
  })  : _apiClient = apiClient,
        _tokenService = tokenService,
        super(RoomListInitial()) {
    on<LoadRooms>(_onLoadRooms);
    on<CreateRoom>(_onCreateRoom);
  }

  Future<void> _onLoadRooms(
    LoadRooms event,
    Emitter<RoomListState> emit,
  ) async {
    emit(RoomListLoading());

    try {
      final token = await _tokenService.getToken();
      if (token == null) {
        emit(const RoomListError('Not authenticated'));
        return;
      }

      final response = await _apiClient.get(
        Uri.parse('${AppConfig.baseUrl}${AppConfig.roomsEndpoint}'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> json = jsonDecode(response.body);
        final rooms = json.map((e) => Room.fromJson(e as Map<String, dynamic>)).toList();
        emit(RoomListLoaded(rooms));
      } else {
        emit(RoomListError('Failed to load rooms: ${response.statusCode}'));
      }
    } catch (e) {
      emit(RoomListError('Error: $e'));
    }
  }

  Future<void> _onCreateRoom(
    CreateRoom event,
    Emitter<RoomListState> emit,
  ) async {
    try {
      final token = await _tokenService.getToken();
      if (token == null) {
        emit(const RoomListError('Not authenticated'));
        return;
      }

      final request = {
        'name': event.name,
        'type': event.type.name,
        'participantIds': event.participantIds,
      };

      final response = await _apiClient.post(
        Uri.parse('${AppConfig.baseUrl}${AppConfig.roomsEndpoint}'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode(request),
      );

      if (response.statusCode == 201) {
        // Room created, reload list
        add(LoadRooms());
      } else {
        emit(RoomListError('Failed to create room: ${response.statusCode}'));
      }
    } catch (e) {
      emit(RoomListError('Error: $e'));
    }
  }
}
```

### 6.3 Room List Events

```dart
// lib/features/chat/room_list/room_list_event.dart
import 'package:equatable/equatable.dart';

abstract class RoomListEvent extends Equatable {
  const RoomListEvent();

  @override
  List<Object?> get props => [];
}

class LoadRooms extends RoomListEvent {
  const LoadRooms();
}

class CreateRoom extends RoomListEvent {
  final String name;
  final RoomType type;
  final List<String> participantIds;

  const CreateRoom({
    required this.name,
    required this.type,
    required this.participantIds,
  });

  @override
  List<Object?> get props => [name, type, participantIds];
}
```

### 6.4 Room List States

```dart
// lib/features/chat/room_list/room_list_state.dart
import 'package:equatable/equatable.dart';
import '../../../../core/models/room.dart';

abstract class RoomListState extends Equatable {
  const RoomListState();

  @override
  List<Object?> get props => [];
}

class RoomListInitial extends RoomListState {
  const RoomListInitial();
}

class RoomListLoading extends RoomListState {
  const RoomListLoading();
}

class RoomListLoaded extends RoomListState {
  final List<Room> rooms;

  const RoomListLoaded(this.rooms);

  @override
  List<Object?> get props => [rooms];
}

class RoomListError extends RoomListState {
  final String message;

  const RoomListError(this.message);

  @override
  List<Object?> get props => [message];
}
```

---

## 7. CMS Mode Features

### 7.1 Dashboard Screen

```dart
// lib/features/cms/dashboard/dashboard_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:http/http.dart' as http;
import '../../../../core/services/token_service.dart';
import '../../../../core/config/app_config.dart';
import 'dashboard_bloc.dart';
import 'dashboard_state.dart';
import 'widgets/stat_card.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final DashboardBloc _bloc = DashboardBloc(
    apiClient: http.Client(),
    tokenService: TokenService(baseUrl: AppConfig.baseUrl),
  );

  @override
  void initState() {
    super.initState();
    _bloc.add(LoadDashboardStats());
  }

  @override
  void dispose() {
    _bloc.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('CMS Dashboard'),
      ),
      body: BlocBuilder<DashboardBloc, DashboardState>(
        bloc: _bloc,
        builder: (context, state) {
          if (state is DashboardLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is DashboardLoaded) {
            final stats = state.stats;

            return SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Statistics Cards
                  GridView.count(
                    crossAxisCount: 2,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    mainAxisSpacing: 16,
                    crossAxisSpacing: 16,
                    children: [
                      StatCard(
                        title: 'Total Rooms',
                        value: stats.totalRooms.toString(),
                        icon: Icons.meeting_room,
                        color: Colors.blue,
                      ),
                      StatCard(
                        title: 'Total Messages',
                        value: stats.totalMessages.toString(),
                        icon: Icons.message,
                        color: Colors.green,
                      ),
                      StatCard(
                        title: 'Active Users',
                        value: stats.activeUsers.toString(),
                        icon: Icons.people,
                        color: Colors.orange,
                      ),
                      StatCard(
                        title: 'Archived Rooms',
                        value: stats.archivedRooms.toString(),
                        icon: Icons.archive,
                        color: Colors.purple,
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),

                  // Quick Actions
                  Text(
                    'Quick Actions',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 16),
                  Card(
                    child: Column(
                      children: [
                        ListTile(
                          leading: const Icon(Icons.list),
                          title: const Text('Manage Rooms'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () {
                            // Navigate to room management
                          },
                        ),
                        const Divider(),
                        ListTile(
                          leading: const Icon(Icons.archive),
                          title: const Text('Browse Archives'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () {
                            // Navigate to archive browser
                          },
                        ),
                        const Divider(),
                        ListTile(
                          leading: const Icon(Icons.vpn_key),
                          title: const Text('Generate Token'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () {
                            // Navigate to token generator
                          },
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          }

          if (state is DashboardError) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error, size: 48),
                  const SizedBox(height: 16),
                  Text(state.message),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: () => _bloc.add(LoadDashboardStats()),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }
}
```

### 7.2 Dashboard BLoC

```dart
// lib/features/cms/dashboard/dashboard_bloc.dart
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:http/http.dart' as http;
import '../../../../core/services/token_service.dart';
import '../../../../core/config/app_config.dart';
import 'dashboard_event.dart';
import 'dashboard_state.dart';

class DashboardBloc extends Bloc<DashboardEvent, DashboardState> {
  final http.Client _apiClient;
  final TokenService _tokenService;

  DashboardBloc({
    required http.Client apiClient,
    required TokenService tokenService,
  })  : _apiClient = apiClient,
        _tokenService = tokenService,
        super(DashboardInitial()) {
    on<LoadDashboardStats>(_onLoadStats);
  }

  Future<void> _onLoadStats(
    LoadDashboardStats event,
    Emitter<DashboardState> emit,
  ) async {
    emit(DashboardLoading());

    try {
      final token = await _tokenService.getToken();
      if (token == null) {
        emit(const DashboardError('Not authenticated'));
        return;
      }

      // Fetch rooms count
      final roomsResponse = await _apiClient.get(
        Uri.parse('${AppConfig.baseUrl}${AppConfig.roomsEndpoint}'),
        headers: {'Authorization': 'Bearer $token'},
      );

      // Fetch archived rooms count
      final archiveResponse = await _apiClient.get(
        Uri.parse('${AppConfig.baseUrl}${AppConfig.archiveEndpoint}/rooms'),
        headers: {'Authorization': 'Bearer $token'},
      );

      if (roomsResponse.statusCode == 200 && archiveResponse.statusCode == 200) {
        final roomsList = jsonDecode(roomsResponse.body) as List;
        final archiveList = jsonDecode(archiveResponse.body) as List;

        final stats = DashboardStats(
          totalRooms: roomsList.length,
          totalMessages: 0, // Would require additional API call
          activeUsers: 0, // Would require Redis access
          archivedRooms: archiveList.length,
        );

        emit(DashboardLoaded(stats));
      } else {
        emit(const DashboardError('Failed to load dashboard stats'));
      }
    } catch (e) {
      emit(DashboardError('Error: $e'));
    }
  }
}
```

### 7.3 Token Generator Screen

```dart
// lib/features/cms/dev_tools/token_generator_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import '../../../../core/config/app_config.dart';
import 'package:uuid/uuid.dart';

class TokenGeneratorScreen extends StatefulWidget {
  const TokenGeneratorScreen({super.key});

  @override
  State<TokenGeneratorScreen> createState() => _TokenGeneratorScreenState();
}

class _TokenGeneratorScreenState extends State<TokenGeneratorScreen> {
  final _formKey = GlobalKey<FormState>();
  final _userIdController = TextEditingController();
  final _nameController = TextEditingController();
  final _tenantController = TextEditingController(text: 'tenant-001');

  String? _generatedToken;
  bool _isGenerating = false;

  final Uuid _uuid = const Uuid();

  @override
  void dispose() {
    _userIdController.dispose();
    _nameController.dispose();
    _tenantController.dispose();
    super.dispose();
  }

  Future<void> _generateToken() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isGenerating = true;
      _generatedToken = null;
    });

    try {
      final userId = _userIdController.text.trim().isEmpty
          ? _uuid.v4()
          : _userIdController.text.trim();

      final uri = Uri.parse(
        '$baseUrl${AppConfig.devTokenEndpoint}/$userId',
      ).replace(queryParameters: {
        'name': _nameController.text.trim(),
        'tenant': _tenantController.text.trim(),
      });

      final response = await http.Client().get(uri);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          _generatedToken = data['token'] as String;
        });
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e')),
      );
    } finally {
      setState(() {
        _isGenerating = false;
      });
    }
  }

  void _copyToken() {
    if (_generatedToken != null) {
      Clipboard.setData(ClipboardData(text: _generatedToken!));
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Token copied to clipboard')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Token Generator'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Generate Test JWT Token',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _userIdController,
                        decoration: const InputDecoration(
                          labelText: 'User ID (Optional)',
                          hintText: 'Auto-generated if empty',
                          prefixIcon: Icon(Icons.person),
                        ),
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _nameController,
                        decoration: const InputDecoration(
                          labelText: 'Name',
                          prefixIcon: Icon(Icons.badge),
                        ),
                        validator: (value) =>
                            value?.trim().isEmpty ?? true ? 'Required' : null,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _tenantController,
                        decoration: const InputDecoration(
                          labelText: 'Tenant ID',
                          prefixIcon: Icon(Icons.business),
                        ),
                        validator: (value) =>
                            value?.trim().isEmpty ?? true ? 'Required' : null,
                      ),
                      const SizedBox(height: 24),
                      FilledButton(
                        onPressed: _isGenerating ? null : _generateToken,
                        style: FilledButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                        ),
                        child: _isGenerating
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Text('Generate Token'),
                      ),
                    ],
                  ),
                ),
              ),
              if (_generatedToken != null) ...[
                const SizedBox(height: 16),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Generated Token',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                            TextButton.icon(
                              onPressed: _copyToken,
                              icon: const Icon(Icons.copy),
                              label: const Text('Copy'),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.grey[100],
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            _generatedToken!,
                            style: const TextStyle(fontFamily: 'monospace', fontSize: 10),
                            maxLines: null,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
```

---

## 8. API Integration

### 8.1 API Service Base Class

```dart
// lib/core/services/api_service.dart
import 'package:http/http.dart' as http;
import 'package:json_annotation/json_annotation.dart';
import 'token_service.dart';

part 'api_service.g.dart';

class ApiService {
  final String baseUrl;
  final TokenService _tokenService;
  final http.Client _client;

  ApiService({
    required this.baseUrl,
    required TokenService tokenService,
    http.Client? client,
  })  : _tokenService = tokenService,
        _client = client ?? http.Client();

  // GET request
  Future<http.Response> get(String endpoint, {Map<String, dynamic>? query}) async {
    final token = await _tokenService.getToken();
    if (token == null) {
      throw ApiException('Not authenticated');
    }

    final uri = Uri.parse('$baseUrl$endpoint').replace(
      queryParameters: query,
    );

    return _client.get(
      uri,
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
    );
  }

  // POST request
  Future<http.Response> post(
    String endpoint, {
    required Map<String, dynamic> body,
  }) async {
    final token = await _tokenService.getToken();
    if (token == null) {
      throw ApiException('Not authenticated');
    }

    return _client.post(
      Uri.parse('$baseUrl$endpoint'),
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
      body: jsonEncode(body),
    );
  }

  // PUT request
  Future<http.Response> put(
    String endpoint, {
    required Map<String, dynamic> body,
  }) async {
    final token = await _tokenService.getToken();
    if (token == null) {
      throw ApiException('Not authenticated');
    }

    return _client.put(
      Uri.parse('$baseUrl$endpoint'),
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
      body: jsonEncode(body),
    );
  }

  // DELETE request
  Future<http.Response> delete(String endpoint) async {
    final token = await _tokenService.getToken();
    if (token == null) {
      throw ApiException('Not authenticated');
    }

    return _client.delete(
      Uri.parse('$baseUrl$endpoint'),
      headers: {
        'Authorization': 'Bearer $token',
      },
    );
  }
}

class ApiException implements Exception {
  final String message;
  final int? statusCode;

  ApiException(this.message, {this.statusCode});

  @override
  String toString() => 'ApiException: $message';
}
```

### 8.2 REST API Endpoints Reference

**Room Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/rooms?page=0&size=20` | List all rooms |
| POST | `/api/rooms` | Create new room |
| GET | `/api/rooms/{id}` | Get room details |
| GET | `/api/rooms/{id}/messages?page=0&size=50` | Get room messages |

**Message Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/messages` | Send message |
| GET | `/api/messages/{roomId}?page=0&size=50` | Get messages |

**File Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload-url` | Get upload URL |
| POST | `/api/files/{id}/confirm` | Confirm upload |
| GET | `/api/files/{id}/download-url` | Get download URL |

**Config Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/config/webrtc` | Get WebRTC config |

**Admin Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/archive/{roomId}` | Archive room |
| GET | `/api/admin/archived/{roomId}` | Check archive status |

**Archive Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/archive/rooms` | List archived rooms |
| GET | `/api/archive/rooms/{id}` | Get archived room |
| GET | `/api/archive/rooms/{id}/messages` | Get archived messages |
| GET | `/api/archive/search/by-original-room/{roomId}` | Find archive |

---

## 9. WebSocket Integration

### 9.1 WebSocket Service

```dart
// lib/core/services/websocket_service.dart
import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'chat_event.dart';

class ChatWebSocketService {
  static const String _tokenKey = 'auth_token';
  static const String _wsUrlKey = 'ws_url';

  WebSocketChannel? _channel;
  StreamSubscription? _streamSubscription;
  final StreamController<ChatEvent> _eventController = StreamController.broadcast();
  Timer? _reconnectTimer;
  int _reconnectAttempts = 0;

  Stream<ChatEvent> get events => _eventController.stream;

  Future<void> connect(String wsUrl, String token) async {
    await disconnect();

    try {
      final uri = Uri.parse('$wsUrl?token=$token');
      _channel = WebSocketChannel.connect(uri);

      _streamSubscription = _channel!.stream.listen(
        _onMessage,
        onError: _onError,
        onDone: _onDone,
        cancelOnError: false,
      );

      _reconnectAttempts = 0; // Reset on success

      // Store URL for reconnection
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_wsUrlKey, wsUrl);
    } catch (e) {
      _onError(e);
    }
  }

  void _onMessage(dynamic message) {
    try {
      final json = jsonDecode(message as String) as Map<String, dynamic>;
      final event = ChatEvent.fromJson(json);
      _eventController.add(event);
    } catch (e) {
      print('Error parsing WebSocket message: $e');
    }
  }

  void _onError(dynamic error) {
    print('WebSocket error: $error');
  }

  void _onDone() {
    // Connection closed, attempt reconnect
    if (_reconnectAttempts < AppConfig.maxReconnectAttempts) {
      _reconnectAttempts++;
      final delay = _getReconnectDelay();

      print('WebSocket closed. Reconnecting in ${delay.inSeconds}s (attempt $_reconnectAttempts)');

      _reconnectTimer = Timer(delay, () => _reconnect());
    } else {
      print('Max reconnect attempts reached');
    }
  }

  Duration _getReconnectDelay() {
    final exponential = AppConfig.initialReconnectDelay.inMilliseconds *
        pow(2, _reconnectAttempts).toInt();
    final capped = min(
      exponential,
      AppConfig.maxReconnectDelay.inMilliseconds,
    );
    return Duration(milliseconds: capped);
  }

  Future<void> _reconnect() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(_tokenKey);
    final wsUrl = prefs.getString(_wsUrlKey);

    if (token != null && wsUrl != null) {
      await connect(wsUrl, token);
    }
  }

  void sendEvent(String eventType, Map<String, dynamic> data) {
    if (_channel == null) {
      print('WebSocket not connected');
      return;
    }

    final envelope = {
      'event': eventType,
      'traceId': const Uuid().v4(),
      'data': data,
    };

    try {
      _channel!.sink.add(jsonEncode(envelope));
    } catch (e) {
      print('Error sending WebSocket message: $e');
    }
  }

  void sendMessage({
    required String roomId,
    required String type,
    String? contentText,
    String? contentMeta,
    String? clientRef,
  }) {
    sendEvent('SEND_MSG', {
      'roomId': roomId,
      'type': type,
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
    _reconnectTimer?.cancel();
    await _streamSubscription?.cancel();
    await _channel?.sink.close();
    _channel = null;
    await _eventController.close();
  }
}
```

### 9.2 WebSocket Events Reference

**Client → Server Events:**

| Event | Data Fields | Description |
|-------|-------------|-------------|
| SEND_MSG | roomId, type, contentText, contentMeta, clientRef | Send message |
| TYPING | roomId, isTyping | Typing indicator |
| SIGNAL_SDP | targetId, type, sdp | WebRTC SDP |
| SIGNAL_ICE | targetId, candidate, sdpMid, sdpMLineIndex | WebRTC ICE |
| ACK | messageId | Acknowledge message |

**Server → Client Events:**

| Event | Data Fields | Description |
|-------|-------------|-------------|
| NEW_MESSAGE | id, roomId, senderId, type, contentText, createdAt | New message |
| TYPING | roomId, userId, userName, isTyping | Typing indicator |
| PRESENCE | userId, userName, status | User presence |
| SIGNAL_SDP | targetId, type, sdp | WebRTC SDP |
| SIGNAL_ICE | targetId, candidate, sdpMid, sdpMLineIndex | WebRTC ICE |
| CALL_REJECTED | callerId, targetId, status, reason | Call rejected |

---

## 10. WebRTC Implementation

### 10.1 WebRTC Service

```dart
// lib/core/services/webrtc_service.dart
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'websocket_service.dart';
import 'token_service.dart';

class WebRTCService {
  final ChatWebSocketService _wsService;
  final TokenService _tokenService;

  RTCPeerConnection? _peerConnection;
  MediaStream? _localStream;
  MediaStream? _remoteStream;

  final List<RTCIceCandidate> _remoteCandidates = [];

  WebRTCService({
    required ChatWebSocketService wsService,
    required TokenService tokenService,
  })  : _wsService = wsService,
        _tokenService = tokenService;

  Future<void> initializeCall({
    required bool isVideoCall,
    required List<IceServer> iceServers,
  }) async {
    final configuration = RTCConfiguration(
      iceServers: iceServers.map((server) {
        return RTCIceServer(
          urls: server.urls,
          username: server.username,
          credential: server.credential,
        );
      }).toList(),
    );

    _peerConnection = await createPeerConnection(configuration);

    _peerConnection!.onIceCandidate = (candidate) {
      print('ICE candidate found: ${candidate.candidate}');
      // Send to peer via WebSocket
      // _wsService.sendSignalIce(...);
    };

    _peerConnection!.onConnectionState = (state) {
      print('Connection state: $state');
    };

    _peerConnection!.onAddStream = (stream) {
      print('Remote stream added');
      _remoteStream = stream;
    };

    // Get user media
    final constraints = {
      'audio': true,
      'video': isVideoCall,
    };

    _localStream = await navigator.mediaDevices.getUserMedia(constraints);
  }

  Future<RTCSessionDescription> createOffer() async {
    final offer = await _peerConnection!.createOffer();
    await _peerConnection!.setLocalDescription(offer);
    return offer;
  }

  Future<void> setRemoteDescription(RTCSessionDescription description) async {
    await _peerConnection!.setRemoteDescription(description);
  }

  Future<RTCSessionDescription> createAnswer() async {
    final answer = await _peerConnection!.createAnswer();
    await _peerConnection!.setLocalDescription(answer);
    return answer;
  }

  Future<void> addCandidate(RTCIceCandidate candidate) async {
    if (_peerConnection!.remoteDescription != null) {
      await _peerConnection!.addCandidate(candidate);
    } else {
      _remoteCandidates.add(candidate);
    }
  }

  Future<void> hangup() async {
    await _localStream?.dispose();
    await _remoteStream?.dispose();
    await _peerConnection?.close();
    _peerConnection = null;
  }
}
```

### 10.2 Call Screen UI

```dart
// lib/features/chat/call/call_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import '../../../../core/services/webrtc_service.dart';
import 'widgets/call_controls.dart';

class CallScreen extends StatefulWidget {
  final String targetUserId;
  final String targetUserName;
  final bool isVideoCall;
  final bool isIncoming;

  const CallScreen({
    super.key,
    required this.targetUserId,
    required this.targetUserName,
    required this.isVideoCall,
    this.isIncoming = false,
  });

  @override
  State<CallScreen> createState() => _CallScreenState();
}

class _CallScreenState extends State<CallScreen> {
  late WebRTCService _webrtcService;
  MediaStream? _localStream;
  MediaStream? _remoteStream;
  bool _isMuted = false;
  bool _isCameraOff = false;
  bool _isSpeakerOn = true;
  DateTime? _callStartTime;
  Timer? _callDurationTimer;

  @override
  void initState() {
    super.initState();
    _initializeCall();
  }

  Future<void> _initializeCall() async {
    // Get WebRTC config from API
    final iceServers = await _getIceServers();

    _webrtcService = WebRTCService(
      wsService: context.read<ChatWebSocketService>(),
      tokenService: context.read<TokenService>(),
    );

    await _webrtcService.initializeCall(
      isVideoCall: widget.isVideoCall,
      iceServers: iceServers,
    );

    setState(() {
      _localStream = _webrtcService._localStream;
    });

    if (widget.isIncoming) {
      // Answer the call
      await _answerCall();
    } else {
      // Start the call
      await _startCall();
    }

    setState(() {
      _callStartTime = DateTime.now();
    });
    _startCallDurationTimer();
  }

  Future<void> _startCall() async {
    // Create offer
    final offer = await _webrtcService.createOffer();

    // Send offer via WebSocket
    context.read<ChatWebSocketService>().sendSignalSdp(
      targetId: widget.targetUserId,
      type: 'offer',
      sdp: offer.sdp!,
    );
  }

  Future<void> _answerCall() async {
    // Create answer
    final answer = await _webrtcService.createAnswer();

    // Send answer via WebSocket
    context.read<ChatWebSocketService>().sendSignalSdp(
      targetId: widget.targetUserId,
      type: 'answer',
      sdp: answer.sdp!,
    );
  }

  void _startCallDurationTimer() {
    _callDurationTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      setState(() {}); // Update UI every second
    });
  }

  Duration get _callDuration {
    if (_callStartTime == null) return Duration.zero;
    return DateTime.now().difference(_callStartTime!);
  }

  @override
  void dispose() {
    _callDurationTimer?.cancel();
    _webrtcService.hangup();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Column(
          children: [
            // Header
            AppBar(
              backgroundColor: Colors.transparent,
              elevation: 0,
              title: Text(widget.isIncoming ? 'Incoming Call' : 'Calling'),
              actions: [
                Text(_formatDuration(_callDuration)),
                const SizedBox(width: 16),
              ],
            ),

            // Video Area
            Expanded(
              child: Stack(
                children: [
                  // Remote Video
                  if (_remoteStream != null)
                    RTCVideoView(_remoteStream!.renderer.srcObject!)
                  else
                    Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          CircleAvatar(
                            radius: 40,
                            child: Text(
                              widget.targetUserName[0].toUpperCase(),
                              style: const TextStyle(fontSize: 32),
                            ),
                          ),
                          const SizedBox(height: 16),
                          Text(
                            widget.targetUserName,
                            style: const TextStyle(fontSize: 24, color: Colors.white),
                          ),
                          if (widget.isIncoming)
                            const Text('Incoming call...', style: TextStyle(color: Colors.grey))
                          else
                            const Text('Calling...', style: TextStyle(color: Colors.grey)),
                        ],
                      ),
                    ),

                  // Local Video (Picture-in-Picture)
                  if (_localStream != null && widget.isVideoCall)
                    Positioned(
                      right: 16,
                      top: 16,
                      child: SizedBox(
                        width: 120,
                        height: 160,
                        child: RTCVideoView(_localStream!.renderer.srcObject!),
                      ),
                    ),
                ],
              ),
            ),

            // Controls
            CallControls(
              isMuted: _isMuted,
              isCameraOff: _isCameraOff,
              isSpeakerOn: _isSpeakerOn,
              onToggleMute: () => setState(() => _isMuted = !_isMuted),
              onToggleCamera: () => setState(() => _isCameraOff = !_isCameraOff),
              onToggleSpeaker: () => setState(() => _isSpeakerOn = !_isSpeakerOn),
              onEndCall: () => Navigator.of(context).pop(),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDuration(Duration duration) {
    final minutes = duration.inMinutes.toString().padLeft(2, '0');
    final seconds = (duration.inSeconds % 60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }
}
```

---

## 11. Implementation Phases

### Week 1: Foundation (Day 1-7)

**Tasks:**
1. Create Flutter project
2. Setup folder structure
3. Add dependencies to pubspec.yaml
4. Create AppConfig class
5. Create TokenService
6. Create ApiService base class
7. Create Identity Selector Screen
8. Create ModeSwitcher widget
9. Setup routing

**Deliverable**: App launches, shows identity selector, can pick name/tenant/mode

### Week 2: Chat Mode - Room Management (Day 8-14)

**Tasks:**
1. Create Room model
2. Create RoomListBloc (events, states)
3. Create RoomListScreen
4. Create RoomListItem widget
5. Create CreateRoomDialog
6. Connect to GET /api/rooms
7. Connect to POST /api/rooms
8. Add pull-to-refresh
9. Add error handling

**Deliverable**: User can list rooms, create new rooms

### Week 3: Chat Mode - Messaging Core (Day 15-21)

**Tasks:**
1. Create Message model
2. Create ChatEvent model
3. Create WebSocketService
4. Create ChatBloc
5. Create ChatScreen UI
6. Create MessageBubble widget
7. Create MessageInput widget
8. Implement SEND_MSG via WebSocket
9. Implement NEW_MESSAGE reception
10. Add message pagination
11. Add load more functionality

**Deliverable**: User can send and receive messages in real-time

### Week 4: Chat Mode - Advanced Features (Day 22-28)

**Tasks:**
1. Implement typing indicators (TYPING event)
2. Implement presence system (PRESENCE event)
3. Create FileService
4. Implement file upload flow
5. Implement file download flow
6. Create AttachmentButton widget
7. Add image picker integration
8. Add file validation

**Deliverable**: User can share files, see typing, presence

### Week 5: WebRTC Calls + CMS Mode (Day 29-35)

**Part A: WebRTC Calls**
1. Create WebRTCService
2. Get WebRTC config from API
3. Create CallScreen UI
4. Create CallControls widget
5. Implement voice call
6. Implement video call
7. Add permission handling
8. Implement signaling (SIGNAL_SDP, SIGNAL_ICE)

**Part B: CMS Mode**
1. Create DashboardBloc
2. Create DashboardScreen
3. Add stat cards
4. Create CMSRoomListScreen
5. Create ArchiveListScreen
6. Create TokenGeneratorScreen
7. Connect to admin endpoints

**Deliverable**: Calls work, CMS mode functional

### Week 6: Polish & Demo Prep (Day 36-42)

**Tasks:**
1. Add comprehensive error handling
2. Add loading states
3. Implement WebSocket auto-reconnect
4. Add message status indicators
5. Create preset users (Alice, Bob, Carol)
6. Write README with setup instructions
7. Create demo script
8. Test all features end-to-end

**Deliverable**: Production-ready demo app

---

## 12. Demo Scenarios

### Scenario 1: Basic Two-User Chat

**Objective**: Demonstrate basic chat functionality

**Steps**:
1. Open browser tab 1 → Select "Alice" → Chat Mode
2. Open browser tab 2 → Select "Bob" → Chat Mode
3. Tab 1 (Alice): Create room "Engineering Team" with Bob
4. Tab 1: Send "Hello Bob!"
5. Tab 2 (Bob): See message appear instantly
6. Tab 2: Reply "Hi Alice! How are you?"
7. Tab 1: See reply appear instantly

**Expected Results**: Messages appear in real-time, no page refresh needed

### Scenario 2: Multi-User Group Chat

**Objective**: Demonstrate group messaging

**Steps**:
1. Three browser tabs: Alice, Bob, Carol
2. Alice creates group room with Bob and Carol
3. Alice sends "Hey team!"
4. Bob and Carol see message
5. Bob sends "Hi Alice"
6. Alice and Carol see message
7. Carol starts typing → Alice and Bob see "Carol is typing..."
8. Carol sends "Ready for the meeting"

**Expected Results**: All users receive messages, typing indicators work

### Scenario 3: File Sharing

**Objective**: Demonstrate file upload/download

**Steps**:
1. Alice sends image to Bob
2. Alice clicks attachment button → Picks image
3. Image uploads, preview shows in chat
4. Bob sees image with download button
5. Bob clicks download → Image opens in browser
6. Bob sends document to Alice
7. Alice downloads document

**Expected Results**: Files upload/download correctly, previews work

### Scenario 4: Voice Call

**Objective**: Demonstrate WebRTC voice calling

**Steps**:
1. Alice clicks voice call button with Bob
2. Bob sees incoming call screen
3. Bob accepts → Voice call starts
4. Both can hear each other
5. Alice mutes microphone → Bob can't hear Alice
6. Alice unmutes → Audio resumes
7. Bob ends call

**Expected Results**: Voice call works, controls function properly

### Scenario 5: Video Call

**Objective**: Demonstrate WebRTC video calling

**Steps**:
1. Alice video calls Bob
2. Bob accepts → Video call starts
3. Both see each other's video
4. Alice turns off camera → Bob sees black screen
5. Alice turns camera back on → Video resumes
6. Bob ends call

**Expected Results**: Video call works, camera controls work

### Scenario 6: Room Archiving (CMS Mode)

**Objective**: Demonstrate admin archive functionality

**Steps**:
1. Switch to CMS Mode
2. View dashboard stats
3. Open room management
4. Select a room → Click "Archive"
5. Confirm archive action
6. Room disappears from main list
7. Browse archives → Find archived room
8. View archived messages

**Expected Results**: Room moved to archive, data preserved

---

## 13. Testing Guide

### 13.1 Unit Testing

Test BLoC state transitions:

```dart
// test/features/chat/room_list/room_list_bloc_test.dart
void main() {
  late RoomListBloc bloc;
  late MockApiService mockApi;
  late MockTokenService mockToken;

  setUp(() {
    mockApi = MockApiService();
    mockToken = MockTokenService();
    bloc = RoomListBloc(apiClient: mockApi, tokenService: mockToken);
  });

  tearDown(() {
    bloc.close();
  });

  test('initial state is RoomListInitial', () {
    expect(bloc.state, equals(RoomListInitial()));
  });

  test('emits [RoomListLoading, RoomListLoaded] when LoadRooms succeeds', () async {
    // Arrange
    when(() => mockToken.getToken()).thenAnswer((_) async => 'test-token');
    when(() => mockApi.get(any)).thenAnswer(
      (_) async => http.Response(
        body: jsonEncode([
          {'id': '1', 'name': 'Room 1', 'type': 'GROUP'},
        ]),
        statusCode: 200,
      ),
    );

    // Act
    final expected = [
      RoomListInitial(),
      RoomListLoading(),
      RoomListLoaded([Room(id: '1', name: 'Room 1', type: RoomType.group)]),
    ];
    expectLater(bloc.stream, emitsInOrder(expected));

    // Assert
    bloc.add(LoadRooms());
  });
}
```

### 13.2 Widget Testing

Test UI rendering:

```dart
// test/features/auth/identity_selector_screen_test.dart
void main() {
  testWidgets('IdentitySelectorScreen renders form', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: IdentitySelectorScreen()),
    );

    expect(find.text('NexusChat Demo'), findsOneWidget);
    expect(find.byType(TextField), findsNWidgets(2));
    expect(find.text('Start Demo'), findsOneWidget);
  });

  testWidgets('Preset names fill name field', (tester) async {
    await tester.pumpWidget(
      MaterialApp(home: IdentitySelectorScreen()),
    );

    await tester.tap(find.text('Alice'));
    await tester.pump();

    final textField = tester.widget<TextField>(
      find.byKey(Key('name-field')),
    );
    expect(textField.controller?.text, equals('Alice'));
  });
}
```

### 13.3 Integration Testing

Test API integration:

```dart
// test/integration/api_integration_test.dart
void main() {
  group('Room API Integration', () {
    late ApiService apiService;
    late TokenService tokenService;

    setUp(() async {
      apiService = ApiService(baseUrl: 'http://localhost:8080');
      tokenService = TokenService(baseUrl: 'http://localhost:8080');

      // Generate test token
      await tokenService.generateIdentity('TestUser', 'tenant-001');
    });

    test('GET /api/rooms returns list of rooms', () async {
      final response = await apiService.get('/api/rooms');
      expect(response.statusCode, equals(200));
    });
  });
}
```

---

## 14. Troubleshooting

### 14.1 Common Issues

**Issue**: WebSocket connection fails
**Solution**:
- Ensure server is running on localhost:8080
- Check token is valid
- Verify wsUrl is correct: `ws://localhost:8080/ws/chat`

**Issue**: Messages not appearing in real-time
**Solution**:
- Check browser console for WebSocket errors
- Verify Redis is running
- Check server logs for pub/sub errors

**Issue**: File upload fails
**Solution**:
- Ensure MinIO is running
- Check file size doesn't exceed 100MB
- Verify upload URL is valid (not expired)

**Issue**: WebRTC call fails
**Solution**:
- Check camera/microphone permissions
- Verify WebRTC config is fetched
- Check firewall settings
- Ensure both users have valid tokens

**Issue**: Mode switcher doesn't work
**Solution**:
- Check if AppMode enum is properly defined
- Verify state management is set up correctly
- Ensure navigation is set up properly

### 14.2 Debug Tips

1. **Enable Logging**:
```dart
import 'package:flutter/foundation.dart';

// Add debug prints
debugPrint('WebSocket message: $message');
```

2. **Use Flutter DevTools**:
```bash
flutter pub global activate devtools
flutter pub global run devtools
```

3. **Check Network Traffic**:
- Open browser DevTools (F12)
- Go to Network tab
- Filter by WS (WebSocket) or XHR (HTTP)

4. **Monitor Server Logs**:
```bash
# In server directory
./gradlew quarkusDev
# Watch logs for WebSocket events
```

5. **Test API with cURL**:
```bash
# Test token generation
curl "http://localhost:8080/api/dev/token/test-id?name=Alice&tenant=tenant-001"

# Test room list
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/rooms
```

---

## Appendix

### A. Model Classes

```dart
// lib/core/models/room.dart
import 'package:json_annotation/json_annotation.dart';

part 'room.g.dart';

@JsonSerializable()
class Room {
  final String id;
  final String name;
  @JsonKey(name: 'type')
  final RoomType type;
  @JsonKey(name: 'tenantId')
  final String tenantId;
  @JsonKey(name: 'createdAt')
  final DateTime createdAt;

  Room({
    required this.id,
    required this.name,
    required this.type,
    required this.tenantId,
    required this.createdAt,
  });

  factory Room.fromJson(Map<String, dynamic> json) => _$RoomFromJson(json);

  Map<String, dynamic> toJson() => _$RoomToJson(this);
}

enum RoomType {
  @JsonValue('DIRECT')
  direct,
  @JsonValue('GROUP')
  group,
}
```

```dart
// lib/core/models/message.dart
import 'package:json_annotation/json_annotation.dart';

part 'message.g.dart';

@JsonSerializable()
class Message {
  final String id;
  @JsonKey(name: 'roomId')
  final String roomId;
  @JsonKey(name: 'senderId')
  final String senderId;
  final MessageType type;
  @JsonKey(name: 'contentText')
  final String? contentText;
  @JsonKey(name: 'contentMeta')
  final String? contentMeta;
  @JsonKey(name: 'createdAt')
  final DateTime createdAt;

  Message({
    required this.id,
    required this.roomId,
    required this.senderId,
    required this.type,
    this.contentText,
    this.contentMeta,
    required this.createdAt,
  });

  factory Message.fromJson(Map<String, dynamic> json) => _$MessageFromJson(json);

  Map<String, dynamic> toJson() => _$MessageToJson(this);
}

enum MessageType {
  @JsonValue('TEXT')
  text,
  @JsonValue('IMAGE')
  image,
  @JsonValue('FILE')
  file,
  @JsonValue('AUDIO')
  audio,
  @JsonValue('VIDEO')
  video,
  @JsonValue('SYSTEM')
  system,
}
```

### B. Main Entry Point

```dart
// lib/main.dart
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'features/auth/identity_selector_screen.dart';
import 'core/config/app_config.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(const NexusChatDemo());
}

class NexusChatDemo extends StatelessWidget {
  const NexusChatDemo({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NexusChat Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      home: const IdentitySelectorScreen(),
    );
  }
}
```

---

**End of Documentation**

For questions or issues, refer to:
- API Documentation: `docs/API-FRONTEND.md` and `docs/API-BACKEND.md`
- Backend README: `README.md`
- Flutter PRD: `flutter_prd.md`
