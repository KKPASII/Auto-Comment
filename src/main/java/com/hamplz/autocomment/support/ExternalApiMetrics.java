package com.hamplz.autocomment.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ExternalApiMetrics {

    private final Map<ExternalApiOperation, Counter> retryCounters =
        new EnumMap<>(ExternalApiOperation.class);

    public ExternalApiMetrics(MeterRegistry meterRegistry) {
        for (ExternalApiOperation operation : ExternalApiOperation.values()) {
            Counter counter = Counter.builder("external.api.retry")
                .description("Number of external API retries")
                .tag("provider", providerOf(operation))
                .tag("operation", operation.logName())
                .register(meterRegistry);

            retryCounters.put(operation, counter);
        }
    }

    public void recordRetry(ExternalApiOperation operation) {
        retryCounters.get(operation).increment();
    }

    private String providerOf(ExternalApiOperation operation) {
        if (operation.name().startsWith("GITHUB")) {
            return "github";
        }

        if (operation.name().startsWith("OPENAI")) {
            return "openai";
        }

        return "unknown";
    }
}