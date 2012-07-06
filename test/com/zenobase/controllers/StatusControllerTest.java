package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.models.StatusInfo;
import com.zenobase.services.Cluster;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.IndexManager;

public class StatusControllerTest {

	private final IndexManager manager = mock(IndexManager.class);
	private final Cluster cluster = mock(Cluster.class);
	private final CommandRepository history = mock(CommandRepository.class);

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(mock(SecurityContext.class));
				bind(IndexManager.class).toInstance(manager);
				bind(CommandRepository.class).toInstance(history);
				requestStaticInjection(StatusController.class);
			}
		});
	}

	@Test
	public void test() {
		StatusInfo expected = new StatusInfo(Long.MAX_VALUE, ClusterHealthStatus.GREEN); // need to use a non-integer value for correct round-tripping
		when(manager.getCluster()).thenReturn(cluster);
		when(history.size()).thenReturn(expected.getCount());
		when(cluster.getHealthStatus()).thenReturn(expected.getHealth());
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent(expected.toJson());
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.get(), fakeRequest());
	}
}
