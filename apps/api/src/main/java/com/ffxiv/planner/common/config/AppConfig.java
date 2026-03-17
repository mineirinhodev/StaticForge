package com.ffxiv.planner.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Beans de infraestrutura geral da aplicação.
 *
 * <p>Define o {@link Clock} UTC como bean singleton para injeção nos
 * componentes que precisam de tempo atual (ex: schedulers). O uso de
 * {@code Clock} em vez de {@code LocalDate.now()} diretamente permite
 * substituição por {@code Clock.fixed(...)} nos testes unitários.</p>
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
