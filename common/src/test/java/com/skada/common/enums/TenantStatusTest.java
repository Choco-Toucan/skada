package com.skada.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantStatus 单元测试")
class TenantStatusTest {

    @Test
    @DisplayName("ENABLED 的 value=1, label=启用")
    void enabledValues() {
        assertThat(TenantStatus.ENABLED.getValue()).isEqualTo(1);
        assertThat(TenantStatus.ENABLED.getLabel()).isEqualTo("启用");
        assertThat(TenantStatus.ENABLED.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("DISABLED 的 value=0, label=停用")
    void disabledValues() {
        assertThat(TenantStatus.DISABLED.getValue()).isEqualTo(0);
        assertThat(TenantStatus.DISABLED.getLabel()).isEqualTo("停用");
        assertThat(TenantStatus.DISABLED.isEnabled()).isFalse();
    }

    @Nested
    @DisplayName("fromValue")
    class FromValue {

        @Test
        @DisplayName("value=1 返回 ENABLED")
        void fromValueEnabled() {
            assertThat(TenantStatus.fromValue(1)).isEqualTo(TenantStatus.ENABLED);
        }

        @Test
        @DisplayName("value=0 返回 DISABLED")
        void fromValueDisabled() {
            assertThat(TenantStatus.fromValue(0)).isEqualTo(TenantStatus.DISABLED);
        }

        @Test
        @DisplayName("null 返回 DISABLED")
        void fromValueNull() {
            assertThat(TenantStatus.fromValue(null)).isEqualTo(TenantStatus.DISABLED);
        }

        @Test
        @DisplayName("非法值返回 DISABLED")
        void fromValueInvalid() {
            assertThat(TenantStatus.fromValue(99)).isEqualTo(TenantStatus.DISABLED);
            assertThat(TenantStatus.fromValue(-1)).isEqualTo(TenantStatus.DISABLED);
        }
    }
}
