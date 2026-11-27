package com.zenobase.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.filters.SecurityHeadersFilter;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.testing.junit5.DirectClient;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** A thrown exception must produce exactly the response of the {@code sendX} call it replaced. */
public class ResponseStatusExceptionTest {

	private record Case(Supplier<ResponseStatusException> thrown, Consumer<ServerResponse> sent) {}

	// What the removed sendUnauthorized(res) and sendNotFound(res, message) helpers wrote.
	private static final List<Case> CASES = List.of(
		new Case(ResponseStatusException::unauthorized, res -> res.status(Status.UNAUTHORIZED_401).send()),
		new Case(
			() -> ResponseStatusException.notFound("user not found"),
			res -> ControllerSupport.sendError(res, Status.NOT_FOUND_404, "user not found")
		)
	);

	@Test
	public void testThrownMatchesSent() {
		HttpRouting.Builder routing = HttpRouting.builder()
			.addFilter(new SecurityHeadersFilter())
			.error(ResponseStatusException.class, (req, res, e) -> e.send(res));
		for (int i = 0; i < CASES.size(); i++) {
			Case c = CASES.get(i);
			routing.get("/thrown/" + i, (req, res) -> {
				throw c.thrown().get();
			});
			routing.get("/sent/" + i, (req, res) -> c.sent().accept(res));
		}
		DirectClient client = new DirectClient(routing);
		for (int i = 0; i < CASES.size(); i++) {
			try (
				Http1ClientResponse thrown = client.get("/thrown/" + i).request();
				Http1ClientResponse sent = client.get("/sent/" + i).request()
			) {
				assertThat(thrown.status()).isEqualTo(sent.status());
				assertThat(thrown.headers().value(HeaderNames.CONTENT_TYPE)).isEqualTo(
					sent.headers().value(HeaderNames.CONTENT_TYPE)
				);
				assertThat(thrown.headers().value(HeaderNames.CACHE_CONTROL)).contains("no-store");
				assertThat(body(thrown)).isEqualTo(body(sent));
			}
		}
	}

	private static String body(Http1ClientResponse response) {
		return response.entity().hasEntity() ? response.as(String.class) : "";
	}
}
