package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;
import com.google.inject.AbstractModule;

public class TimezoneControllerTest extends ControllerTestSupport {


	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {

			}
		});
	}

	@Test
	public void testList() {
		Result result = call(null, null);
		assertThat(result).hasStatus(OK).asArrayNode().path(0).isEqualTo("Africa/Abidjan");
	}

	@Test
	public void testFind() {
		Result result = call("47.61", "-122.33");
		assertThat(result).hasStatus(OK).asObjectNode().path("timeZoneId").isEqualTo("America/Los_Angeles");
	}

	private static Result call(String lat, String lon) {
		return callAction(com.zenobase.controllers.routes.ref.TimezoneController.get(lat, lon), fakeRequest());
	}
}
