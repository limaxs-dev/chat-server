# 📘 NexusChat Engine: Master Technical Specification (PTDD)

**Version:** 1.5 (Production & GraalVM Ready)

**Architecture:** Hybrid Reactive-Imperative with Java 21 Virtual Threads

**License:** Agnostic Tenant Support (B2B)

---

## Arsitektur Paket & Responsibilitas (Project Structure)

Untuk menjaga kode tetap bersih dalam mode Hybrid, struktur paket dibagi berdasarkan domain dan metode akses data.

```text
src/main/java/tech/limaxs/chat/
├── api/
│   ├── rest/                # HTTP Endpoints (Imperative + Virtual Threads)
│   │   ├── dto/             # Request/Response POJO khusus REST
│   │   └── resource/        # JAX-RS Resources (@RunOnVirtualThread)
│   └── websocket/           # WebSocket Handlers (Reactive Next)
│       ├── codec/           # JSON Encoders/Decoders (Jackson based)
│       ├── dto/             # WebSocket Envelope & Event Payload
│       └── handler/         # @ServerEndpoint logic & WebRTC Signaling
├── core/
│   ├── model/               # JPA Entities (Shared POJO, Hibernate & Reactive)
│   ├── repository/          # Persistence Layer (Separated Pools)
│   │   ├── imperative/      # Repository via EntityManager (JDBC)
│   │   └── reactive/        # Repository via Mutiny (Reactive)
│   └── service/             # Business Logic & Orchestration
├── infra/
│   ├── auth/                # RSA Key Store & JWT Validation Logic
│   ├── config/              # Quarkus Producers & Native Reflection Config
│   ├── redis/               # Pub/Sub Manager & Presence Logic
│   └── storage/             # MinIO Integration & File Services
└── util/  

## 1. Arsitektur Sistem & Concurrency Model

Sistem ini menggunakan pendekatan **Dual-Engine** untuk memaksimalkan kapabilitas Java 21 dan Quarkus 3.30:

1. **Engine REST (Imperative):** Menggunakan `@RunOnVirtualThread` + `Hibernate ORM`. Digunakan untuk transaksi database yang kompleks dan bersifat "heavy read/write" (History, Room Management).
2. **Engine WebSocket (Reactive):** Menggunakan `WebSockets Next` + `Hibernate Reactive`. Digunakan untuk pengiriman pesan instan, presence, dan WebRTC signaling.
3. **Cross-Node Sync:** Menggunakan **Redis Pub/Sub** untuk menjamin pesan sampai ke user meskipun mereka terhubung ke instance server yang berbeda.

---

## 2. Keamanan B2B (Multi-Tenant RSA JWT)

Sistem bersifat *stateless* terhadap *user service* pihak ketiga.

* **Public Key Management:** Setiap tenant mendaftarkan Public Key RSA. Chat server memvalidasi JWT berdasarkan `iss` (Issuer) yang ada di header token.
* **Authentication Flow:**
* Client membawa JWT di header `Authorization: Bearer <token>`.
* `smallrye-jwt` memverifikasi integritas token.
* `user_id` dan `tenant_id` diekstrak dari klaim JWT (`sub` dan `iss`).



---

## 3. Skema Database (PostgreSQL)

Desain ini mendukung isolasi data per tenant dan performa pagination yang optimal.

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: chat_users (Data minimal untuk relasi)
CREATE TABLE chat_users (
    id UUID PRIMARY KEY, -- ID disinkronkan dari Client Service
    name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: rooms
CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(10) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(255),
    last_message_preview TEXT,
    tenant_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_rooms_tenant_updated_at ON rooms (tenant_id, updated_at DESC);

-- Table: room_participants
CREATE TABLE room_participants (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES chat_users(id),
    role VARCHAR(10) NOT NULL CHECK (role IN ('MEMBER', 'ADMIN')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

-- Table: messages (Hot Storage)
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES chat_users(id),
    type VARCHAR(15) NOT NULL CHECK (type IN ('TEXT', 'IMAGE', 'FILE', 'AUDIO', 'VIDEO', 'SYSTEM', 'VOICE_CALL')),
    content_text TEXT,
    content_meta JSONB, 
    client_ref UUID UNIQUE, -- Idempotency Key
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ
);
CREATE INDEX idx_messages_room_history ON messages(room_id, created_at DESC);

-- Table: room_read_states
CREATE TABLE room_read_states (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES chat_users(id),
    last_read_message_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);

-- Table: files (MinIO Metadata)
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploader_id UUID NOT NULL REFERENCES chat_users(id),
    object_key VARCHAR(512) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    mime VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

```

