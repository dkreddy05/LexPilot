package com.lexpilot.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

/**
 * Centralized service for recording custom application metrics via Micrometer.
 * Exported to Prometheus for monitoring dashboards.
 */
@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record a successful LLM call.
     */
    public void recordLlmCallSuccess(String model) {
        Counter.builder("lexpilot.llm.calls.total")
                .tag("model", model)
                .tag("status", "success")
                .register(registry)
                .increment();
    }

    /**
     * Record a failed LLM call.
     */
    public void recordLlmCallFailure(String model, String errorType) {
        Counter.builder("lexpilot.llm.calls.total")
                .tag("model", model)
                .tag("status", "error")
                .tag("error_type", errorType)
                .register(registry)
                .increment();
    }

    /**
     * Time an LLM API call.
     */
    public <T> T timeLlmCall(String model, Callable<T> callable) throws Exception {
        Timer timer = Timer.builder("lexpilot.llm.latency")
                .tag("model", model)
                .register(registry);
        return timer.recordCallable(callable);
    }

    /**
     * Time a vector/BM25/RRF retrieval operation.
     */
    public <T> T timeRetrieval(String searchType, Callable<T> callable) throws Exception {
        Timer timer = Timer.builder("lexpilot.retrieval.latency")
                .tag("search_type", searchType)
                .register(registry);
        return timer.recordCallable(callable);
    }

    /**
     * Time the embedding service call.
     */
    public <T> T timeEmbedding(Callable<T> callable) throws Exception {
        Timer timer = Timer.builder("lexpilot.embedding.latency")
                .register(registry);
        return timer.recordCallable(callable);
    }

    /**
     * Record total generated queries and their confidence status.
     */
    public void recordQueryGenerated(boolean lowConfidence) {
        Counter.builder("lexpilot.query.total")
                .tag("confidence", lowConfidence ? "low" : "high")
                .register(registry);
    }
}
