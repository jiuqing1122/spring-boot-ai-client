package com.example.aiclient.dto;

/**
 * 登录/注册成功返回：token + 用户信息。
 */
public record AuthResponse(String token, UserVO user) {
}