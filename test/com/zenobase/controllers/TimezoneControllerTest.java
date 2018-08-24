package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static play.test.Helpers.*;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;

import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;

public class TimezoneControllerTest extends ControllerTestSupport {

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bindConstant().annotatedWith(Names.named("google.service.key")).to("");
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
