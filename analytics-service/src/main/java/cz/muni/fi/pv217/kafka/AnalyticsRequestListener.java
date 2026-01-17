package cz.muni.fi.pv217.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.pv217.kafka.dto.AnalyticsRequest;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class AnalyticsRequestListener {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsRequestListener.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MeterRegistry registry;

    // Gauge backing stores – 1 gauge per id+tags
    private final ConcurrentMap<String, AtomicDouble> metricValues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicDouble> metricViolations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicDouble> locationLat = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicDouble> locationLon = new ConcurrentHashMap<>();


    void onStart(@Observes StartupEvent ev) {
        LOG.info("AnalyticsRequestListener started. Consuming Kafka topic: processed-data-stream");
    }


    @Incoming("processed-data-stream")
    @Timed("data_processing_processing_time")
    @Counted("data_processing_request_count")
    public void consume(String message) {
        if (message == null || message.isBlank()) {
            LOG.warn("Received empty Kafka message – skipping");
            return;
        }

        try {
            AnalyticsRequest req = objectMapper.readValue(message, AnalyticsRequest.class);

            LOG.info("Received analytics for deviceId={}, timestamp={}",
                    req.getDeviceId(), req.getTimestamp()
            );

            pushMetrics(req);

        } catch (JsonProcessingException e) {
            LOG.error("JSON parse error: {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Processing error", e);
        }
    }


    private AtomicDouble gauge(String name, Tags tags, ConcurrentMap<String, AtomicDouble> store) {

        // Stable unique key for this metric + tags combination
        String key = name + "|" +
                tags.stream()
                        .map(t -> t.getKey() + "=" + t.getValue())
                        .sorted()
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");

        return store.computeIfAbsent(key, k -> {
            AtomicDouble ref = new AtomicDouble(0.0);
            registry.gauge(name, tags, ref); // correct Micrometer gauge form
            return ref;
        });
    }


    private void pushMetrics(AnalyticsRequest req) {

        String deviceId = req.getDeviceId().toString();

        for (AnalyticsRequest.ProcessedEventMetricTo metric : req.getMetrics()) {

            gauge(
                    "device_metric_value",
                    Tags.of("deviceId", deviceId, "metricName", metric.getMetricName()),
                    metricValues
            ).set(metric.getValue());

            gauge(
                    "device_metric_violation",
                    Tags.of("deviceId", deviceId, "metricName", metric.getMetricName()),
                    metricViolations
            ).set(metric.isViolatingSafety() ? 1.0 : 0.0);
        }

        // ---- LOCATION ----
        if (req.getLocation() != null) {

            Double lat = req.getLocation().get("latitude");
            Double lon = req.getLocation().get("longitude");

            if (lat != null && lon != null) {
                gauge(
                        "device_location",
                        Tags.of("deviceId", deviceId, "type", "latitude"),
                        locationLat
                ).set(lat);

                gauge(
                        "device_location",
                        Tags.of("deviceId", deviceId, "type", "longitude"),
                        locationLon
                ).set(lon);
            }
        }
    }
}
