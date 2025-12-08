package com.example.kafka.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.kafka.service.pipeline.steps.TemplateStep;

/**
 * Δημιουργεί ένα TemplateStep bean για χρήση από:
 *  - το κύριο transform pipeline
 *  - το SyncMarkerFactory (SYNC_START / SYNC_END)
 *
 * ΠΡΟΣΟΧΗ:
 *  Το template εδώ πρέπει να είναι το ίδιο με αυτό
 *  που έχεις στο transform.pipeline στο application.yml.
 */
@Configuration
public class TemplateStepConfig {

  @Bean
  public TemplateStep templateStep() {
    String template = """
        {
          "emsDomain": "${emsDomain}",
          "faultId": "${faultId}",
          "neName": "${neName}",
          "neEquipment": "${neId} | ${affectedObjectName}",
          "type": "${type}",
          "neId": "${neId}",
          "severity": "${severity}",
          "timestamp": ${timestamp},
          "sourceEvent": ${sourceEvent}
        }
        """;

    // Το target στο YAML είναι "$", άρα το κρατάμε ίδιο
    String target = "$";

    return new TemplateStep(template, target);
  }
}
