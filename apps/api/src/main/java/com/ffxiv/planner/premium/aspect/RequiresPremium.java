package com.ffxiv.planner.premium.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método que exige assinatura premium ativa para ser executado.
 *
 * <p>Quando o usuário autenticado não possui {@code isPremium == true},
 * o {@link PremiumAspect} lança {@link com.ffxiv.planner.common.exception.ForbiddenException}
 * resultando em HTTP 403.</p>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPremium {
}
