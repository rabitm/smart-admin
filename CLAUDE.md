# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SmartAdmin is a comprehensive enterprise rapid development platform featuring:
- **Dual Frontend Versions**: JavaScript and TypeScript implementations using Vue3 + Ant Design Vue + Vite5 + Pinia
- **Dual Backend Versions**: Java8/SpringBoot2 and Java17/SpringBoot3 with Sa-Token + MyBatis-Plus
- **Mobile App**: UniApp-based cross-platform application (APP/小程序/H5)
- **Multi-Database Support**: Including domestic databases like 达梦、金仓、南大通用 and mainstream databases
- **Security Compliance**: Meets 《网络安全-三级等保》and《数据安全》requirements

## Project Structure

```
smart-admin/
├── smart-admin-api-java17-springboot3/    # Backend API (Java 17 + SpringBoot 3)
│   ├── sa-admin/                          # Main admin module
│   └── sa-base/                           # Base common module  
├── smart-admin-api-java8-springboot2/     # Do not use this project
├── smart-admin-web-javascript/            #  Do not use this project
├── smart-admin-web-typescript/            # Frontend (TypeScript version)
├── smart-app/                             # Do not use this project（ Mobile app (UniApp Vue3)）
└── sql/                                   # Database scripts
```

## Development Commands

### Frontend (TypeScript)

**Development:**

```bash
cd smart-admin-web-typescript
npm run dev              # Start development server (port 8081)
npm run localhost        # Start with localhost mode
```

**Build:**
```bash
npm run build:test      # Build for test environment 
npm run build:pre       # Build for pre-production
npm run build:prod      # Build for production
```

**Code Quality:**
```bash
npm run lint            # Run ESLint (check .eslintrc.cjs)
npm run format          # Run Prettier (check .prettierrc.cjs)
```

### Backend (Java)

**Maven Build:**
```bash
cd smart-admin-api-java17-springboot3
mvn clean compile       # Compile
mvn clean package       # Package
mvn spring-boot:run     # Run application (port 1024)
```

**Multi-Environment Support:**
- dev: Development environment
- test: Test environment  
- pre: Pre-production environment
- prod: Production environment

### Mobile App (UniApp)

```bash
cd smart-app
npm run dev:h5          # H5 development
npm run dev:mp-weixin   # WeChat mini-program
npm run dev:app         # App development
npm run build:h5        # Build H5
npm run build:mp-weixin # Build WeChat mini-program
npm run build:app       # Build App
```

## Architecture & Code Organization

### Backend Architecture (Four-Layer)

1. **Controller**: REST API endpoints and request handling
2. **Service**: Business logic and transaction management
3. **Manager**: Complex business operations and external service integration
4. **DAO**: Data access layer with MyBatis-Plus

### Frontend Architecture
- **Views**: Page components organized by business modules
- **Components**: Reusable UI components
- **API**: HTTP client configuration and API calls
- **Store**: Pinia state management
- **Router**: Vue Router configuration
- **Constants**: Enum constants and configurations
- **Utils**: Utility functions and helpers

### Key Design Patterns

- **Constant Enums**: Uses vue-enum for maintainable constants instead of magic numbers
- **Four-Layer Backend**: Clear separation of concerns with controller/service/manager/dao layers
- **Multi-Environment**: Comprehensive environment configuration for localhost/dev/test/pre/prod
- **Code Generation**: Template-based code generation with online preview
- **Security Integration**: Built-in encryption/decryption, data masking, and access control

### Module Structure

Backend modules are organized under `net.lab1024.sa.admin.module`:
- **business**: Business-specific modules including:
  - **oa.police**: Police Emergency Management System with real-time collaboration
  - **category, goods**: Standard business modules
- **system**: System management (employee, department, role, menu, etc.)
- **support**: Support modules (login logs, operation logs, file management, etc.)

### Police Emergency Management System (v3.27.0+)

