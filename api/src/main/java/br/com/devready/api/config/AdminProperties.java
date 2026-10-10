package br.com.devready.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.admin")
public record AdminProperties(String email, String password) {}
