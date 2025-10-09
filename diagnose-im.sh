#!/bin/bash

# IM连接问题快速诊断脚本
# 使用方法: bash diagnose-im.sh

echo "=================================="
echo "IM连接问题快速诊断工具"
echo "=================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 检查后端服务
echo "1️⃣  检查SmartAdmin后端服务..."
if netstat -ano 2>/dev/null | grep -q ":1024.*LISTENING" || netstat -tuln 2>/dev/null | grep -q ":1024"; then
    echo -e "${GREEN}✅ 后端服务运行中 (端口1024)${NC}"
else
    echo -e "${RED}❌ 后端服务未运行${NC}"
    echo "   请启动后端: cd smart-admin-api-java17-springboot3 && mvn spring-boot:run"
    exit 1
fi

# 2. 检查OpenIM服务
echo ""
echo "2️⃣  检查OpenIM服务..."

# 检查Docker容器
if command -v docker &> /dev/null; then
    if docker ps 2>/dev/null | grep -q "openim-server"; then
        echo -e "${GREEN}✅ OpenIM Server容器运行中${NC}"

        # 检查端口
        if netstat -ano 2>/dev/null | grep -q ":10002.*LISTENING" || netstat -tuln 2>/dev/null | grep -q ":10002"; then
            echo -e "${GREEN}✅ OpenIM API端口监听中 (10002)${NC}"
        else
            echo -e "${YELLOW}⚠️  端口10002未监听${NC}"
        fi

        if netstat -ano 2>/dev/null | grep -q ":10001.*LISTENING" || netstat -tuln 2>/dev/null | grep -q ":10001"; then
            echo -e "${GREEN}✅ OpenIM WebSocket端口监听中 (10001)${NC}"
        else
            echo -e "${YELLOW}⚠️  端口10001未监听${NC}"
        fi
    else
        echo -e "${RED}❌ OpenIM Server容器未运行${NC}"
        echo "   请启动OpenIM: docker start openim-server openim-chat"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  Docker未安装,无法检查OpenIM容器${NC}"
fi

# 3. 检查后端配置
echo ""
echo "3️⃣  检查后端OpenIM配置..."

CONFIG_FILE="smart-admin-api-java17-springboot3/sa-base/target/classes/sa-base.yaml"
if [ -f "$CONFIG_FILE" ]; then
    if grep -q "^openim:" "$CONFIG_FILE"; then
        echo -e "${GREEN}✅ OpenIM配置存在${NC}"

        # 提取配置信息
        API_URL=$(grep "api-url:" "$CONFIG_FILE" | head -1 | awk '{print $2}')
        WS_URL=$(grep "ws-url:" "$CONFIG_FILE" | head -1 | awk '{print $2}')

        echo "   API URL: $API_URL"
        echo "   WS URL: $WS_URL"
    else
        echo -e "${RED}❌ OpenIM配置不存在${NC}"
        echo "   请检查 sa-base/src/main/resources/dev/sa-base.yaml"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  配置文件未编译,请运行: mvn clean compile${NC}"
fi

# 4. 测试OpenIM连通性
echo ""
echo "4️⃣  测试OpenIM服务连通性..."

# 尝试获取管理员Token
ADMIN_TOKEN_RESPONSE=$(curl -s -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{"secret":"openIM123"}' 2>/dev/null)

if echo "$ADMIN_TOKEN_RESPONSE" | grep -q '"errCode":0'; then
    echo -e "${GREEN}✅ OpenIM管理员Token获取成功${NC}"

    # 提取token
    ADMIN_TOKEN=$(echo "$ADMIN_TOKEN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "   Token: ${ADMIN_TOKEN:0:20}..."
else
    echo -e "${RED}❌ OpenIM管理员Token获取失败${NC}"
    echo "   响应: $ADMIN_TOKEN_RESPONSE"
fi

# 5. 检查测试用户是否存在
echo ""
echo "5️⃣  检查OpenIM测试用户..."

if [ -n "$ADMIN_TOKEN" ]; then
    USER_CHECK=$(curl -s -X POST http://localhost:10002/user/get_users \
      -H "Content-Type: application/json" \
      -H "token: $ADMIN_TOKEN" \
      -d '{"userIDs":["1"]}' 2>/dev/null)

    if echo "$USER_CHECK" | grep -q '"userID":"1"'; then
        echo -e "${GREEN}✅ 用户ID=1已注册${NC}"
    else
        echo -e "${YELLOW}⚠️  用户ID=1未注册${NC}"
        echo ""
        echo "📝 执行以下命令注册用户:"
        echo ""
        echo "curl -X POST http://localhost:10002/user/user_register \\"
        echo "  -H 'Content-Type: application/json' \\"
        echo "  -H 'token: $ADMIN_TOKEN' \\"
        echo "  -d '{\"secret\":\"openIM123\",\"users\":[{\"userID\":\"1\",\"nickname\":\"系统管理员\"}]}'"
        echo ""
    fi
fi

# 6. 检查后端Bean加载
echo ""
echo "6️⃣  检查后端IM Bean加载..."

LOG_DIR="smart-admin-api-java17-springboot3/logs"
if [ -d "$LOG_DIR" ]; then
    # 查找最新的日志文件
    LATEST_LOG=$(find "$LOG_DIR" -name "*.log" -type f -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -1 | cut -f2- -d" ")

    if [ -n "$LATEST_LOG" ]; then
        if grep -q "ImAuthController\|ImUserSyncService" "$LATEST_LOG" 2>/dev/null; then
            echo -e "${GREEN}✅ IM相关Bean已加载${NC}"
        else
            echo -e "${YELLOW}⚠️  未找到IM Bean加载日志${NC}"
            echo "   可能原因: @ConditionalOnProperty条件未满足"
        fi
    else
        echo -e "${YELLOW}⚠️  未找到日志文件${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  日志目录不存在: $LOG_DIR${NC}"
fi

# 7. 诊断总结
echo ""
echo "=================================="
echo "📊 诊断总结"
echo "=================================="
echo ""
echo "请根据上述检查结果执行相应的修复步骤:"
echo ""
echo "1. 如果OpenIM服务未运行:"
echo "   docker start openim-server openim-chat"
echo ""
echo "2. 如果用户未注册:"
echo "   参考上面第5步的注册命令"
echo ""
echo "3. 如果Bean未加载:"
echo "   cd smart-admin-api-java17-springboot3"
echo "   mvn clean compile"
echo "   mvn spring-boot:run"
echo ""
echo "4. 如果配置文件缺失:"
echo "   检查 sa-base/src/main/resources/dev/sa-base.yaml"
echo ""
echo "详细解决方案请查看: IM连接问题-完整解决方案.md"
echo ""
