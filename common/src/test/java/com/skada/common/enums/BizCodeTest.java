package com.skada.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BizCode 单元测试")
class BizCodeTest {

    @Test
    @DisplayName("系统错误码在 10000-19999 范围内")
    void systemCodesInRange() {
        assertThat(BizCode.SYSTEM_ERROR).isBetween(10000, 19999);
        assertThat(BizCode.PARAM_MISSING).isBetween(10000, 19999);
        assertThat(BizCode.REQUEST_FORMAT_ERROR).isBetween(10000, 19999);
        assertThat(BizCode.INTERNAL_ERROR).isBetween(10000, 19999);
    }

    @Test
    @DisplayName("SAAS接入层错误码在 20000-29999 范围内")
    void saasCodesInRange() {
        assertThat(BizCode.SAAS_ERROR).isBetween(20000, 29999);
        assertThat(BizCode.TENANT_ID_MISSING).isBetween(20000, 29999);
        assertThat(BizCode.SIGN_MISSING).isBetween(20000, 29999);
        assertThat(BizCode.SIGN_INVALID).isBetween(20000, 29999);
        assertThat(BizCode.TIMESTAMP_MISSING).isBetween(20000, 29999);
        assertThat(BizCode.TIMESTAMP_EXPIRED).isBetween(20000, 29999);
        assertThat(BizCode.TENANT_NOT_FOUND_OR_DISABLED).isBetween(20000, 29999);
        assertThat(BizCode.TENANT_AUTH_FAILED).isBetween(20000, 29999);
    }

    @Test
    @DisplayName("业务错误码在 30000-39999 范围内")
    void bizCodesInRange() {
        assertThat(BizCode.BIZ_ERROR).isBetween(30000, 39999);
        assertThat(BizCode.LEADERBOARD_NOT_FOUND_OR_DENIED).isBetween(30000, 39999);
        assertThat(BizCode.LEADERBOARD_STOPPED).isBetween(30000, 39999);
        assertThat(BizCode.INSTANCE_CHANGED).isBetween(30000, 39999);
        assertThat(BizCode.NO_ACTIVE_INSTANCE).isBetween(30000, 39999);
        assertThat(BizCode.INSTANCE_ID_REQUIRED).isBetween(30000, 39999);
        assertThat(BizCode.CONCURRENT_CONFLICT).isBetween(30000, 39999);
        assertThat(BizCode.RESOURCE_NOT_FOUND).isBetween(30000, 39999);
    }
}
