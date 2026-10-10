package br.com.devready.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "br.com.devready")
@EnableScheduling
public class DevReadyWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevReadyWorkerApplication.class, args);
    }
}
