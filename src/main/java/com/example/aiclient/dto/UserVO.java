package com.example.aiclient.dto;

import com.example.aiclient.entity.UserEntity;

/**
 * 对外用户视图：id 转字符串，避免 JS 大整数精度丢失。
 */
public record UserVO(String id, String username, String nickname, String role) {

    public static UserVO from(UserEntity user) {
        return new UserVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getRole());
    }
}