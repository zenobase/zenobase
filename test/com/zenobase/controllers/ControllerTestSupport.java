package com.zenobase.controllers;

import play.Application;
import play.GlobalSettings;
import play.libs.Json;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.zenobase.json.Nodes;

public class ControllerTestSupport extends WithApplication implements CustomHeaders {

	protected FakeApplication fakeApplication(final Module module) {

		return Helpers.fakeApplication(new GlobalSettings() {

			Injector injector = Guice.createInjector(module);

			@Override
			public void onStart(Application app) {
				Json.setObjectMapper(Nodes.MAPPER);
			}

			@Override
			public <A> A getControllerInstance(Class<A> controllerClass) {
				return injector.getInstance(controllerClass);
			}
		});
	}
}
