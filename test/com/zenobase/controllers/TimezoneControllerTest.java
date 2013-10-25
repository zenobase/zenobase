package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;

public class TimezoneControllerTest extends ControllerTestSupport {

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {

			}
		});
	}

	@Test
	public void test() {
		Result result = call();
		assertThat(result).hasStatus(OK).asArrayNode().path(0).isEqualTo("Africa/Abidjan");
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.TimezoneListController.get(null, null), fakeRequest());
	}
}
