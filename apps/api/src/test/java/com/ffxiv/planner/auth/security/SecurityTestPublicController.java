package com.ffxiv.planner.auth.security;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller mínimo usado apenas pelos testes de SecurityConfig.
 * Expõe os endpoints públicos de auth para validar que não são
 * bloqueados pelo filtro de segurança.
 */
@RestController
@RequestMapping("/auth")
class SecurityTestPublicController {

    @PostMapping("/register")
    String register() { return "ok"; }

    @PostMapping("/login")
    String login() { return "ok"; }

    @PostMapping("/refresh")
    String refresh() { return "ok"; }

    @PostMapping("/forgot-password")
    String forgotPassword() { return "ok"; }

    @PostMapping("/reset-password")
    String resetPassword() { return "ok"; }
}
