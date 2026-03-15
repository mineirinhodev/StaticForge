package com.ffxiv.planner.auth.security;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller mínimo usado apenas pelos testes de SecurityConfig.
 * Expõe um endpoint protegido para verificar que o filtro JWT
 * está sendo aplicado corretamente.
 *
 * <p>{@code @Profile} garante que não conflite com controllers reais
 * quando o contexto completo é carregado em {@code @SpringBootTest}.</p>
 */
@RestController
@Profile("security-test")
class SecurityTestController {

    @GetMapping("/protected")
    String protectedEndpoint() {
        return "ok";
    }
}
