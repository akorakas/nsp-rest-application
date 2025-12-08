package com.example.kafka.nsp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.kafka.service.Transformer;
import com.example.kafka.service.sync.SyncMarkerFactory;
import com.example.kafka.sink.SinkRouter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NspRestPoller {

    private final NspClient nspClient;
    private final Transformer transformer;
    private final SinkRouter sinks;

    // ✅ ΝΕΟ: factory για SYNC_START / SYNC_END μηνύματα
    private final SyncMarkerFactory syncMarkerFactory;

    @Value("${app.rest.nsp.host}")
    private String host;

    @PostConstruct
    public void logProps() {
        log.info("NSP config from YAML (via @Value): host={}", host);
    }

    // @Scheduled(fixedDelayString = "${app.rest.nsp.poll-interval-ms:60000}")
    public void pollActiveAlarms() {
        try {
            List<String> events = nspClient.fetchActiveAlarmEvents();
            log.info("NSP REST: fetched {} alarms", events.size());

            // Κοινά headers για όλα τα μηνύματα αυτής της poll παρτίδας
            Map<String, String> headers = new HashMap<>();
            headers.put("source", "NSP-REST");
            headers.put("source-host", host);

            // 1) Στείλε SYNC_START dummy message
            try {
                String syncStartJson = syncMarkerFactory.buildSyncStart();
                sinks.sendOutput(null, syncStartJson, headers);
                log.info("NSP REST: sent SYNC_START marker");
            } catch (Exception e) {
                log.error("NSP REST: failed to build/send SYNC_START marker", e);
            }

            // 2) Κανονική ροή: transform & send για κάθε alarm event
            for (String value : events) {
                try {
                    String outJson = transformer.transform(value);
                    sinks.sendOutput(null, outJson, headers);
                } catch (Exception e) {
                    // Δεν σταματά η παρτίδα αν χαλάσει ένα event
                    log.error("NSP REST: failed to transform/send single alarm event", e);
                }
            }

            // 3) Στείλε SYNC_END dummy message
            try {
                String syncEndJson = syncMarkerFactory.buildSyncEnd();
                sinks.sendOutput(null, syncEndJson, headers);
                log.info("NSP REST: sent SYNC_END marker");
            } catch (Exception e) {
                log.error("NSP REST: failed to build/send SYNC_END marker", e);
            }

        } catch (Exception e) {
            log.error("NSP REST: failed to poll or process alarms", e);
        }
    }
}
