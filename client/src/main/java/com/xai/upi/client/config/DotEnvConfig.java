package com.xai.upi.client.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class DotEnvConfig {

    @PostConstruct
    public void loadDotEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load(); // Loads from src/main/resources/.env
            dotenv.entries().forEach(entry -> {
                System.out.println("Loaded: " + entry.getKey() + "=" + entry.getValue());
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
        }
    }
}
