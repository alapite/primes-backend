package ereh.won.otbackend;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.stereotype.Component;

@Component
public class ServiceMetrics {
    private final LongCounter requestCounter;
    private final LongCounter errorCounter;
    private final DoubleHistogram responseTimeHistogram;

    public ServiceMetrics(OpenTelemetry openTelemetry) {
        // Create a Meter instance for the order service
        Meter meter = openTelemetry.getMeter("primes_service");

        this.requestCounter = meter
                .counterBuilder("requests_total")
                .setDescription("Total number of requests")
                .setUnit("requests")
                .build();

        this.errorCounter = meter
                .counterBuilder("errors_total")
                .setDescription("Total number of failed requests")
                .setUnit("errors")
                .build();

        this.responseTimeHistogram = meter
                .histogramBuilder("response_time_ms")
                .setDescription("Response time for requests")
                .setUnit("ms")
                .build();
    }

    public void recordRequest(String endpoint) {
        requestCounter.add(1, Attributes.builder().put("endpoint", endpoint).build());
    }


    public void recordError(String endpoint, String errorType) {
        errorCounter.add(
                1,
                Attributes.builder()
                        .put("endpoint", endpoint)
                        .put("error_type", errorType)
                        .build()
        );
    }


    public void recordResponseTime(String endpoint, double durationInMs) {
        responseTimeHistogram.record(durationInMs, Attributes.builder().put("endpoint", endpoint).build());
    }
}
