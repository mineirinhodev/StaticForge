package com.ffxiv.planner.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Retorna 401 com corpo JSON quando uma requisição não autenticada
 * tenta acessar um recurso protegido.
 *
 * <p>Sem este componente, o Spring Security redirecionaria para uma
 * página de login HTML — inadequado para uma API REST stateless.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","message":"Autenticação necessária"}""");
    }
}
