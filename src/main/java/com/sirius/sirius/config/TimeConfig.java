package com.sirius.sirius.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone:Europe/Kyiv}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
