package com.siteguard;

import com.siteguard.service.MonitorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiteGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteGuardApplication.class, args);
    }

    /** Adds a few demo sites the first time the app starts (only if the database is empty). */
    @Bean
    CommandLineRunner seedData(MonitorService monitorService) {
        return args -> monitorService.seedDemoSitesIfEmpty();
    }
}
