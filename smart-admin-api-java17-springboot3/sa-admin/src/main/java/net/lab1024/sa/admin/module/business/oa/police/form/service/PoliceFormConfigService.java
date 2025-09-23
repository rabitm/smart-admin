package net.lab1024.sa.admin.module.business.oa.police.form.service;

import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormConfigVO;
import net.lab1024.sa.admin.module.business.oa.police.form.manager.PoliceFormConfigManager;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 警情表单配置服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Service
public class PoliceFormConfigService {

    @Autowired
    private PoliceFormConfigManager policeFormConfigManager;

    /**
     * 获取表单配置
     */
    public ResponseDTO<PoliceFormConfigVO> getFormConfig(Integer reportType, Long organizationId) {
        if (reportType == null) {
            return ResponseDTO.userErrorParam("警情类型不能为空");
        }

        PoliceFormConfigVO config = policeFormConfigManager.getFormConfig(reportType, organizationId);

        if (config == null) {
            return ResponseDTO.userErrorParam("未找到对应的表单配置");
        }

        return ResponseDTO.ok(config);
    }
}