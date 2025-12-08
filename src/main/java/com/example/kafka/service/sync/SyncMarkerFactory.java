package com.example.kafka.service.sync;

import org.springframework.stereotype.Component;

import com.example.kafka.service.pipeline.TransformContext;
import com.example.kafka.service.pipeline.steps.TemplateStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * SyncMarkerFactory
 *
 * Χρησιμοποιεί το ΙΔΙΟ TemplateStep με το κανονικό pipeline
 * για να φτιάξει δύο ειδικά μηνύματα:
 *
 *  - type = "SYNC_START"
 *  - type = "SYNC_END"
 *
 * Τα υπόλοιπα πεδία είναι κενά (""), εκτός από:
 *  - timestamp = System.currentTimeMillis()
 *  - sourceEvent = {} (κενό JSON object)
 *
 * Έτσι αν αλλάξεις το template στο YAML, αλλάζει αυτόματα
 * και η μορφή των SYNC_START / SYNC_END.
 */
@Component
public class SyncMarkerFactory {

  private final TemplateStep templateStep;
  private final ObjectMapper mapper = new ObjectMapper();

  public SyncMarkerFactory(TemplateStep templateStep) {
    this.templateStep = templateStep;
  }

  /** Δημιουργεί ένα SYNC_START μήνυμα (ως String JSON). */
  public String buildSyncStart() throws Exception {
    return buildWithType("SYNC_START");
  }

  /** Δημιουργεί ένα SYNC_END μήνυμα (ως String JSON). */
  public String buildSyncEnd() throws Exception {
    return buildWithType("SYNC_END");
  }

  /** Κοινή μέθοδος που χτίζει marker για οποιοδήποτε type. */
  private String buildWithType(String type) throws Exception {
    // Άδειο root JsonNode
    ObjectNode emptyRoot = mapper.createObjectNode();
    TransformContext ctx = new TransformContext(emptyRoot);

    // Όλα κενά / null, εκτός από type + timestamp
    ctx.put("emsDomain", "");
    ctx.put("faultId", "");
    ctx.put("neName", "");
    ctx.put("neId", "");
    ctx.put("affectedObjectName", "");
    ctx.put("severity", "");
    ctx.put("type", type);

    // Unix timestamp now (ms)
    ctx.put("timestamp", System.currentTimeMillis());

    // Κενό sourceEvent = {}
    ObjectNode emptySourceEvent = mapper.createObjectNode();
    ctx.put("sourceEvent", emptySourceEvent);

    // Τρέχουμε το ίδιο TemplateStep με το pipeline
    templateStep.apply(ctx);

    JsonNode out = ctx.root;   // ή ctx.getRoot() αν έχεις getter

    return mapper.writeValueAsString(out);
  }
}
