package com.example.aiclient.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * M0 兼容性探针：只放在测试代码里，验证 MyBatis-Plus 在 Boot 4 下能执行查询与插入。
 * 验证通过后在 M1 用真实 Mapper 替换，不进入主代码。
 */
@Mapper
public interface CompatMapper {

    @Select("SELECT 1")
    Integer selectOne();

    @Update("CREATE TABLE IF NOT EXISTS _m0_compat (v VARCHAR(16) NOT NULL)")
    void createTable();

    @Insert("INSERT INTO _m0_compat (v) VALUES (#{v})")
    int insert(@Param("v") String v);

    @Select("SELECT COUNT(*) FROM _m0_compat")
    int count();

    @Update("DROP TABLE IF EXISTS _m0_compat")
    void dropTable();
}