---

## 4. Spesifikasi Redis (State & Pub/Sub)

Untuk performa tinggi, data ephemeral disimpan di Redis:

* **Presence:** `user:presence:{user_id}` → Value: `online`, TTL: 60s.
* **Unread Count:** `unread:{room_id}:{user_id}` → Value: `Integer` (Increment on new message).
* **Pub/Sub Channels:**
* `chat:room:{room_id}`: Untuk pesan baru, edit, dan delete.
* `signal:user:{user_id}`: Khusus WebRTC signaling (P2P).
* `typing:room:{room_id}`: Indikator mengetik.



---

## 5. Interface Spesifikasi (REST & WebSocket)

### 5.1 OpenAPI / Swagger

Dokumen API dihasilkan otomatis melalui `quarkus-openapi-generator`. Endpoint tersedia di `/swagger-ui`.

### 5.2 WebSocket Event Contract

Setiap payload dikirim dalam *Envelope JSON*:

```json
{
  "event": "SEND_MSG | SIGNAL_SDP | TYPING | ACK",
  "traceId": "uuid",
  "data": { ... }
}

```

**Payload WebRTC Signaling:**

```json
{
  "event": "SIGNAL_SDP",
  "data": {
    "target_id": "uuid",
    "type": "OFFER | ANSWER",
    "sdp": "v=0..."
  }
}

```

---

## 6. Strategi Implementasi GraalVM (Native Friendly)

Agar sistem dapat dikompilasi menjadi Native Image tanpa error:

1. **Repository Pattern:** Menggunakan repository terpisah untuk ORM dan Reactive. Menghindari *dynamic bytecode enhancement* yang kompleks pada level Entity.
2. **Reflection Registration:** Semua DTO dan Payload wajib dianotasi dengan `@RegisterForReflection`.
3. **No Dynamic Queries:** Gunakan Named Queries atau Panache built-in methods untuk menghindari refleksi runtime yang berlebihan.

---

## 7. Operasional & Mitigasi

| Fitur | Strategi |
| --- | --- |
| **Zombie Call** | WebSocket PING/PONG. Timeout 30s otomatis trigger `CALL_END`. |
| **Dual Pool** | Limitasi `max-size` koneksi DB (misal: JDBC 20, Reactive 30). |
| **Cleanup** | Scheduler harian menghapus file `is_confirmed = false` di MinIO & DB. |
| **Scaling** | Stateless app nodes dengan Redis sebagai *source of truth* untuk session routing. |

---

## 8. Konfigurasi Proyek 

### 8.1 build.gradle 

Pastikan menggunakan Java 21 dan Quarkus 3.30+. Dependency ini sudah diatur agar mendukung mode Hybrid.

