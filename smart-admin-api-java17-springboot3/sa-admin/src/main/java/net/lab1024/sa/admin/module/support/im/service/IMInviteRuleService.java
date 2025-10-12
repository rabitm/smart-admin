package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import net.lab1024.sa.admin.module.support.im.dao.IMGroupInviteRuleDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupInviteRuleEntity;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * IM群组邀请规则服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class IMInviteRuleService {

    @Resource
    private IMGroupInviteRuleDao imGroupInviteRuleDao;

    @Resource
    private EmployeeDao employeeDao;

    /**
     * 根据警情计算需要邀请的目标人员
     *
     * @param report 警情实体
     * @return 目标员工ID列表
     */
    public List<Long> calculateTargetEmployees(PoliceReportEntity report) {
        if (report == null) {
            return Collections.emptyList();
        }

        log.info("🎯 [规则引擎] 开始为警情{}计算目标人员", report.getReportId());

        // 1. 查询所有启用的规则(按优先级降序)
        List<IMGroupInviteRuleEntity> rules = imGroupInviteRuleDao.selectEnabledRules();

        if (rules.isEmpty()) {
            log.info("⚠️ [规则引擎] 没有启用的邀请规则");
            return Collections.emptyList();
        }

        log.info("📋 [规则引擎] 找到{}条启用的规则", rules.size());

        // 2. 用Set去重
        Set<Long> targetEmployeeIds = new LinkedHashSet<>();

        // 3. 按优先级执行规则
        for (IMGroupInviteRuleEntity rule : rules) {
            try {
                List<Long> ruleTargets = executeRule(rule, report);
                if (!ruleTargets.isEmpty()) {
                    targetEmployeeIds.addAll(ruleTargets);
                    log.info("✅ [规则引擎] 规则[{}]匹配{}个目标人员",
                            rule.getRuleName(), ruleTargets.size());
                }
            } catch (Exception e) {
                log.error("❌ [规则引擎] 规则[{}]执行失败: {}", rule.getRuleName(), e.getMessage(), e);
            }
        }

        List<Long> result = new ArrayList<>(targetEmployeeIds);
        log.info("✅ [规则引擎] 警情{}共计算出{}个目标人员", report.getReportId(), result.size());

        return result;
    }

    /**
     * 执行单条规则
     */
    private List<Long> executeRule(IMGroupInviteRuleEntity rule, PoliceReportEntity report) {
        JSONObject config = JSON.parseObject(rule.getRuleConfig());

        switch (rule.getRuleType()) {
            case 1: // 按警情类型
                return executeReportTypeRule(config, report);
            case 2: // 按部门
                return executeDepartmentRule(config, report);
            case 3: // 按角色
                return executeRoleRule(config, report);
            case 4: // 按警情等级
                return executeReportLevelRule(config, report);
            case 5: // 固定人员
                return executeFixedEmployeeRule(config, report);
            default:
                log.warn("⚠️ [规则引擎] 未知的规则类型: {}", rule.getRuleType());
                return Collections.emptyList();
        }
    }

    /**
     * 执行警情类型规则
     */
    private List<Long> executeReportTypeRule(JSONObject config, PoliceReportEntity report) {
        List<Integer> reportTypes = config.getJSONArray("report_types").toJavaList(Integer.class);

        // 检查警情类型是否匹配
        if (!reportTypes.contains(report.getReportType())) {
            return Collections.emptyList();
        }

        // 获取目标部门
        List<Long> targetDepartments = config.getJSONArray("target_departments").toJavaList(Long.class);

        if (targetDepartments.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询部门下的员工
        LambdaQueryWrapper<EmployeeEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(EmployeeEntity::getDepartmentId, targetDepartments);
        queryWrapper.eq(EmployeeEntity::getDeletedFlag, false);
        queryWrapper.eq(EmployeeEntity::getDisabledFlag, false);

        List<EmployeeEntity> employees = employeeDao.selectList(queryWrapper);

        return employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .collect(Collectors.toList());
    }

    /**
     * 执行部门规则
     */
    private List<Long> executeDepartmentRule(JSONObject config, PoliceReportEntity report) {
        List<Long> departments = config.getJSONArray("departments").toJavaList(Long.class);

        if (departments.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<EmployeeEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(EmployeeEntity::getDepartmentId, departments);
        queryWrapper.eq(EmployeeEntity::getDeletedFlag, false);
        queryWrapper.eq(EmployeeEntity::getDisabledFlag, false);

        List<EmployeeEntity> employees = employeeDao.selectList(queryWrapper);

        return employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .collect(Collectors.toList());
    }

    /**
     * 执行角色规则
     */
    private List<Long> executeRoleRule(JSONObject config, PoliceReportEntity report) {
        List<Long> roleIds = config.getJSONArray("role_ids").toJavaList(Long.class);

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO: 根据角色查询员工(需要角色-员工关联表)
        // 这里暂时返回空列表,实际应该查询 t_employee_role 表
        log.warn("⚠️ [规则引擎] 角色规则暂未实现完整,需要角色-员工关联表");

        return Collections.emptyList();
    }

    /**
     * 执行警情等级规则
     */
    private List<Long> executeReportLevelRule(JSONObject config, PoliceReportEntity report) {
        List<Integer> reportLevels = config.getJSONArray("report_levels").toJavaList(Integer.class);

        // 检查警情等级是否匹配
        if (!reportLevels.contains(report.getReportLevel())) {
            return Collections.emptyList();
        }

        Boolean notifySuperiors = config.getBoolean("notify_superiors");

        if (Boolean.TRUE.equals(notifySuperiors)) {
            // 通知上级领导
            Integer superiorLevels = config.getInteger("superior_levels");
            if (superiorLevels == null) {
                superiorLevels = 1;
            }

            // TODO: 根据层级查询上级领导(需要组织架构层级关系)
            log.warn("⚠️ [规则引擎] 上级通知功能暂未实现完整,需要组织架构数据");
        }

        return Collections.emptyList();
    }

    /**
     * 执行固定人员规则
     */
    private List<Long> executeFixedEmployeeRule(JSONObject config, PoliceReportEntity report) {
        List<Long> fixedEmployeeIds = config.getJSONArray("fixed_employee_ids").toJavaList(Long.class);

        if (fixedEmployeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 验证员工是否存在且未删除
        LambdaQueryWrapper<EmployeeEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(EmployeeEntity::getEmployeeId, fixedEmployeeIds);
        queryWrapper.eq(EmployeeEntity::getDeletedFlag, false);
        queryWrapper.eq(EmployeeEntity::getDisabledFlag, false);

        List<EmployeeEntity> employees = employeeDao.selectList(queryWrapper);

        return employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有启用的规则
     */
    public List<IMGroupInviteRuleEntity> getEnabledRules() {
        return imGroupInviteRuleDao.selectEnabledRules();
    }

    /**
     * 根据规则类型获取启用的规则
     */
    public List<IMGroupInviteRuleEntity> getEnabledRulesByType(Integer ruleType) {
        return imGroupInviteRuleDao.selectEnabledRulesByType(ruleType);
    }
}
