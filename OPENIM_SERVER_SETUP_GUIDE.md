# OpenIM Server Setup Guide

## 📅 Date
2025-10-09

## 🎯 Issue Description

When attempting to send messages or create groups, the following error occurs:

```
Connection refused: no further information
POST request for "http://localhost:10002/auth/user_token"
```

**Root Cause**: OpenIM server is not running on `localhost:10002`

---

## 🔧 Solution: Start OpenIM Server

### Option 1: Docker Compose (Recommended)

If you have OpenIM deployed with Docker Compose:

```bash
cd /path/to/openim
docker-compose up -d
```

Verify containers are running:
```bash
docker-compose ps
```

Expected output should show OpenIM API server running on port 10002.

### Option 2: Local Development

If running OpenIM directly:

```bash
cd /path/to/openim
./scripts/start-all.sh
```

Or start the API server specifically:
```bash
./scripts/start-api.sh
```

### Option 3: Check Configuration

Verify OpenIM configuration matches your SmartAdmin settings:

**SmartAdmin Configuration Location:**
- File: `smart-admin-api-java17-springboot3/sa-base/src/main/resources/application.yaml`

Expected configuration:
```yaml
openim:
  api-url: http://localhost:10002
  admin:
    user-id: imAdmin
    secret: openIM123
```

---

## ✅ Verification Steps

### 1. Check OpenIM Server Status

```bash
# Check if port 10002 is listening
netstat -an | findstr 10002
```

Expected output:
```
TCP    0.0.0.0:10002    0.0.0.0:0    LISTENING
```

### 2. Test OpenIM API Directly

Using curl or Postman:

```bash
curl -X POST http://localhost:10002/auth/user_token \
  -H "Content-Type: application/json" \
  -d '{
    "userID": "imAdmin",
    "platformID": 10,
    "secret": "openIM123"
  }'
```

Expected response (200 OK):
```json
{
  "errCode": 0,
  "errMsg": "",
  "data": {
    "token": "eyJhbGciOiJIUzI1...",
    "expireTimeSeconds": 604800
  }
}
```

### 3. Verify SmartAdmin Connection

After OpenIM server is running, test the connection from SmartAdmin:

1. Restart SmartAdmin backend
2. Navigate to any police report detail page
3. Click "即时聊天" (Instant Chat) tab
4. Try sending a message

Expected behavior:
- ✅ Group auto-creation succeeds
- ✅ Message sends successfully
- ✅ No "Connection refused" errors in logs

---

## 📊 OpenIM Server Requirements

### System Requirements
- **Memory**: Minimum 2GB RAM
- **CPU**: 2+ cores recommended
- **Ports**:
  - 10002 (API Server)
  - 10003 (WebSocket)
  - 10004 (Admin API)
  - 27017 (MongoDB)
  - 6379 (Redis)

### Dependencies
- MongoDB (for message storage)
- Redis (for caching and pub/sub)
- Kafka (optional, for message queue)

---

## 🐛 Common Issues & Solutions

### Issue 1: Port Already in Use

**Symptom**:
```
Error: bind: address already in use
```

**Solution**:
```bash
# Find process using port 10002
netstat -ano | findstr 10002

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### Issue 2: MongoDB Connection Failed

**Symptom**:
```
Error connecting to MongoDB: connection refused
```

**Solution**:
```bash
# Start MongoDB
mongod --dbpath /path/to/data

# Or with Docker
docker run -d -p 27017:27017 mongo:latest
```

### Issue 3: Redis Connection Failed

**Symptom**:
```
Error connecting to Redis: connection refused
```

**Solution**:
```bash
# Start Redis
redis-server

# Or with Docker
docker run -d -p 6379:6379 redis:latest
```

---

## 🔄 Alternative: Mock Mode (Development Only)

If you need to develop without OpenIM server, you can implement a mock mode:

### 1. Create Mock Configuration

Add to `application.yaml`:
```yaml
openim:
  mock-mode: true  # Enable mock mode for development
```

### 2. Implement Mock Client

Create `MockOpenIMClient.java`:
```java
@Component
@ConditionalOnProperty(name = "openim.mock-mode", havingValue = "true")
public class MockOpenIMClient {

    public String mockUserToken() {
        return "mock-token-" + UUID.randomUUID();
    }

    public String mockCreateGroup(String groupName) {
        return "mock-group-" + UUID.randomUUID();
    }

    public String mockSendMessage(String groupId, String content) {
        return "mock-msg-" + UUID.randomUUID();
    }
}
```

**⚠️ Warning**: Mock mode should ONLY be used in development. Production must use real OpenIM server.

---

## 📚 OpenIM Installation Guide

### Quick Start with Docker

1. **Clone OpenIM Repository**:
```bash
git clone https://github.com/OpenIMSDK/Open-IM-Server.git
cd Open-IM-Server
```

2. **Configure Environment**:
```bash
cp .env.example .env
# Edit .env to set your configuration
```

3. **Start Services**:
```bash
docker-compose up -d
```

4. **Verify Installation**:
```bash
curl http://localhost:10002/manager/account/login
```

### Manual Installation

See official documentation:
- English: https://docs.openim.io/
- Chinese: https://doc.rentsoft.cn/

---

## 🎯 Integration Status

### Current SmartAdmin Integration

**Files Modified for OpenIM Integration**:

1. **Backend**:
   - `IMMessageService.java` - Message sending with auto-group creation
   - `IMGroupManagementService.java` - Group management
   - `IMUserSyncService.java` - User synchronization
   - `OpenIMClient.java` - HTTP client for OpenIM API
   - `OpenIMTokenManager.java` - Token management

2. **Frontend**:
   - `ChatPanel.vue` - Chat interface
   - `im-websocket.service.ts` - WebSocket integration
   - `im-api.ts` - API client

### Features Implemented
- ✅ Auto user synchronization
- ✅ Auto group creation on first message
- ✅ Real-time messaging via WebSocket
- ✅ Message history loading
- ✅ Connection status monitoring

### Features Pending OpenIM Server
- ⏸️ Actual message delivery (requires OpenIM running)
- ⏸️ Message persistence (requires MongoDB)
- ⏸️ User presence (requires Redis)
- ⏸️ Push notifications (requires Kafka)

---

## 🆘 Support

### OpenIM Community
- GitHub: https://github.com/OpenIMSDK/Open-IM-Server
- Discord: https://discord.gg/openim
- Forum: https://forum.openim.io/

### SmartAdmin Support
- GitHub Issues: https://github.com/1024-lab/smart-admin/issues
- Documentation: https://smartadmin.vip/

---

## 📝 Next Steps

1. **Install and start OpenIM server** using one of the methods above
2. **Verify connection** using the verification steps
3. **Test messaging functionality** in SmartAdmin
4. **Monitor logs** for any errors

Once OpenIM server is running, the auto-group creation and messaging features will work seamlessly!

---

**Document Created By**: Claude Code Assistant
**Version**: v3.27.0+
**Last Updated**: 2025-10-09 19:15
**Status**: ⚠️ OpenIM Server Required
