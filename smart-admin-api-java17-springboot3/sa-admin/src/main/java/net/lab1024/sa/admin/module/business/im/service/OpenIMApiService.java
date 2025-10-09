package net.lab1024.sa.admin.module.business.im.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.config.OpenIMProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * OpenIM API服务
 * 封装所有OpenIM REST API调用
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
// 临时移除条件注解以便调试
// @ConditionalOnProperty(prefix = "openim", name = "api-url")
public class OpenIMApiService {

    @Qualifier("openIMRestTemplate")
    private final RestTemplate restTemplate;

    private final OpenIMProperties openIMProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取管理员Token
     */
    public String getAdminToken() {
        try {
            String url = openIMProperties.getApiUrl() + "/auth/get_admin_token";

            Map<String, Object> request = new HashMap<>();
            request.put("secret", openIMProperties.getAdmin().getSecret());
            request.put("userID", openIMProperties.getAdmin().getUserId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("operationID", UUID.randomUUID().toString());  // Required by OpenIM API

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                String token = jsonNode.get("data").get("token").asText();
                log.info("📱 [OpenIM API] 获取管理员Token成功");
                return token;
            } else {
                String errMsg = jsonNode.get("errMsg").asText();
                log.error("📱 [OpenIM API] 获取管理员Token失败: {}", errMsg);
                throw new RuntimeException("获取管理员Token失败: " + errMsg);
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 获取管理员Token异常", e);
            throw new RuntimeException("获取管理员Token异常", e);
        }
    }

    /**
     * 注册用户
     */
    public boolean registerUser(String userID, String nickname, String faceURL) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/user/user_register";

            Map<String, Object> user = new HashMap<>();
            user.put("userID", userID);
            user.put("nickname", nickname);
            user.put("faceURL", faceURL != null ? faceURL : "");

            Map<String, Object> request = new HashMap<>();
            request.put("users", Collections.singletonList(user));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("📱 [OpenIM API] 注册用户请求 - UserID: {}, Nickname: {}, URL: {}", userID, nickname, url);
            log.info("📱 [OpenIM API] 注册用户请求体: {}", objectMapper.writeValueAsString(request));

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("📱 [OpenIM API] 注册用户响应 - UserID: {}, Response: {}", userID, response.getBody());
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                log.info("📱 [OpenIM API] 注册用户成功 - UserID: {}", userID);
                return true;
            } else {
                String errMsg = jsonNode.get("errMsg").asText();
                log.error("📱 [OpenIM API] 注册用户失败 - UserID: {}, 错误码: {}, 错误信息: {}",
                        userID, jsonNode.get("errCode").asInt(), errMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 注册用户异常 - UserID: {}", userID, e);
            return false;
        }
    }

    /**
     * 获取用户Token
     */
    public String getUserToken(String userID) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/auth/get_user_token";

