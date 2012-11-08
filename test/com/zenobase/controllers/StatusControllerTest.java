package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.junit.Before;
import org.junit.Test;
import play.GlobalSettings;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.zenobase.models.StatusInfo;
import com.zenobase.services.Cluster;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.IndexManager;

public class StatusControllerTest extends WithApplication {

	private final IndexManager manager = mock(IndexManager.class);
	private final Cluster cluster = mock(Cluster.class);
	private final CommandRepository history = mock(CommandRepository.class);

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(mock(SecurityContext.class));
				bind(IndexManager.class).toInstance(manager);
				bind(CommandRepository.class).toInstance(history);
				bind(StatusController.class);
			}
		});
	}

	protected void start(final Module module) {
		start(Helpers.fakeApplication(new GlobalSettings() {
			Injector injector = Guice.createInjector(module);
			@Override
			public <A> A getControllerInstance(Class<A> controllerClass) {
				return injector.getInstance(controllerClass);
			}
		}));
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
