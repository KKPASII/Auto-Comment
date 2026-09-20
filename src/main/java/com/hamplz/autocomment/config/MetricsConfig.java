package com.hamplz.autocomment.config;

import com.hamplz.autocomment.review.service.ReviewJobQueueService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(
        MeterRegistry meterRegistry,
        ReviewJobQueueService reviewJobQueueService
    ) {
        Gauge.builder(
                "review.queue.size",
                reviewJobQueueService,
                ReviewJobQueueService::waitingQueueSize
            )
            .description("Number of review jobs waiting in Redis queue")
            .tag("state", "waiting")
            .register(meterRegistry);

        Gauge.builder(
                "review.queue.size",
                reviewJobQueueService,
                ReviewJobQueueService::processingQueueSize
            )
            .description("Number of review jobs currently processing")
            .tag("state", "processing")
            .register(meterRegistry);
    }
}
