package br.com.devready.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "br.com.devready")
public class DevReadyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevReadyApiApplication.class, args);
    }
}
