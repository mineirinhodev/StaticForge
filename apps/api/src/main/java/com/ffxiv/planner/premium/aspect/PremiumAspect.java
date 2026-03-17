package com.ffxiv.planner.premium.aspect;

import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ForbiddenException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspecto AOP que intercepta métodos anotados com {@link RequiresPremium}.
 *
 * <p>Verifica se o usuário autenticado possui assinatura premium ativa.
 * Lança {@link ForbiddenException} (HTTP 403) se não possuir.</p>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/29
 */
@Aspect
@Component
public class PremiumAspect {

    private final UserRepository userRepository;

    public PremiumAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Before("@annotation(com.ffxiv.planner.premium.aspect.RequiresPremium)")
    public void checkPremium() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            throw new ForbiddenException("Acesso restrito a usuários premium");
        }

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new ForbiddenException("Acesso restrito a usuários premium"));

        if (!user.isPremium()) {
            throw new ForbiddenException("Acesso restrito a usuários premium");
        }
    }
}
