package com.example.kafka.service.sync;

import org.springframework.stereotype.Component;

import com.example.kafka.service.pipeline.TransformContext;
import com.example.kafka.service.pipeline.steps.TemplateStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 * ΣΗΜΑΝΤΙΚΟ:
 *  Παίρνουμε το τελικό JSON από ctx.rendered,
 *  γιατί εκεί γράφει το TemplateStep.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncMarkerFactory {

  private final TemplateStep templateStep;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Δημιουργεί ένα SYNC_START μήνυμα (ως String JSON).
   */
  public String buildSyncStart() {
    return buildWithType("SYNC_START");
  }

  /**
   * Δημιουργεί ένα SYNC_END μήνυμα (ως String JSON).
   */
  public String buildSyncEnd() {
    return buildWithType("SYNC_END");
  }

  /**
   * Κοινή μέθοδος που χτίζει marker για οποιοδήποτε type.
   * Π.χ. "SYNC_START", "SYNC_END".
   */
  private String buildWithType(String type) {
    try {
      // Άδειο root JsonNode – δεν θα το χρησιμοποιήσουμε απευθείας,
      // αλλά χρειάζεται για τον TransformContext.
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

      // Τρέχουμε το ΙΔΙΟ TemplateStep που χρησιμοποιεί και το pipeline
      templateStep.apply(ctx);

      // ΣΗΜΑΝΤΙΚΟ:
      // Το TemplateStep γράφει το τελικό JSON στο ctx.rendered (String),
      // ΟΧΙ στο ctx.root. Άρα:
      if (ctx.rendered != null && !ctx.rendered.isBlank()) {
        return ctx.rendered;
      }

      // Fallback ασφαλείας (δεν θα έπρεπε να συμβαίνει)
      log.warn("SyncMarkerFactory: ctx.rendered was null/blank for type={}", type);
      ObjectNode fallback = mapper.createObjectNode();
      fallback.put("emsDomain", "");
      fallback.put("faultId", "");
      fallback.put("neName", "");
      fallback.put("neEquipment", " | ");
      fallback.put("type", type);
      fallback.put("neId", "");
      fallback.put("severity", "");
      fallback.put("timestamp", System.currentTimeMillis());
      fallback.set("sourceEvent", mapper.createObjectNode());
      return mapper.writeValueAsString(fallback);

    } catch (Exception e) {
      throw new RuntimeException("Failed to build sync marker '" + type + "'", e);
    }
  }
}
