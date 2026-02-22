package com.example.demo.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    @Bean
    public AtomicInteger activeUsersGauge(MeterRegistry registry) {
        AtomicInteger activeUsers = new AtomicInteger(0);
        Gauge.builder("jutjubic.active.users", activeUsers, AtomicInteger::get)
                .description("Broj trenutno aktivnih korisnika")
                .register(registry);
        return activeUsers;
    }

    @Bean
    public Counter videoViewsCounter(MeterRegistry registry) {
        return Counter.builder("jutjubic.video.views")
                .description("Ukupan broj pregleda videa")
                .register(registry);
    }

    @Bean
    public Counter apiRequestsCounter(MeterRegistry registry) {
        return Counter.builder("jutjubic.api.requests")
                .description("Ukupan broj API zahteva")
                .register(registry);
    }

    @Bean
    public Timer videoStreamTimer(MeterRegistry registry) {
        return Timer.builder("jutjubic.video.stream.duration")
                .description("Vreme trajanja stream-a videa")
                .register(registry);
    }
}