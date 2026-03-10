package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.StatusInfo;

public class StatusControllerHttpGetTest extends StatusControllerTestSupport {

	@Test
	public void testGreen() {
		ClusterHealthResponse health = mock(ClusterHealthResponse.class);
		StatusInfo expected = new StatusInfo(Long.MAX_VALUE, ClusterHealthStatus.GREEN, 4, 2, true, true);
		when(health.getStatus()).thenReturn(expected.getHealth());
		when(health.getNumberOfNodes()).thenReturn(expected.getNodes());
		when(manager.getCluster()).thenReturn(cluster);
		when(history.size()).thenReturn(expected.getCount());
		when(cluster.getHealth()).thenReturn(health);
		when(bus.count()).thenReturn(2);
		when(bus.isReadOnly()).thenReturn(true);
		when(bus.isSchedulerDisabled()).thenReturn(true);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(expected.toJson());
	}

	@Test
	public void testRed() {
		ClusterHealthResponse health = mock(ClusterHealthResponse.class);
		StatusInfo expected = new StatusInfo(Long.MAX_VALUE, ClusterHealthStatus.RED, 4, 2, true, true);
		when(health.getStatus()).thenReturn(expected.getHealth());
		when(health.getNumberOfNodes()).thenReturn(expected.getNodes());
		when(manager.getCluster()).thenReturn(cluster);
		when(history.size()).thenReturn(expected.getCount());
		when(cluster.getHealth()).thenReturn(health);
		when(bus.count()).thenReturn(2);
		when(bus.isReadOnly()).thenReturn(true);
		when(bus.isSchedulerDisabled()).thenReturn(true);
		Result result = call();
		assertThat(result).hasStatus(503).hasContent(expected.toJson());
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.get(), fakeRequest());
	}
}
