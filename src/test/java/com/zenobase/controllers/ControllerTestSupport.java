package com.zenobase.controllers;

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.DirectClient;

public abstract class ControllerTestSupport implements CustomHeaders {

	protected DirectClient client;

	protected abstract void routing(HttpRouting.Builder builder);

	@org.junit.jupiter.api.BeforeEach
	public void setUpClient() {
		HttpRouting.Builder routingBuilder = HttpRouting.builder();
		routing(routingBuilder);
		client = new DirectClient(routingBuilder);
	}
}
