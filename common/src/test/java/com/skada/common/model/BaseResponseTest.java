package com.skada.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BaseResponse 单元测试")
class BaseResponseTest {

    @Nested
    @DisplayName("success")
    class Success {

        @Test
        @DisplayName("success(data) 返回 code=200, message=success")
        void successWithData() {
            BaseResponse<String> resp = BaseResponse.success("hello");

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getMessage()).isEqualTo("success");
            assertThat(resp.getData()).isEqualTo("hello");
            assertThat(resp.getTimestamp()).isPositive();
        }

        @Test
        @DisplayName("success() 无参版本 data 为 null")
        void successNoData() {
            BaseResponse<Void> resp = BaseResponse.success();

            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getMessage()).isEqualTo("success");
            assertThat(resp.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("error")
    class Error {

        @Test
        @DisplayName("error(code, message) 返回对应 code 和 message")
        void errorWithCode() {
            BaseResponse<Void> resp = BaseResponse.error(30001, "排行榜不存在");

            assertThat(resp.getCode()).isEqualTo(30001);
            assertThat(resp.getMessage()).isEqualTo("排行榜不存在");
            assertThat(resp.getData()).isNull();
        }

        @Test
        @DisplayName("error(message) 使用默认系统错误码")
        void errorDefaultCode() {
            BaseResponse<Void> resp = BaseResponse.error("something wrong");

            assertThat(resp.getCode()).isEqualTo(10000);
            assertThat(resp.getMessage()).isEqualTo("something wrong");
        }
    }
}
