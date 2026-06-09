package com.skada.common.exception;

import com.skada.common.enums.BizCode;
import com.skada.common.model.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException → PARAM_MISSING")
    void handleIllegalArgument() {
        var resp = handler.handleIllegalArgument(new IllegalArgumentException("name不能为空"));

        assertThat(resp.getCode()).isEqualTo(BizCode.PARAM_MISSING);
        assertThat(resp.getMessage()).isEqualTo("name不能为空");
    }

    @Test
    @DisplayName("BusinessException → 保持异常的 code")
    void handleBusiness() {
        var resp = handler.handleBusiness(new BusinessException(30001, "排行榜不存在"));

        assertThat(resp.getCode()).isEqualTo(30001);
        assertThat(resp.getMessage()).isEqualTo("排行榜不存在");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → REQUEST_FORMAT_ERROR")
    void handleMessageNotReadable() {
        var resp = handler.handleMessageNotReadable(mock(HttpMessageNotReadableException.class));

        assertThat(resp.getCode()).isEqualTo(BizCode.REQUEST_FORMAT_ERROR);
        assertThat(resp.getMessage()).contains("JSON");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 取第一个 fieldError 的 message")
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("req", "name", "名称不能为空");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var resp = handler.handleValidation(ex);

        assertThat(resp.getCode()).isEqualTo(BizCode.PARAM_MISSING);
        assertThat(resp.getMessage()).contains("name").contains("名称不能为空");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 无 fieldError 时返回默认消息")
    void handleValidationNoFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var resp = handler.handleValidation(ex);

        assertThat(resp.getCode()).isEqualTo(BizCode.PARAM_MISSING);
        assertThat(resp.getMessage()).isEqualTo("参数校验失败");
    }

    @Test
    @DisplayName("NoResourceFoundException → RESOURCE_NOT_FOUND")
    void handleNoResourceFound() {
        var resp = handler.handleNoResourceFound(mock(NoResourceFoundException.class));

        assertThat(resp.getCode()).isEqualTo(BizCode.RESOURCE_NOT_FOUND);
        assertThat(resp.getMessage()).isEqualTo("请求的资源不存在");
    }

    @Test
    @DisplayName("通用 Exception → INTERNAL_ERROR")
    void handleException() {
        var resp = handler.handleException(new RuntimeException("内部错误"));

        assertThat(resp.getCode()).isEqualTo(BizCode.INTERNAL_ERROR);
        assertThat(resp.getMessage()).isEqualTo("服务器内部错误");
    }
}
