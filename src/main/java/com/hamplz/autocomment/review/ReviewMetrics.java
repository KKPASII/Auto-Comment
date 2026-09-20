package com.hamplz.autocomment.review;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ReviewMetrics {

    private final Counter successCounter;
    private final Counter partialFailedCounter;
    private final Counter failedCounter;
    private final MeterRegistry meterRegistry;
    private final Timer processingTimer;

    public ReviewMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.successCounter = Counter.builder("review.jobs")
            .description("Number of review jobs")
            .tag("result", "success")
            .register(meterRegistry);

        this.partialFailedCounter = Counter.builder("review.jobs")
            .description("Number of review jobs")
            .tag("result", "partial_failed")
            .register(meterRegistry);

        this.failedCounter = Counter.builder("review.jobs")
            .description("Number of review jobs")
            .tag("result", "failed")
            .register(meterRegistry);

        this.processingTimer = Timer.builder("review.job.duration")
            .description("Review job processing duration")
            .publishPercentileHistogram()
            .register(meterRegistry);
    }

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordPartialFailed() {
        partialFailedCounter.increment();
    }

    public void recordFailed() {
        failedCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
}