package com.example.aiclient.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiclient.dto.AuthResponse;
import com.example.aiclient.dto.LoginRequest;
import com.example.aiclient.dto.RegisterRequest;
import com.example.aiclient.dto.UserVO;
import com.example.aiclient.entity.UserEntity;
import com.example.aiclient.exception.BizException;
import com.example.aiclient.mapper.UserMapper;
import com.example.aiclient.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 注册、登录、当前用户。角色注册时固定为 USER。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userMapper.exists(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, req.getUsername()))) {
            throw new BizException(40900, "用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null ? "" : req.getNickname());
        user.setRole("USER");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时由唯一索引兜底
            throw new BizException(40900, "用户名已存在");
        }
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(40100, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(40300, "账号已禁用");
        }
        return toAuthResponse(user);
    }

    public UserVO me(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(40400, "用户不存在");
        }
        return UserVO.from(user);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        return new AuthResponse(jwtService.generate(user), UserVO.from(user));
    }
}