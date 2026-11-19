package com.example.aiclient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aiclient.entity.UserEntity;
import com.example.aiclient.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M1 验收：Flyway 迁移执行、种子账号可查、分页插件生效。
 */
@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Test
    void flywayMigrationsApplied() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }

    @Test
    void seedUsersQueryable() {
        UserEntity admin = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, "admin"));
        assertThat(admin).isNotNull();
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(admin.getNickname()).isEqualTo("管理员");
        assertThat(admin.getPasswordHash()).startsWith("$2a$");

        UserEntity demo = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, "demo"));
        assertThat(demo).isNotNull();
        assertThat(demo.getRole()).isEqualTo("USER");
    }

    @Test
    void paginationInterceptorConfigured() {
        assertThat(mybatisPlusInterceptor.getInterceptors())
                .anyMatch(i -> i instanceof PaginationInnerInterceptor);

        Page<UserEntity> page = userMapper.selectPage(new Page<>(1, 1), null);
        assertThat(page.getTotal()).isGreaterThanOrEqualTo(2);
        assertThat(page.getRecords()).hasSize(1);
    }
}