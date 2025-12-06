package com.softteco.outbox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for the outbox pattern infrastructure.
 * Automatically configures all necessary components when this library is on the
 * classpath.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
@ComponentScan(basePackages = {
        "com.softteco.outbox.processor",
        "com.softteco.outbox.metrics",
        "com.softteco.outbox.cleanup"
})
@EnableJpaRepositories(basePackages = "com.softteco.outbox.repository")
@EntityScan(basePackages = "com.softteco.outbox.entity")
@RequiredArgsConstructor
public class OutboxAutoConfiguration {

    private final OutboxProperties properties;

    /**
     * Configure retry template for outbox event publishing.
     * Uses exponential backoff with configurable parameters.
     *
     * @return configured retry template
     */
    @Bean
    public RetryTemplate outboxRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3); // Fixed retry attempts
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMultiplier(2.0); // Double each time
        backOffPolicy.setMaxInterval(30000); // Max 30 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
