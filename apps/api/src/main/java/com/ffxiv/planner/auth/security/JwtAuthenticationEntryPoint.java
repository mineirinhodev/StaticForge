package com.ffxiv.planner.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Retorna 401 com corpo no formato RFC 7807 (ProblemDetail) quando uma
 * requisição não autenticada tenta acessar um recurso protegido.
 *
 * <p>Usa o mesmo formato de erro do {@code GlobalExceptionHandler},
 * garantindo consistência entre todos os erros da API.</p>
 *
 * <p>Sem este componente, o Spring Security redirecionaria para uma
 * página de login HTML — inadequado para uma API REST stateless.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final URI UNAUTHORIZED_TYPE =
            URI.create("https://httpstatuses.com/401");

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        problem.setType(UNAUTHORIZED_TYPE);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
