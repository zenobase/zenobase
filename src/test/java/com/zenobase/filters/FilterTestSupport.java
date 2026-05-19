package com.zenobase.filters;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.DirectClient;
import org.junit.jupiter.api.BeforeEach;

public abstract class FilterTestSupport {

	protected DirectClient client;

	@BeforeEach
	public void setUpClient() {
		HttpRouting.Builder routing = HttpRouting.builder();
		configureFilters(routing);
		configureRoutes(routing);
		client = new DirectClient(routing);
	}

	protected abstract void configureFilters(HttpRouting.Builder routing);

	protected void configureRoutes(HttpRouting.Builder routing) {
		routing.get("/ping", (req, res) -> res.send("pong"));
		routing.post("/ping", (req, res) -> res.send("pong"));
	}

	/**
	 * Make one request through the filter chain and assert that a real HTTP status is returned.
	 */
	protected void ping() {
		try (Http1ClientResponse r = client.get("/ping").request()) {
			assertThat(r.status().code()).isBetween(100, 599);
		}
	}
}
