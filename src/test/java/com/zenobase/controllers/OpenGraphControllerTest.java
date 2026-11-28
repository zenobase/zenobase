package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zenobase.io.PublicAddressPolicy;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.testing.NodeAssert;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenGraphControllerTest extends ControllerTestSupport {

	private static final String METADATA_URL = "http://169.254.169.254/latest/meta-data/";

	private static final long LARGE_BODY_BYTES = 16L * 1024 * 1024;

	private static HttpServer server;
	private static String origin;
	private static CompletableFuture<Long> largeBodyBytesWritten;

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final PublicAddressPolicy policy = mock(PublicAddressPolicy.class);

	@BeforeAll
	public static void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		origin = "http://127.0.0.1:" + server.getAddress().getPort();
		server.createContext("/ogp", exchange ->
			respond(
				exchange,
				"text/html",
				"""
				<html><head>
				<meta property="og:title" content="Open Graph protocol">
				<title>Open Graph protocol</title>
				</head><body></body></html>
				"""
			)
		);
		server.createContext("/untyped", exchange -> respond(exchange, null, "<title>Untyped</title>"));
		server.createContext("/large", largeBody("text/html; charset=UTF-8"));
		server.createContext("/large.png", largeBody("image/png"));
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().set("Location", METADATA_URL);
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.start();
	}

	private static void respond(HttpExchange exchange, String type, String body) throws IOException {
		byte[] bytes = body.getBytes();
		if (type != null) {
			exchange.getResponseHeaders().set("Content-Type", type);
		}
		exchange.sendResponseHeaders(200, bytes.length);
		try (var out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	/** Serves a title followed by filler, recording how much was written before the client hung up. */
	private static HttpHandler largeBody(String type) {
		return exchange -> {
			exchange.getResponseHeaders().set("Content-Type", type);
			exchange.sendResponseHeaders(200, LARGE_BODY_BYTES);
			byte[] head = "<html><head><title>Large</title></head><body>".getBytes();
			byte[] chunk = new byte[64 * 1024];
			long written = 0;
			try (var out = exchange.getResponseBody()) {
				out.write(head);
				written += head.length;
				while (written < LARGE_BODY_BYTES) {
					int n = (int) Math.min(chunk.length, LARGE_BODY_BYTES - written);
					out.write(chunk, 0, n);
					written += n;
				}
			} catch (IOException hungUp) {
			} finally {
				largeBodyBytesWritten.complete(written);
			}
		};
	}

	@AfterAll
	public static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		largeBodyBytesWritten = new CompletableFuture<>();
		when(auth.current(any())).thenReturn(new Authorization(new Identity("user-1")));
		InetAddress testServer = InetAddress.getByName("127.0.0.1");
		when(policy.isAllowed(any())).thenAnswer(invocation -> testServer.equals(invocation.getArgument(0)));
	}

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(UserRepository.class).toInstance(mock(UserRepository.class));
				bind(AuthorizationContext.class).toInstance(auth);
				bind(PublicAddressPolicy.class).toInstance(policy);
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
	public void testLargeBody() throws Exception {
		try (Http1ClientResponse result = call(origin + "/large")) {
			assertThat(result).hasStatus(200).asObjectNode().path("title").isEqualTo("Large");
		}
		assertThat(largeBodyBytesWritten.get(5, TimeUnit.SECONDS)).isLessThan(LARGE_BODY_BYTES);
	}

	@Test
	public void testNonHtml() throws Exception {
		String url = origin + "/large.png";
		try (Http1ClientResponse result = call(url)) {
			assertThat(result)
				.hasStatus(400)
				.asObjectNode()
				.path("message")
				.isEqualTo("Couldn't parse resource: " + url);
		}
		assertThat(largeBodyBytesWritten.get(5, TimeUnit.SECONDS)).isLessThan(LARGE_BODY_BYTES);
	}

	@Test
	public void testMissingContentType() {
		try (Http1ClientResponse result = call(origin + "/untyped")) {
			assertThat(result).hasStatus(200).asObjectNode().path("title").isEqualTo("Untyped");
		}
	}

	@Test
	public void testUppercaseScheme() {
		try (Http1ClientResponse result = call(origin.replace("http", "HTTP") + "/ogp")) {
			assertThat(result).hasStatus(200).asObjectNode().path("title").isEqualTo("Open Graph protocol");
		}
	}

	@Test
	public void testAnonymous() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = call(origin + "/ogp")) {
			assertThat(result).hasStatus(401);
		}
		verify(policy, never()).isAllowed(any());
	}

	@Test
	public void testThirdParty() {
		when(auth.current(any())).thenReturn(
			new Authorization(new Identity("user-1"), new Identity("client-1"), "read")
		);
		try (Http1ClientResponse result = call(origin + "/ogp")) {
			assertThat(result).hasStatus(403);
		}
		verify(policy, never()).isAllowed(any());
	}

	@Test
	public void testNonPublicAddress() throws Exception {
		try (Http1ClientResponse result = call(METADATA_URL)) {
			assertThat(result)
				.hasStatus(400)
				.asObjectNode()
				.path("message")
				.isEqualTo("Couldn't retrieve resource: " + METADATA_URL);
		}
		verify(policy).isAllowed(InetAddress.getByName("169.254.169.254"));
	}

	@Test
	public void testRedirectToNonPublicAddress() throws Exception {
		try (Http1ClientResponse result = call(origin + "/redirect")) {
			assertThat(result).hasStatus(400);
		}
		verify(policy).isAllowed(InetAddress.getByName("169.254.169.254"));
	}

	@Test
	public void testInvalidUrl() {
		try (Http1ClientResponse result = call("")) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testUnsupportedScheme() {
		try (Http1ClientResponse result = call("httpx://example.com/")) {
			assertThat(result)
				.hasStatus(400)
				.asObjectNode()
				.path("message")
				.isEqualTo("Invalid URL: httpx://example.com/");
		}
	}

	@Test
	public void testMissingUrl() {
		try (Http1ClientResponse result = client.get("/og").request()) {
			assertThat(result).hasStatus(400).asObjectNode().path("message").isEqualTo("URL is required");
		}
	}

	@Test
	public void testNotFound() {
		String url = origin + "/missing";
		try (Http1ClientResponse result = call(url)) {
			assertThat(result)
				.hasStatus(400)
				.asObjectNode()
				.path("message")
				.isEqualTo("Couldn't retrieve resource: " + url);
		}
	}

	@Test
	public void testDefaultsToHttps() {
		try (Http1ClientResponse result = call("http.invalid/")) {
			assertThat(result)
				.hasStatus(400)
				.asObjectNode()
				.path("message")
				.isEqualTo("Couldn't retrieve resource: https://http.invalid/");
		}
	}

	private Http1ClientResponse call(String url) {
		return client.get("/og").queryParam("url", url).request();
	}
}
