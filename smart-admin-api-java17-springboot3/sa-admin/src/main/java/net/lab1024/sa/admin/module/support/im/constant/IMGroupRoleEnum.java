package net.lab1024.sa.admin.module.support.im.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IM群组角色枚举
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Getter
@AllArgsConstructor
public enum IMGroupRoleEnum {

    /**
     * 普通成员
     */
    MEMBER(1, "普通成员", 20),

    /**
     * 管理员
     */
    ADMIN(2, "管理员", 60),

    /**
     * 群主
     */
    OWNER(3, "群主", 100),
    ;

    private final Integer code;
    private final String description;
    private final Integer roleLevel;

    public static IMGroupRoleEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IMGroupRoleEnum roleEnum : values()) {
            if (roleEnum.getCode().equals(code)) {
                return roleEnum;
            }
        }
        return null;
    }

    public static boolean isOwner(Integer code) {
        return OWNER.getCode().equals(code);
    }

    public static boolean isAdmin(Integer code) {
        return ADMIN.getCode().equals(code);
    }

    public static boolean isMember(Integer code) {
        return MEMBER.getCode().equals(code);
    }
}
