package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.services.CommandRepository;

public class StatusControllerTest {

	private final CommandRepository history = mock(CommandRepository.class);

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(mock(SecurityContext.class));
				bind(CommandRepository.class).toInstance(history);
				requestStaticInjection(StatusController.class);
			}
		});
	}

	@Test
	public void test() {
		when(history.size()).thenReturn(1L);
		Result result = call();
		assertThat(result).hasStatus(OK).hasContent("1");
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.get(), fakeRequest());
	}
}
