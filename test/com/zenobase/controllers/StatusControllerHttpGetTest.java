package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.models.StatusInfo;

public class StatusControllerHttpGetTest extends StatusControllerTestSupport {

	@Test
	public void test() {
		ClusterHealthResponse health = mock(ClusterHealthResponse.class);
		StatusInfo expected = new StatusInfo(Long.MAX_VALUE, ClusterHealthStatus.GREEN, 4, 2, true, true); // need to use a non-integer value for correct round-tripping
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

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.get(), fakeRequest());
	}
}
