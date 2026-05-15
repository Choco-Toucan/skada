package com.skada.common.enums;

/**
 * 租户状态枚举
 */
public enum TenantStatus {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final int value;
    private final String label;

    TenantStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    /** 从整数值转换，非法值默认返回 DISABLED */
    public static TenantStatus fromValue(Integer value) {
        if (value == null) return DISABLED;
        for (TenantStatus s : values()) {
            if (s.value == value) return s;
        }
        return DISABLED;
    }

    public boolean isEnabled() { return this == ENABLED; }
}
