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