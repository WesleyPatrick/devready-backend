package br.com.devready.infrastructure.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "br.com.devready.infrastructure")
@EnableJpaRepositories(basePackages = "br.com.devready.infrastructure")
public class PersistenceConfiguration {}
