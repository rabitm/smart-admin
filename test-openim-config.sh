#!/bin/bash

echo "Testing OpenIM Configuration..."
echo "================================"
echo ""

# Test 1: Check OpenIM services
echo "Test 1: OpenIM Services Status"
echo "------------------------------"
curl -s http://localhost:10002/healthz 2>&1 | head -n 5
echo ""

# Test 2: Test get_admin_token API
echo "Test 2: Get Admin Token"
echo "------------------------------"
echo "Testing with default secret 'openIM123':"
curl -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{"secret":"openIM123"}' 2>&1
echo ""
echo ""

# Test 3: Try common default secrets
echo "Test 3: Trying Common Secrets"
echo "------------------------------"

SECRETS=("openIM123" "openIM" "123456" "admin" "secret")

for secret in "${SECRETS[@]}"; do
  echo "Trying secret: $secret"
  response=$(curl -s -X POST http://localhost:10002/auth/get_admin_token \
    -H "Content-Type: application/json" \
    -d "{\"secret\":\"$secret\"}")

  if echo "$response" | grep -q "errCode.*0"; then
    echo "✅ SUCCESS! The secret is: $secret"
    echo "Response: $response"
    break
  else
    echo "❌ Failed with secret: $secret"
  fi
  echo ""
done

echo ""
echo "================================"
echo "Test Complete"
