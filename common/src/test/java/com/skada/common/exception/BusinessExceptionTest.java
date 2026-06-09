package com.skada.common.exception;

import com.skada.common.enums.BizCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BusinessException 单元测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("构造时保存 code 和 message")
    void withCodeAndMessage() {
        BusinessException ex = new BusinessException(30001, "排行榜不存在");

        assertThat(ex.getCode()).isEqualTo(30001);
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("排行榜不存在");
    }

    @Test
    @DisplayName("单参数构造使用默认 BizCode.BIZ_ERROR")
    void defaultCode() {
        BusinessException ex = new BusinessException("业务错误");

        assertThat(ex.getCode()).isEqualTo(BizCode.BIZ_ERROR);
        assertThat(ex.getMessage()).isEqualTo("业务错误");
    }
}
