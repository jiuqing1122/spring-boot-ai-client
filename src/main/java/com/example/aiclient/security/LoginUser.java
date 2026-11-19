package com.example.aiclient.security;

/**
 * 从 JWT 解析出的登录用户，作为 SecurityContext 的 principal。
 * 异步线程不读取 SecurityContext，需要用户信息时由 Controller 线程显式传参。
 */
public record LoginUser(Long id, String username, String role) {
}