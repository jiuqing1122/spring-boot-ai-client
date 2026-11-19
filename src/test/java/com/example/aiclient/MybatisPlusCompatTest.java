package com.example.aiclient;

import com.example.aiclient.mapper.CompatMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M0 验收：应用上下文能启动，MyBatis-Plus Mapper 能执行查询与插入。
 * 需要本机 MySQL 可用（连接信息来自 DB_URL / DB_USERNAME / DB_PASSWORD）。
 */
@SpringBootTest
class MybatisPlusCompatTest {

    @Autowired
    private CompatMapper compatMapper;

    @Test
    void mapperCanQueryAndInsert() {
        assertThat(compatMapper.selectOne()).isEqualTo(1);

        try {
            compatMapper.createTable();
            assertThat(compatMapper.insert("ok")).isEqualTo(1);
            assertThat(compatMapper.count()).isEqualTo(1);
        } finally {
            compatMapper.dropTable();
        }
    }
}