A comprehensive real-time police emergency intake system with the following features:
- **Multi-user Collaboration**: Real-time field-level editing with user conflict detection
- **Professional Field Configuration**: Dynamic form rendering based on incident types
- **WebSocket Integration**: Live updates and field locking across multiple clients
- **Smart Location Input**: Integrated location selection with collaboration support
- **Operation History**: Complete audit trail of all user actions and field changes
- **Professional Indicators**: Visual indicators showing who is editing which fields

### Configuration Files
- **Frontend**: `.env.*` files for different environments
- **Backend**: `application.yaml` and `sa-base.yaml` for multi-environment configuration
- **Database**: SQL scripts in `/sql` directory

## Key Dependencies

### Frontend

- Vue 3.4.27 + Composition API
- Ant Design Vue 4.2.5 + Icons
- Vite 5.2.12 (build tool)
- Pinia 2.1.7 (state management)
- Axios 1.6.8 (HTTP client)
- TypeScript 5.6.3 (TS version only)

### Backend

- Spring Boot 3.5.4 (Java 17 version) / 2.x (Java 8 version)
- Sa-Token 1.44.0 (authentication & authorization)
- MyBatis-Plus 3.5.12 (ORM)
- Knife4j/Swagger (API documentation)
- Druid (database connection pool)
- Redisson 3.50.0 (Redis client)

### Mobile App

- UniApp 3.0.0 with Vue 3.2.47
- uni-ui components
- Pinia 2.0.36 for state management

## Development Guidelines

### Code Quality

- Follow the established ESLint and Prettier configurations
- Use the project's naming conventions and code structure patterns
- Maintain the four-layer architecture pattern in backend development
- Use enum constants instead of magic numbers/strings in frontend

### Security Considerations

- The codebase implements enterprise-grade security features
- Never expose sensitive configuration in environment files
- Follow the established encryption/decryption patterns for sensitive data
- Use the built-in data masking features when handling PII

### Testing
- No specific test framework is configured - check with the team for testing strategy
- The codebase focuses on enterprise features rather than extensive unit testing

## Common Development Tasks

### Adding New Business Module
1. Create controller/service/manager/dao layers in backend
2. Add corresponding frontend pages and API calls
3. Update menu and permission configurations
4. Follow the established module structure patterns

### Environment Configuration  
- Frontend: Update appropriate `.env.*` file
- Backend: Modify `application.yaml` or `sa-base.yaml` for the target environment
- Use the multi-environment build commands for proper deployment

### Database Operations
- Use MyBatis-Plus for ORM operations
- Follow the DAO pattern established in existing modules
- Database scripts are maintained in the `/sql` directory

## Advanced Features & Patterns

### Real-time Collaboration System

The police emergency management system implements a sophisticated real-time collaboration framework:

**Frontend Collaboration Components**:
- `field-collaboration-manager.ts`: Core field-level state management
- `global-collaboration-manager.ts`: Global collaboration state and event handling
- `websocket-client.ts`: WebSocket connection management
- `CollaborationFieldIndicator.vue`: Visual field editing indicators
- `CollaborationActivityFeed.vue`: Real-time activity timeline

**Backend Collaboration Services**:
- `CollaborationHistoryService.java`: Operation history tracking
- `PoliceEditLockService.java`: Field locking and conflict resolution
- `CollaborationWebSocketConfig.java`: WebSocket configuration

**Key Collaboration Features**:
- **Field-Level Locking**: Only one user can edit a specific field at a time
- **Visual Indicators**: Real-time display of who is editing which fields
- **Auto-unlock Timers**: Automatic release of locked fields after inactivity
- **Conflict Resolution**: Intelligent handling of simultaneous edits
- **Operation History**: Complete audit trail with user attribution
- **Cross-Client Sync**: Real-time updates across all connected clients

### Dynamic Form Configuration

Professional fields are dynamically configured based on incident types:
- Forms adapt based on selected emergency type (fire, rescue, medical, etc.)
- Field configurations stored in database with API-driven rendering
- Support for various field types: text, select, multi-select, location, etc.
- Real-time form structure synchronization across clients

