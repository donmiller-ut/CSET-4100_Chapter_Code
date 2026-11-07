package com.example.northstarmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class NorthstarPropertiesTests {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(Settings.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(NorthstarProperties.class)
    static class Settings {}

    @Test void bindsAValidCartCapacity() {
        context.withPropertyValues("northstar.cart.maximum-total-quantity=18").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBean(NorthstarProperties.class).cart().maximumTotalQuantity()).isEqualTo(18);
        });
    }
    @Test void rejectsZeroDuringContextStartup() {
        context.withPropertyValues("northstar.cart.maximum-total-quantity=0")
                .run(c -> assertThat(c).hasFailed());
    }
    @Test void rejectsValuesAboveTheDatabaseLimit() {
        context.withPropertyValues("northstar.cart.maximum-total-quantity=21")
                .run(c -> assertThat(c).hasFailed());
    }
    @Test void rejectsMissingNestedSettings() {
        context.run(c -> assertThat(c).hasFailed());
    }
}
