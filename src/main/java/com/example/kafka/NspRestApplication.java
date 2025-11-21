package com.example.kafka;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import com.example.kafka.nsp.NspRestPoller;

@SpringBootApplication
// @EnableScheduling
public class NspRestApplication {

  public static void main(String[] args) {
    SpringApplication.run(NspRestApplication.class, args);
  }
    @Bean
    public ApplicationRunner runOnceAndExit(NspRestPoller poller,
                                            ConfigurableApplicationContext ctx) {
        return args -> {
            // 1) Τρέχουμε ΜΙΑ φορά το NSP poll + transform + sinks
            poller.pollActiveAlarms();

            // 2) Κλείνουμε καθαρά το Spring context → τερματίζει το jar
            SpringApplication.exit(ctx, () -> 0);
        };
    }
}