### Key Technical Patterns

**Vue 3 Composition API**: Modern reactive programming with `ref()`, `computed()`, `watch()`
**WebSocket Integration**: Bi-directional real-time communication with auto-reconnection
**Field State Management**: Reactive field-level state with conflict detection
**Defensive Coding**: Comprehensive null checks and error handling
**Memory Management**: Proper cleanup of event listeners and timers

## Troubleshooting & Known Issues

### Data Loss Prevention
A critical fix was implemented to prevent professional field data loss during multi-user collaboration:
- **Issue**: Data would disappear when switching incident types during collaborative editing
- **Root Cause**: `clearDynamicFields()` was called before data reload in `loadEditData()`
- **Solution**: Reordered execution and added skip flags for edit-mode loading

### Performance Considerations
- Debounced field updates (500ms) to reduce server load
- Efficient field state diffing to minimize unnecessary updates
- Auto-cleanup of stale collaboration sessions
- Connection pooling for WebSocket management

## Development Tips

### Working with Collaboration Features
1. Always test multi-user scenarios with multiple browser tabs
2. Use browser dev tools to monitor WebSocket messages
3. Check console logs for collaboration debug information
4. Test field locking behavior across different user sessions

### Debugging Collaboration Issues
- Enable debug logs with `console.log` statements prefixed with emoji identifiers
- Monitor WebSocket connection status in browser dev tools
- Check user ID consistency across different authentication methods
- Verify field state synchronization using Vue DevTools

## Real-time Collaboration System Architecture & Development Guide

### System Overview

The SmartAdmin police emergency management system features a comprehensive real-time collaboration framework supporting 200-500 concurrent users with field-level editing, conflict resolution, and high-performance synchronization.

### Core Architecture Components

#### 1. Frontend Real-time Architecture

**WebSocket Management Layer**:
```typescript
// Primary WebSocket client with unified messaging
unified-websocket-client.ts           // Core WebSocket client implementation
websocket-manager.ts                  // Connection pool and lifecycle management
high-performance-websocket-pool.ts    // High-concurrency connection pooling (NEW)
```

**Collaboration Management Layer**:
```typescript
// Field-level collaboration
field-collaboration-manager.ts        // Field locking and state management
global-collaboration-manager.ts       // Global collaboration state coordination
simple-field-lock-manager.ts         // Simplified field locking for basic use cases

// List and data synchronization
police-list-update-manager.ts         // High-performance list synchronization
performance-monitor.ts                // Performance monitoring and degradation (NEW)
```

**Service Integration Layer**:
```typescript
// WebSocket service abstractions
police-websocket.service.ts           // Police-specific WebSocket operations
collaboration-websocket.service.ts    // Generic collaboration WebSocket services
```

#### 2. Backend Real-time Architecture

**WebSocket Infrastructure**:
```java
// Core WebSocket configuration
WebSocketConfig.java                  // Basic WebSocket setup
HighPerformanceRedisConfig.java      // Optimized Redis configuration for high concurrency

// Message handling and transport
WebSocketMessage.java                 // Unified message structure
WebSocketTransport.java              // Message transport layer
WebSocketSessionManager.java         // Session lifecycle management
```

**Police-specific Services**:
```java
// High-performance list updates
PoliceListUpdateService.java          // Optimized for 200-500 concurrent updates
PoliceReportService.java             // Enhanced with batch processing
PoliceWebSocketHandler.java          // Police-specific WebSocket message routing

// Collaboration services
SyncService.java                     // Field synchronization coordination
WebSocketSyncServiceImpl.java       // WebSocket-based sync implementation
```

### Development Guidelines & Standards

#### 1. WebSocket Message Standards

**Message Structure**:
```typescript
interface WebSocketMessage {
  messageId: string;              // Unique message identifier
  type: string;                   // Message type (FIELD_EDIT, LIST_UPDATE, etc.)
  module: string;                 // Module namespace (police, collaboration, system)
  data: any;                      // Business payload
  timestamp: string;              // ISO timestamp
  fromUserId?: number;            // Sender user ID
  fromUserName?: string;          // Sender user name
}
```

