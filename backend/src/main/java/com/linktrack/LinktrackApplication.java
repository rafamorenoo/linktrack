package com.linktrack;

import com.linktrack.config.AppProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.linktrack.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableJpaRepositories(basePackages = "com.linktrack.repository")
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
public class LinktrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(LinktrackApplication.class, args);
    }
}
