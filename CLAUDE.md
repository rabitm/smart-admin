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
- **business**: Business-specific modules (category, goods, etc.)
- **system**: System management (employee, department, role, menu, etc.) 
- **support**: Support modules (login logs, operation logs, file management, etc.)

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