**Business Message Types**:
```typescript
// Police module messages
'FIELD_EDIT'          // Field editing operations
'FIELD_FOCUS'         // Field focus events
'FIELD_BLUR'          // Field blur events
'LIST_UPDATE'         // List synchronization updates
'REPORT_UPDATE'       // Report-level updates
'USER_JOIN'           // User joining collaboration
'USER_LEAVE'          // User leaving collaboration

// System module messages
'CONNECTED'           // Connection established
'PING' / 'PONG'       // Heartbeat messages
'SUBSCRIBE_ACK'       // Subscription confirmations
'ERROR'               // Error notifications
```

#### 2. Field Collaboration Implementation Pattern

**Frontend Field Collaboration Setup**:
```typescript
// 1. Initialize field collaboration manager
const fieldCollaboration = useFieldCollaborationManager();

// 2. Register field for collaboration
const handleFieldFocus = (fieldName: string) => {
  fieldCollaboration.onFieldFocus(fieldName, currentUser);
};

const handleFieldBlur = (fieldName: string) => {
  fieldCollaboration.onFieldBlur(fieldName, currentUser);
};

const handleFieldEdit = (fieldName: string, value: any) => {
  fieldCollaboration.onFieldEdit(fieldName, value, currentUser);
};

// 3. Listen for collaboration state changes
fieldCollaboration.onFieldStateChange((fieldName, state) => {
  // Update UI based on field collaboration state
  updateFieldIndicators(fieldName, state);
});
```

**Backend Field Sync Implementation**:
```java
// Service method for field synchronization
@Async
public void syncFieldUpdate(Long reportId, Long userId, String userName,
                           String fieldName, String fieldValue, String operationType) {

    // 1. Validate and sanitize input
    if (reportId == null || StringUtils.isBlank(fieldName)) {
        log.warn("Invalid sync request: reportId={}, fieldName={}", reportId, fieldName);
        return;
    }

    // 2. Create update message
    Map<String, Object> updateData = new HashMap<>();
    updateData.put("reportId", reportId);
    updateData.put("fieldName", fieldName);
    updateData.put("fieldValue", fieldValue);
    updateData.put("userId", userId);
    updateData.put("userName", userName);
    updateData.put("timestamp", System.currentTimeMillis());

    // 3. Broadcast to all subscribers
    webSocketTransport.broadcast("police", "FIELD_EDIT", updateData);

    // 4. Update database (if needed)
    if (shouldPersistField(fieldName)) {
        updateDatabase(reportId, fieldName, fieldValue);
    }
}
```

#### 3. High-Performance List Synchronization

**Frontend List Update Handler**:
```typescript
// Enhanced list update processing with performance optimizations
const handleWebSocketListUpdate = (data: any) => {
  switch (data.type) {
    case 'UPDATE':
      // Single field update
      if (data.reportId && data.data) {
        const fieldName = Object.keys(data.data)[0];
        const fieldValue = Object.values(data.data)[0];

        handleHighPerformanceFieldUpdate({
          reportId: data.reportId,
          fieldName,
          fieldValue,
          userName: data.userName || '其他用户'
        });
      }
      break;

    case 'BATCH':
      // Batch updates - NEW: Enhanced for high concurrency
      console.log('📦 [列表更新] 处理批量更新:', data);
      handleWebSocketBatchUpdate(data);
      break;

    case 'INSERT':
      // New record insertion
      if (data.reportId && data.data) {
        handleHighPerformanceRecordInsert({ record: data.data });
      }
      break;

    case 'DELETE':
      // Record deletion
      if (data.reportId) {
        handleHighPerformanceRecordDelete({ reportId: data.reportId });
      }
      break;
  }
};
```

