package com.zenobase.controllers;

import play.GlobalSettings;
import play.test.Helpers;
import play.test.WithApplication;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ControllerTestSupport extends WithApplication {

	protected void start(final Module module) {
		start(Helpers.fakeApplication(new GlobalSettings() {
			Injector injector = Guice.createInjector(module);
			@Override
			public <A> A getControllerInstance(Class<A> controllerClass) {
				return injector.getInstance(controllerClass);
			}
		}));
	}
}
