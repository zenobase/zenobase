package com.zenobase.controllers;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.DirectClient;

public abstract class ControllerTestSupport implements CustomHeaders {

	protected DirectClient client;
	protected Injector injector;

	protected abstract Module module();

	protected abstract void routing(HttpRouting.Builder builder, Injector injector);

	@org.junit.Before
	public void setUpClient() {
		injector = Guice.createInjector(module());
		HttpRouting.Builder routingBuilder = HttpRouting.builder();
		routing(routingBuilder, injector);
		client = new DirectClient(routingBuilder);
	}
}
