package com.example.kafka.nsp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.kafka.service.Transformer;
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

            for (String value : events) {
                String outJson = transformer.transform(value);

                Map<String, String> headers = new HashMap<>();
                headers.put("source", "NSP-REST");
                headers.put("source-host", host);

                sinks.sendOutput(null, outJson, headers);
            }
        } catch (Exception e) {
            log.error("NSP REST: failed to poll or process alarms", e);
        }
    }
}
