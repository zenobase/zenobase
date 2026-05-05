package com.zenobase.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

public class JvmMetricsEmfTaskTest {

	@Test
	public void emitsValidEmfDocumentToStdout() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(captured));
			new JvmMetricsEmfTask().emit();
		} finally {
			System.setOut(originalOut);
		}

		String raw = captured.toString();
		assertThat(raw.lines().count()).isEqualTo(1L);

		JsonNode root = new ObjectMapper().readTree(raw.trim());

		JsonNode cwm = root.get("_aws").get("CloudWatchMetrics").get(0);
		assertThat(cwm.get("Namespace").asText()).isEqualTo("Zenobase/Jvm");

		JsonNode dimensionSet = cwm.get("Dimensions").get(0);
		assertThat(dimensionSet).hasSize(1);
		assertThat(dimensionSet.get(0).asText()).isEqualTo("Service");

		JsonNode metricDefs = cwm.get("Metrics");
		assertThat(metricDefs).hasSize(6);

		assertThat(root.get("Service").asText()).isEqualTo("zenobase-api");
		assertThat(root.get("JvmHeapUsed").asLong()).isPositive();
		assertThat(root.get("JvmHeapCommitted").asLong()).isPositive();
		assertThat(root.get("JvmNonHeapUsed").asLong()).isPositive();
		assertThat(root.get("JvmThreadsLive").asLong()).isPositive();
		assertThat(root.get("JvmGcPauseCumulativeMs").asLong()).isNotNegative();
		assertThat(root.get("JvmUptime").asLong()).isNotNegative();
	}
}
