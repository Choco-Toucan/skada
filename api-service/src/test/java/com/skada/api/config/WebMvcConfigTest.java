package com.skada.api.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebMvcConfig 单元测试")
class WebMvcConfigTest {

    @Mock
    private Gson gson;

    @Test
    @DisplayName("addInterceptors 注册拦截器")
    void addInterceptors_registersInterceptors() {
        WebMvcConfig config = new WebMvcConfig(gson);
        InterceptorRegistry registry = spy(new InterceptorRegistry());

        config.addInterceptors(registry);

        // 不抛异常即为成功
    }
}
