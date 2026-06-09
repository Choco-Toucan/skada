package com.skada.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PageResult 单元测试")
class PageResultTest {

    @Test
    @DisplayName("正常分页，getTotalPages 正确计算")
    void normalPagination() {
        var records = List.of("a", "b", "c");
        PageResult<String> result = new PageResult<>(records, 25, 1, 10);

        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(25);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("pageSize 为 0 时 totalPages 返回 0")
    void zeroPageSize() {
        PageResult<String> result = new PageResult<>(List.of(), 100, 1, 0);

        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("总数为 0 时 totalPages 返回 0")
    void zeroTotal() {
        PageResult<String> result = new PageResult<>(List.of(), 0, 1, 10);

        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.getRecords()).isEmpty();
    }
}
