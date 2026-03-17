package com.ffxiv.planner.premium.aspect;

import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes do PremiumAspect usando contexto Spring mínimo (sem startup completo).
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/29
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PremiumAspectTest.TestConfig.class)
class PremiumAspectTest {

    @MockBean
    UserRepository userRepository;

    @Autowired
    SampleService sampleService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    // ── @RequiresPremium checks ────────────────────────────────

    @Test
    void requiresPremium_throwsForbiddenWhenUserNotPremium() {
        User user = new User();
        user.setEmail("player@test.com");
        user.setPremium(false);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        authenticateAs(userId);

        assertThatThrownBy(() -> sampleService.premiumOperation())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requiresPremium_proceedsWhenUserIsPremium() {
        User user = new User();
        user.setEmail("player@test.com");
        user.setPremium(true);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        authenticateAs(userId);

        assertThatNoException().isThrownBy(() -> sampleService.premiumOperation());
    }

    @Test
    void requiresPremium_throwsForbiddenWhenNotAuthenticated() {
        // SecurityContext vazio — sem principal
        assertThatThrownBy(() -> sampleService.premiumOperation())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void noAnnotation_doesNotCheckPremium() {
        // Método sem @RequiresPremium não deve consultar o repositório
        authenticateAs(userId);

        assertThatNoException().isThrownBy(() -> sampleService.freeOperation());

        verifyNoInteractions(userRepository);
    }

    // ── helper ────────────────────────────────────────────────

    private void authenticateAs(UUID id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, Collections.emptyList()));
    }

    // ── stub service ──────────────────────────────────────────

    @Service
    static class SampleService {

        @RequiresPremium
        public void premiumOperation() {
            // método de teste — apenas executa se premium
        }

        public void freeOperation() {
            // método sem restrição
        }
    }

    // ── contexto mínimo ───────────────────────────────────────

    @org.springframework.context.annotation.Configuration
    @EnableAspectJAutoProxy
    @Import({PremiumAspect.class, SampleService.class})
    static class TestConfig {
    }
}
