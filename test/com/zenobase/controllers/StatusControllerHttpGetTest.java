package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.StatusInfo;

public class StatusControllerHttpGetTest extends StatusControllerTestSupport {

	@Test
	public void testGreen() {
		StatusInfo expected = new StatusInfo(Long.MAX_VALUE, HealthStatus.Green, 4, true, true);
		HealthResponse health = buildHealthResponse(expected.getHealth(), expected.getNodes());
		when(manager.getCluster()).thenReturn(cluster);
		when(history.size()).thenReturn(expected.getCount());
		when(cluster.getHealth()).thenReturn(health);
		when(bus.isReadOnly()).thenReturn(true);
		when(bus.isSchedulerDisabled()).thenReturn(true);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(expected.toJson());
	}

	@Test
	public void testRed() {
		StatusInfo expected = new StatusInfo(Long.MAX_VALUE, HealthStatus.Red, 4, true, true);
		HealthResponse health = buildHealthResponse(expected.getHealth(), expected.getNodes());
		when(manager.getCluster()).thenReturn(cluster);
		when(history.size()).thenReturn(expected.getCount());
		when(cluster.getHealth()).thenReturn(health);
		when(bus.isReadOnly()).thenReturn(true);
		when(bus.isSchedulerDisabled()).thenReturn(true);
		Result result = call();
		assertThat(result).hasStatus(503).hasContent(expected.toJson());
	}

	private static HealthResponse buildHealthResponse(HealthStatus status, int numberOfNodes) {
		String json = String.format(
			"{\"cluster_name\":\"test\",\"status\":\"%s\",\"timed_out\":false," +
			"\"number_of_nodes\":%d,\"number_of_data_nodes\":%d," +
			"\"active_primary_shards\":0,\"active_shards\":0,\"relocating_shards\":0," +
			"\"initializing_shards\":0,\"unassigned_shards\":0," +
			"\"delayed_unassigned_shards\":0,\"number_of_pending_tasks\":0," +
			"\"number_of_in_flight_fetch\":0,\"task_max_waiting_in_queue_millis\":0," +
			"\"active_shards_percent_as_number\":100.0}",
			status.jsonValue(), numberOfNodes, numberOfNodes);
		try {
			org.opensearch.client.json.jackson.JacksonJsonpMapper mapper = new org.opensearch.client.json.jackson.JacksonJsonpMapper();
			jakarta.json.stream.JsonParser parser = mapper.jsonProvider().createParser(new java.io.StringReader(json));
			return HealthResponse._DESERIALIZER.deserialize(parser, mapper);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.get(), fakeRequest());
	}
}
