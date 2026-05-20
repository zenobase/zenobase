package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.net.httpserver.HttpServer;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.testing.NodeAssert;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenGraphControllerTest extends ControllerTestSupport {

	private static HttpServer server;
	private static String origin;

	@BeforeAll
	public static void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		origin = "http://localhost:" + server.getAddress().getPort();
		server.createContext("/ogp", exchange -> {
			byte[] body = (
				"<html><head>" +
				"<meta property=\"og:title\" content=\"Open Graph protocol\">" +
				"<title>Open Graph protocol</title>" +
				"</head><body></body></html>"
			).getBytes();
			exchange.getResponseHeaders().set("Content-Type", "text/html");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});
		server.start();
	}

	@AfterAll
	public static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(UserRepository.class).toInstance(mock(UserRepository.class));
				bind(AuthorizationContext.class).toInstance(mock(AuthorizationContext.class));
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		OpenGraphController controller = injector.getInstance(OpenGraphController.class);
		builder.get("/og", controller::get);
	}

	@Test
	public void test() {
		String url = origin + "/ogp";
		try (Http1ClientResponse result = call(url)) {
			NodeAssert node = assertThat(result).hasStatus(200).asObjectNode();
			node.path("url").isEqualTo(url);
			node.path("title").isEqualTo("Open Graph protocol");
		}
	}

	@Test
	public void testInvalidUrl() {
		try (Http1ClientResponse result = call("")) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testInvalidHost() {
		try (Http1ClientResponse result = call("http://invalid/")) {
			assertThat(result).hasStatus(400);
		}
	}

	private Http1ClientResponse call(String url) {
		return client.get("/og").queryParam("url", url).request();
	}
}