            Map<String, Object> request = new HashMap<>();
            request.put("userID", userID);
            request.put("platformID", openIMProperties.getPlatformId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                String token = jsonNode.get("data").get("token").asText();
                log.info("📱 [OpenIM API] 获取用户Token成功 - UserID: {}", userID);
                return token;
            } else {
                String errMsg = jsonNode.get("errMsg").asText();
                log.error("📱 [OpenIM API] 获取用户Token失败 - UserID: {}, 错误: {}", userID, errMsg);
                throw new RuntimeException("获取用户Token失败: " + errMsg);
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 获取用户Token异常 - UserID: {}", userID, e);
            throw new RuntimeException("获取用户Token异常", e);
        }
    }

    /**
     * 创建群组
     */
    public String createGroup(String groupName, String ownerUserID, List<String> memberUserIDs) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/group/create_group";

            // 构建groupInfo (不包含ownerUserID)
            Map<String, Object> groupInfo = new HashMap<>();
            groupInfo.put("groupName", groupName);
            groupInfo.put("groupType", openIMProperties.getGroup().getDefaultType());

            // 构建请求体 (ownerUserID作为顶层参数)
            Map<String, Object> request = new HashMap<>();
            request.put("ownerUserID", ownerUserID);  // 顶层参数
            request.put("groupInfo", groupInfo);
            request.put("memberUserIDs", memberUserIDs);

            // 详细日志输出请求参数
            log.info("📱 [OpenIM API] 创建群组请求 - URL: {}", url);
            log.info("📱 [OpenIM API] 创建群组请求 - ownerUserID: {}", ownerUserID);
            log.info("📱 [OpenIM API] 创建群组请求 - groupName: {}", groupName);
            log.info("📱 [OpenIM API] 创建群组请求 - groupType: {}", openIMProperties.getGroup().getDefaultType());
            log.info("📱 [OpenIM API] 创建群组请求 - memberUserIDs: {}", memberUserIDs);
            log.info("📱 [OpenIM API] 创建群组请求体: {}", objectMapper.writeValueAsString(request));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("📱 [OpenIM API] 创建群组响应: {}", response.getBody());

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                String groupID = jsonNode.get("data").get("groupInfo").get("groupID").asText();
                log.info("📱 [OpenIM API] 创建群组成功 - GroupID: {}, GroupName: {}", groupID, groupName);
                return groupID;
            } else {
                String errMsg = jsonNode.get("errMsg").asText();
                log.error("📱 [OpenIM API] 创建群组失败 - GroupName: {}, 错误: {}", groupName, errMsg);
                throw new RuntimeException("创建群组失败: " + errMsg);
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 创建群组异常 - GroupName: {}", groupName, e);
            throw new RuntimeException("创建群组异常", e);
        }
    }

    /**
     * 邀请成员加入群组
     */
    public boolean inviteToGroup(String groupID, List<String> userIDs) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/group/invite_user_to_group";

            Map<String, Object> request = new HashMap<>();
            request.put("groupID", groupID);
            request.put("invitedUserIDs", userIDs);
            request.put("reason", "系统自动邀请");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                log.info("📱 [OpenIM API] 邀请成员成功 - GroupID: {}, 成员数: {}", groupID, userIDs.size());
                return true;
            } else {
                String errMsg = jsonNode.get("errMsg").asText();
                log.warn("📱 [OpenIM API] 邀请成员失败 - GroupID: {}, 错误: {}", groupID, errMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 邀请成员异常 - GroupID: {}", groupID, e);
            return false;
        }
    }

    /**
     * 获取群组信息
     */
    public JsonNode getGroupInfo(String groupID) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/group/get_groups_info";

            Map<String, Object> request = new HashMap<>();
            request.put("groupIDs", Collections.singletonList(groupID));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                JsonNode dataNode = jsonNode.get("data");
                // ✅ 修复：OpenIM API返回 {"data": {"groupInfos": [...]}}，需要访问data.groupInfos
                if (dataNode != null) {
                    JsonNode groupInfosNode = dataNode.get("groupInfos");
                    if (groupInfosNode != null && groupInfosNode.isArray() && groupInfosNode.size() > 0) {
                        JsonNode groupInfoNode = groupInfosNode.get(0);
                        log.debug("📱 [OpenIM API] 获取群组信息成功 - GroupID: {}, Info: {}", groupID, groupInfoNode);
                        return groupInfoNode;
                    } else {
                        log.warn("📱 [OpenIM API] 群组信息为空或不存在 - GroupID: {}, Response: {}", groupID, response.getBody());
                        return null;
                    }
                } else {
                    log.warn("📱 [OpenIM API] data节点为空 - GroupID: {}, Response: {}", groupID, response.getBody());
                    return null;
                }
            } else {
                log.warn("📱 [OpenIM API] 获取群组信息失败 - GroupID: {}, ErrCode: {}, Response: {}",
                        groupID, jsonNode.get("errCode").asInt(), response.getBody());
                return null;
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 获取群组信息异常 - GroupID: {}", groupID, e);
            return null;
        }
    }

    /**
     * 检查用户是否在群组中
     */
    public boolean isUserInGroup(String groupID, String userID) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/group/get_group_member_list";

            Map<String, Object> request = new HashMap<>();
            request.put("groupID", groupID);
            request.put("filter", 0); // 0=所有成员

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if (jsonNode.get("errCode").asInt() == 0) {
                JsonNode members = jsonNode.get("data").get("members");
                if (members != null && members.isArray()) {
                    for (JsonNode member : members) {
                        if (member.get("userID").asText().equals(userID)) {
                            log.info("📱 [OpenIM API] 用户已在群组中 - GroupID: {}, UserID: {}", groupID, userID);
                            return true;
                        }
                    }
                }
                log.info("📱 [OpenIM API] 用户不在群组中 - GroupID: {}, UserID: {}", groupID, userID);
                return false;
            } else {
                log.warn("📱 [OpenIM API] 检查群组成员失败 - GroupID: {}", groupID);
                return false;
            }

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 检查群组成员异常 - GroupID: {}, UserID: {}", groupID, userID, e);
            return false;
        }
    }

    /**
     * 检查用户是否存在
     */
    public boolean checkUserExists(String userID) {
        try {
            String adminToken = getAdminToken();
            String url = openIMProperties.getApiUrl() + "/user/get_users_info";

            Map<String, Object> request = new HashMap<>();
            request.put("userIDs", Collections.singletonList(userID));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", adminToken);
            headers.set("operationID", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("📱 [OpenIM API] 检查用户存在请求 - UserID: {}", userID);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("📱 [OpenIM API] 检查用户存在响应 - UserID: {}, Response: {}", userID, response.getBody());
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // 检查响应结构: {"errCode":0,"data":{"usersInfo":[...]}}
            // 如果usersInfo为null或空数组,表示用户不存在
            boolean exists = false;
            if (jsonNode.get("errCode").asInt() == 0) {
                JsonNode data = jsonNode.get("data");
                if (data != null && !data.isNull()) {
                    JsonNode usersInfo = data.get("usersInfo");
                    if (usersInfo != null && !usersInfo.isNull() && usersInfo.isArray() && usersInfo.size() > 0) {
                        exists = true;
                    }
                }
            }

            log.info("📱 [OpenIM API] 用户存在性检查结果 - UserID: {}, Exists: {}", userID, exists);
            return exists;

        } catch (Exception e) {
            log.error("📱 [OpenIM API] 检查用户存在异常 - UserID: {}", userID, e);
            return false;
        }
    }
}