**Backend High-Concurrency Broadcast**:
```java
// Optimized broadcast method for 200-500 concurrent users
private void broadcastListUpdate(Map<String, Object> updateData) {
    // Circuit breaker protection
    if (circuitBreakerOpen) {
        droppedUpdates.incrementAndGet();
        log.warn("🔥 [熔断器] 广播被熔断器阻止，丢弃更新");
        return;
    }

    Set<String> subscribers = listSubscribers.getOrDefault("all", new HashSet<>());
    if (subscribers.isEmpty()) return;

    // Rate limiting
    if (!broadcastSemaphore.tryAcquire(10, TimeUnit.MILLISECONDS)) {
        droppedUpdates.incrementAndGet();
        log.warn("📊 [限流] 广播队列已满，丢弃更新");
        return;
    }

    try {
        // Batch subscribers into groups of 100 for parallel processing
        List<List<String>> subscriberBatches = partition(new ArrayList<>(subscribers), 100);

        // Parallel broadcast with timeout
        List<CompletableFuture<Void>> batchTasks = subscriberBatches.stream()
            .map(batch -> CompletableFuture.runAsync(() -> sendToBatch(batch, message), broadcastExecutor))
            .collect(Collectors.toList());

        int timeoutMs = Math.min(Math.max(subscribers.size() / 10, 50), 500);
        CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0]))
            .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .get();

    } finally {
        broadcastSemaphore.release();
    }
}
```

#### 4. Performance Optimization Standards

**Frontend Performance Guidelines**:
```typescript
// Use intelligent batching for field updates
const batchSyncManager = {
  pendingUpdates: new Map<string, any>(),
  batchTimeout: 200, // 200ms batch interval

  addUpdate(fieldName: string, fieldValue: any) {
    this.pendingUpdates.set(fieldName, fieldValue);
    this.scheduleBatchSync();
  },

  async processBatch() {
    // Rate limiting check
    const now = Date.now();
    if (now - this.lastSyncTime < 100) {
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    // Parallel processing with error handling
    const promises = Array.from(this.pendingUpdates.entries()).map(async ([fieldName, fieldValue]) => {
      try {
        await policeReportApi.syncFieldUpdate(reportId, fieldName, fieldValue);
      } catch (error) {
        console.error(`❌ [批量同步] 字段 ${fieldName} 同步失败:`, error);
        // Re-queue failed updates
        this.pendingUpdates.set(fieldName, fieldValue);
      }
    });

    await Promise.allSettled(promises);
  }
};
```

**Backend Performance Guidelines**:
```java
// Batch processing with automatic queue management
private final Map<Long, Map<String, Object>> batchUpdateQueue = new ConcurrentHashMap<>();
private static final int BATCH_INTERVAL_MS = 100;

@Async
public void processBatchUpdates(Long userId, String userName) {
    Map<Long, Map<String, Object>> currentBatch = new HashMap<>(batchUpdateQueue);
    batchUpdateQueue.clear();

    // Parallel processing with timeout control
    List<CompletableFuture<Void>> futures = currentBatch.entrySet().stream()
        .map(entry -> CompletableFuture.runAsync(() -> {
            processReportUpdate(entry.getKey(), entry.getValue(), userId, userName);
        }))
        .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .orTimeout(5, TimeUnit.SECONDS)
        .get();
}
```

### Key File Locations & Responsibilities

#### Frontend Files (TypeScript)
```
smart-admin-web-typescript/src/
├── utils/
│   ├── unified-websocket-client.ts           # Core WebSocket client
│   ├── high-performance-websocket-pool.ts    # Connection pooling
│   ├── police-list-update-manager.ts         # List synchronization
│   ├── field-collaboration-manager.ts        # Field collaboration
│   └── performance-monitor.ts                # Performance monitoring
├── services/
│   ├── police-websocket.service.ts           # Police WebSocket service
│   └── collaboration-websocket.service.ts    # Collaboration service
└── views/business/oa/police/
    ├── emergency-intake.vue                  # Main emergency form
    ├── police-report-list.vue               # High-performance list
    └── components/
        └── CollaborationFieldIndicator.vue   # Field collaboration UI
```

