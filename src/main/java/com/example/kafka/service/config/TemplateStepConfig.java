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
          "sourceEms": "NSP_ATNOI",
          "emsVendorID": "NSP",
          "emsDomain": "${emsDomainNormalized}",
          "serialNo": "${serialNo}",
          "faultId": "${faultId}",
          "neName": "${neName}",
          "neEquipment": "${neId} | ${affectedObjectName}",
          "type": "${type}",
          "severity": "${severity}",
          "timestamp": ${timestamp},
          "sourceEvent": ${sourceEvent},
          "metadata": {
            "neId": "${neId}",
            "fdn": "${fdn}",
            "objectId": "${objectId}",
            "emsDomainRaw": "${emsDomain}",
            "probableCause": "${probableCause}",
            "alarmType": "${alarmType}",
            "impact": ${impact},
            "serviceAffecting": ${serviceAffecting},
            "objectFullName": "${objectFullName}"
          },
          "enrichedData": null,
          "alarmIdentifier": "${alarmIdentifier}"
        }
        """;

    // Το target στο YAML είναι "$", άρα το κρατάμε ίδιο
    String target = "$";

    return new TemplateStep(template, target);
  }
}
