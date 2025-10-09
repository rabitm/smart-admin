#!/bin/bash

# 邀请用户加入OpenIM群组脚本
# 用法: bash invite-user-to-group.sh

echo "=========================================="
echo "邀请用户加入OpenIM群组"
echo "=========================================="
echo ""

# 1. 获取管理员Token
echo "1️⃣  获取OpenIM管理员Token..."
OPERATION_ID=$(date +%s%3N)

RESPONSE=$(curl -s -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -H "operationID: $OPERATION_ID" \
  -d '{"secret":"openIM123","userID":"imAdmin"}')

echo "   响应: $RESPONSE"

TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 获取Token失败!"
    exit 1
fi

echo "✅ Token获取成功"
echo ""

# 2. 邀请用户ID=1加入群组800479929
echo "2️⃣  邀请用户ID=1加入群组800479929..."
OPERATION_ID=$(date +%s%3N)

INVITE_RESPONSE=$(curl -s -X POST http://localhost:10002/group/invite_user_to_group \
  -H "Content-Type: application/json" \
  -H "operationID: $OPERATION_ID" \
  -H "token: $TOKEN" \
  -d '{
    "groupID": "800479929",
    "invitedUserIDs": ["1"],
    "reason": "手动邀请加入群组"
  }')

echo "   响应: $INVITE_RESPONSE"

if echo "$INVITE_RESPONSE" | grep -q '"errCode":0'; then
    echo "✅ 邀请成功!"
else
    echo "⚠️  邀请可能失败,请检查响应"
fi

echo ""
echo "=========================================="
echo "✅ 操作完成!"
echo "请刷新浏览器重试发送消息"
echo "=========================================="
