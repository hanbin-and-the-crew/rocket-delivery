package org.sparta.product.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@TestConfiguration
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "org\\.sparta\\.product\\.infrastructure\\.event\\.kafka\\.listener\\..*"
        )
)
public class TestExcludeConfig {
}
