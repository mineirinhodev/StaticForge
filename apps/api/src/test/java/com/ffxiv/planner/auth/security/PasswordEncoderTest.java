package com.ffxiv.planner.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do PasswordEncoder provido pela SecurityConfig.
 * Mantido separado dos testes de SecurityConfig para não inflar
 * o contexto @WebMvcTest com verificações de BCrypt.
 */
class PasswordEncoderTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void encode_returnsBcryptHash() {
        String hash = encoder.encode("senha123");

        assertThat(hash).isNotBlank();
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String hash = encoder.encode("senha123");

        assertThat(encoder.matches("senha123", hash)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hash = encoder.encode("senha123");

        assertThat(encoder.matches("errada", hash)).isFalse();
    }

    @Test
    void encode_producesDifferentHashesForSamePassword() {
        String hash1 = encoder.encode("senha123");
        String hash2 = encoder.encode("senha123");

        // BCrypt usa salt aleatório — dois hashes nunca são iguais
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
