package com.zenobase.actions;

import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.sentry.Sentry;
import io.sentry.metrics.MetricsUnit;
import io.sentry.metrics.SentryMetricsParameters;
import java.util.Map;

public class MetricsFilter implements Filter {

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		long startNanos = System.nanoTime();
		try {
			chain.proceed();
		} finally {
			double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
			Sentry.metrics().distribution(
				"http.server.duration",
				elapsedMs,
				MetricsUnit.Duration.MILLISECOND,
				SentryMetricsParameters.create(
					Map.of(
						"method",
						req.prologue().method().text(),
						"route",
						req.matchingPattern().orElse("unknown"),
						"status",
						String.valueOf(res.status().code())
					)
				)
			);
		}
	}
}