```groovy
plugins {
    id 'java'
    id 'io.quarkus'
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
    implementation enforcedPlatform("${quarkusPlatformGroupId}:quarkus-camel-bom:${quarkusPlatformVersion}")
    implementation 'io.quarkus:quarkus-rest'
    implementation 'io.quarkus:quarkus-hibernate-reactive-panache'
    implementation 'io.quarkus:quarkus-websockets-next'
    implementation 'io.quarkus:quarkus-rest-jackson'
    implementation 'io.quarkus:quarkus-smallrye-fault-tolerance'
    implementation 'io.quarkus:quarkus-hibernate-orm-panache'
    implementation 'io.quarkus:quarkus-smallrye-jwt'
    implementation "io.quarkiverse.openapi.generator:quarkus-openapi-generator:2.13.0-lts"
    implementation "io.quarkiverse.openapi.generator:quarkus-openapi-generator-server:2.13.0-lts"
    implementation 'io.quarkus:quarkus-reactive-pg-client'
    implementation 'io.quarkus:quarkus-smallrye-jwt-build'
    implementation "io.quarkus:quarkus-redis-client"
    implementation 'org.apache.camel.quarkus:camel-quarkus-mapstruct'
    implementation 'io.quarkus:quarkus-arc'
    implementation 'io.quarkus:quarkus-hibernate-orm'
    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
}

group = 'tech.limaxs.chat'
version = '1.0.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

test {
    systemProperty "java.util.logging.manager", "org.jboss.logmanager.LogManager"
    jvmArgs "--add-opens", "java.base/java.lang=ALL-UNNAMED"
}
compileJava {
    options.encoding = 'UTF-8'
    options.compilerArgs << '-parameters'
}

compileTestJava {
    options.encoding = 'UTF-8'
}


```

---

### 8.2 application.properties (Hybrid Config)

Kunci dari arsitektur ini adalah pemisahan pool koneksi namun mengarah ke database yang sama.

```properties
# Project Info
quarkus.application.name=nexus-chat-engine
quarkus.http.port=8080

# --- Virtual Threads ---
quarkus.rest.blocking-default-level=VIRTUAL_THREAD

# --- Database: JDBC (Untuk REST / Hibernate ORM) ---
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=password
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/nexuschat
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.min-size=5

# --- Database: Reactive (Untuk WebSockets) ---
quarkus.datasource.reactive.url=postgresql://localhost:5432/nexuschat
quarkus.datasource.reactive.max-size=30

# --- Hibernate Config ---
# Gunakan 'update' untuk dev, 'validate' untuk production
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-reactive.database.generation=none

# --- Redis Config (Pub/Sub & Cache) ---
quarkus.redis.hosts=redis://localhost:6379
quarkus.redis.timeout=10s

# --- Security: Smallrye JWT (RSA) ---
# Lokasi public key untuk verifikasi signature dari tenant
quarkus.smallrye-jwt.enabled=true
quarkus.smallrye-jwt.auth-mechanism=bearer
# Path ke public key (bisa diarahkan ke folder spesifik per tenant di logic code)
smallrye.jwt.verify.key.location=keys/public-key.pem

# --- OpenAPI & Swagger ---
quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/swagger-ui
quarkus.smallrye-openapi.path=/openapi
quarkus.smallrye-openapi.security-scheme=jwt

# --- Logging ---
quarkus.log.level=INFO
quarkus.log.category."com.nexuschat".level=DEBUG

```

---

## 9. Penutup & Instruksi AI Agent

Dokumen ini sekarang telah mencakup seluruh spektrum pengembangan:

1. **Visi Produk:** Chat engine agnostik dengan WebRTC.
2. **Detail Teknis:** Skema SQL, Event WebSocket, dan Alur Signaling.
3. **Implementasi:** `build.gradle` dan `application.properties`.

**Panduan Implementasi untuk AI Agent:**

* Gunakan **Repository Pattern** untuk memisahkan logic `Mutiny` (Reactive) dan `EntityManager` (Blocking).
* Implementasikan `WebSocketNext` handler dengan `@RunOnVirtualThread` jika di dalamnya terdapat pemanggilan repository blocking, atau gunakan full reactive jika menggunakan reactive repository.
* Gunakan `RedisDataSource` untuk melakukan `publish` dan `subscribe` pada event chat.