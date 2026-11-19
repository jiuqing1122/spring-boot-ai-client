package com.example.aiclient.security;

import com.example.aiclient.entity.UserEntity;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setUsername("demo");
        user.setRole("USER");
        return user;
    }

    @Test
    void generateAndParse() {
        JwtService service = new JwtService(SECRET, 120);

        LoginUser parsed = service.parse(service.generate(user()));

        assertThat(parsed.id()).isEqualTo(42L);
        assertThat(parsed.username()).isEqualTo("demo");
        assertThat(parsed.role()).isEqualTo("USER");
    }

    @Test
    void expiredTokenRejected() {
        JwtService service = new JwtService(SECRET, -1);

        assertThatThrownBy(() -> service.parse(service.generate(user())))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSecretRejected() {
        assertThatThrownBy(() -> new JwtService("short", 120))
                .isInstanceOf(IllegalStateException.class);
    }
}