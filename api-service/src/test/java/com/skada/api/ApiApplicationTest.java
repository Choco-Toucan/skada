package com.skada.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApiApplication 单元测试")
class ApiApplicationTest {

    @Test
    @DisplayName("上下文加载验证")
    void contextLoads() {
        // 验证Application类存在并可加载
        ApiApplication app = new ApiApplication();
    }
}