#### Backend Files (Java)
```
sa-admin/src/main/java/net/lab1024/sa/admin/
├── config/
│   └── HighPerformanceRedisConfig.java      # Optimized Redis config
├── module/business/oa/police/
│   ├── controller/PoliceReportController.java    # REST endpoints
│   ├── service/
│   │   ├── PoliceReportService.java          # Enhanced with batching
│   │   ├── PoliceListUpdateService.java      # High-concurrency updates
│   │   └── sync/SyncService.java             # Synchronization coordination
│   └── websocket/PoliceWebSocketHandler.java     # WebSocket message routing
└── module/support/websocket/
    ├── WebSocketSessionManager.java          # Session management
    └── service/impl/WebSocketTransport.java  # Message transport
```

### Development Workflow Standards

#### 1. Adding Real-time Features
```typescript
// Step 1: Define message types
export enum PoliceMessageType {
  FIELD_EDIT = 'FIELD_EDIT',
  USER_JOIN = 'USER_JOIN',
  // ... other types
}

// Step 2: Implement frontend handler
const handleNewMessageType = (message: WebSocketMessage) => {
  // Process message
  // Update UI state
  // Emit events if needed
};

// Step 3: Register message handler
wsClient.onModuleMessage('police', PoliceMessageType.FIELD_EDIT, handleNewMessageType);
```

```java
// Step 4: Implement backend handler
@Component
public class CustomWebSocketHandler {

    @EventListener
    public void handleCustomMessage(WebSocketMessageEvent event) {
        if ("CUSTOM_TYPE".equals(event.getType())) {
            // Process message
            // Update database if needed
            // Broadcast to subscribers
        }
    }
}
```

#### 2. Performance Testing Guidelines
```typescript
// Enable performance monitoring in development
const monitor = getPerformanceMonitor();
monitor.start();

monitor.on('level_changed', (oldLevel, newLevel, step) => {
  console.log(`🔄 性能级别变更: ${oldLevel} -> ${newLevel} (${step.name})`);
});

monitor.on('degradation_action', (action, level) => {
  console.log(`📋 降级动作: ${action} (级别: ${level})`);
});
```

#### 3. Error Handling Standards
```typescript
// Frontend error handling with retry logic
const handleWebSocketError = async (error: Error, retryCount = 0) => {
  console.error('❌ WebSocket错误:', error);

  if (retryCount < 3) {
    console.log(`🔄 重试连接 (${retryCount + 1}/3)`);
    await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, retryCount)));
    return connectWebSocket().catch(err => handleWebSocketError(err, retryCount + 1));
  }

  // Fallback to polling mode
  enablePollingFallback();
};
```

```java
// Backend error handling with circuit breaker
@Async
public void handleMessageWithCircuitBreaker(WebSocketMessage message) {
    if (circuitBreakerOpen) {
        log.warn("⚡ 熔断器开启，消息被丢弃");
        return;
    }

    try {
        processMessage(message);
        resetCircuitBreakerCounter();
    } catch (Exception e) {
        log.error("❌ 消息处理失败", e);
        incrementCircuitBreakerCounter();

        if (getCircuitBreakerCounter() > THRESHOLD) {
            openCircuitBreaker();
        }
    }
}
```

### Important Implementation Notes

1. **Always use modular WebSocket message handling**: Use `wsClient.onModuleMessage()` instead of direct event listeners
2. **Implement proper cleanup**: Always clean up timers, listeners, and resources in component unmount handlers
3. **Use batch processing**: Batch database operations and WebSocket broadcasts for high-concurrency scenarios
4. **Monitor performance**: Implement performance monitoring for production deployments
5. **Handle failures gracefully**: Use circuit breakers, retry logic, and fallback mechanisms
6. **Test multi-user scenarios**: Always test collaboration features with multiple concurrent users
7. **Follow the emoji logging convention**: Use emoji prefixes for easy log filtering and debugging