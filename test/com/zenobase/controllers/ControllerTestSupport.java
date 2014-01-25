package com.zenobase.controllers;

import play.GlobalSettings;
import play.libs.Json;
import play.test.Helpers;
import play.test.WithApplication;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.zenobase.json.Nodes;

public class ControllerTestSupport extends WithApplication implements CustomHeaders {

	protected void start(final Module module) {
		start(Helpers.fakeApplication(new GlobalSettings() {
			Injector injector = Guice.createInjector(module);
				@Override
				public <A> A getControllerInstance(Class<A> controllerClass) {
					return injector.getInstance(controllerClass);
				}
			})
		);
		Json.setObjectMapper(Nodes.MAPPER);
	}
}
