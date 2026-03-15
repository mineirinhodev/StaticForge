package com.ffxiv.planner.auth.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller mínimo usado apenas pelos testes de SecurityConfig.
 * Expõe um endpoint protegido para verificar que o filtro JWT
 * está sendo aplicado corretamente.
 */
@RestController
class SecurityTestController {

    @GetMapping("/protected")
    String protectedEndpoint() {
        return "ok";
    }
}
