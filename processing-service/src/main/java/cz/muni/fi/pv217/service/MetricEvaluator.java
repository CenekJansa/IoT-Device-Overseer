package cz.muni.fi.pv217.service;

import cz.muni.fi.pv217.entity.vo.ProcessedEventMetric;
import cz.muni.fi.pv217.entity.vo.ProcessedEventVo;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MetricEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(MetricEvaluator.class);

    /**
     * Processes the event by evaluating each metric against its healthy range.
     * Sets isViolatingSafety flag if the value is outside the healthy range (from-to).
     *
     * @param event The processed event containing metrics to evaluate
     */
    public void process(ProcessedEventVo event) {
        if (event == null) {
            LOG.warn("Received null event, skipping processing");
            return;
        }

        if (event.getMetrics() == null || event.getMetrics().isEmpty()) {
            LOG.warn("Event from device {} has no metrics to process", event.getDeviceId());
            return;
        }

        LOG.debug("Processing {} metrics for device {} at timestamp {}",
                  event.getMetrics().size(), event.getDeviceId(), event.getTimestamp());

        for (ProcessedEventMetric metric : event.getMetrics()) {
            processMetric(metric);
        }
    }

    /**
     * Processes a single metric by evaluating safety violations.
     *
     * @param metric The metric to process
     */
    private void processMetric(ProcessedEventMetric metric) {
        if (metric == null) {
            LOG.warn("Encountered null metric, skipping");
            return;
        }

        String metricName = metric.getMetricName();
        Double value = metric.getValue();

        // Check for safety violations
        boolean violatesSafety = evaluateSafetyViolation(metric);
        metric.setViolatingSafety(violatesSafety);

        if (violatesSafety) {
            LOG.warn("Safety violation detected for metric '{}': value={}, healthy range=[{}, {}]",
                     metricName, value, metric.getFrom(), metric.getTo());
        }

        LOG.debug("Processed metric '{}': value={}, violatesSafety={}",
                  metricName, value, violatesSafety);
    }

    /**
     * Evaluates if the metric value violates the safety range.
     * A violation occurs when the value is outside the healthy range [from, to].
     *
     * @param metric The metric to evaluate
     * @return true if the value violates safety, false otherwise
     */
    private boolean evaluateSafetyViolation(ProcessedEventMetric metric) {
        Double value = metric.getValue();
        Double from = metric.getFrom();
        Double to = metric.getTo();

        if (value == null || from == null || to == null) {
            LOG.debug("Skipping safety evaluation for metric '{}': missing value or range boundaries",
                      metric.getMetricName());
            return false;
        }

        // Value is violating safety if it's outside the healthy range [from, to]
        return value < from || value > to;
    }
}
