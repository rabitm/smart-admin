#!/bin/bash

# OpenIM用户注册脚本
# 用法: bash register-openim-user.sh

echo "=========================================="
echo "OpenIM用户注册脚本"
echo "=========================================="
echo ""

# 生成operationID (Unix时间戳毫秒)
OPERATION_ID=$(date +%s)000

echo "1️⃣  获取OpenIM管理员Token..."
echo "   OperationID: $OPERATION_ID"

# 获取管理员Token
RESPONSE=$(curl -s -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -H "operationID: $OPERATION_ID" \
  -d '{"secret":"openIM123"}')

echo "   响应: $RESPONSE"

# 提取token
TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 获取Token失败!"
    echo "   请检查OpenIM服务是否正常运行: docker ps | grep openim"
    exit 1
fi

echo "✅ Token获取成功: ${TOKEN:0:30}..."
echo ""

echo "2️⃣  注册用户ID=1..."

# 生成新的operationID
OPERATION_ID=$(date +%s)001

# 注册用户 - 注意请求格式
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:10002/user/user_register \
  -H "Content-Type: application/json" \
  -H "operationID: $OPERATION_ID" \
  -H "token: $TOKEN" \
  -d '{
    "secret": "openIM123",
    "users": [
      {
        "userID": "1",
        "nickname": "系统管理员",
        "faceURL": ""
      }
    ]
  }')

echo "   响应: $REGISTER_RESPONSE"

# 检查结果
if echo "$REGISTER_RESPONSE" | grep -q '"errCode":0'; then
    echo "✅ 用户注册成功!"
else
    echo "⚠️  注册可能失败,请检查响应"
fi

echo ""
echo "3️⃣  验证用户是否存在..."

# 生成新的operationID
OPERATION_ID=$(date +%s)002

# 查询用户
CHECK_RESPONSE=$(curl -s -X POST http://localhost:10002/user/get_users \
  -H "Content-Type: application/json" \
  -H "operationID: $OPERATION_ID" \
  -H "token: $TOKEN" \
  -d '{
    "userIDs": ["1"]
  }')

echo "   响应: $CHECK_RESPONSE"

if echo "$CHECK_RESPONSE" | grep -q '"userID":"1"'; then
    echo "✅ 用户验证成功!"
    echo ""
    echo "=========================================="
    echo "✅ OpenIM用户注册完成!"
    echo "现在可以刷新浏览器,IM应该能正常连接了"
    echo "=========================================="
else
    echo "❌ 用户验证失败"
    echo "   请检查OpenIM服务状态"
fi
