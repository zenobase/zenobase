package com.zenobase.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

/**
 * Stands in for the network behind {@link OAuthCredentialsManager#execute}: records each request and answers with
 * scripted responses, one per request.
 */
public class RecordingTransport {

	private final List<OAuthRequest> requests = new ArrayList<>();
	private final Deque<Response> responses = new ArrayDeque<>();

	public RecordingTransport reply(int code, String body) {
		responses.add(response(code, body));
		return this;
	}

	public Response execute(OAuthRequest request) {
		requests.add(request);
		if (responses.isEmpty()) {
			throw new AssertionError("Unexpected request: " + request.getCompleteUrl());
		}
		return responses.removeFirst();
	}

	public List<OAuthRequest> requests() {
		return requests;
	}

	public OAuthRequest onlyRequest() {
		assertThat(requests).hasSize(1);
		return requests.getFirst();
	}

	public static Response response(int code, String body) {
		Response response = mock(Response.class);
		when(response.getCode()).thenReturn(code);
		when(response.isSuccessful()).thenReturn(code >= 200 && code < 300);
		when(response.getBody()).thenReturn(body);
		return response;
	}

	public static Map<String, String> bodyParams(OAuthRequest request) {
		return toMap(URLEncodedUtils.parse(request.getBodyContents(), StandardCharsets.UTF_8));
	}

	public static Map<String, String> queryParams(OAuthRequest request) {
		return queryParams(URI.create(request.getCompleteUrl()));
	}

	public static Map<String, String> queryParams(URI uri) {
		return toMap(URLEncodedUtils.parse(uri, StandardCharsets.UTF_8));
	}

	/** Repeats of a parameter with the same value collapse into one entry; differing values are comma-joined. */
	private static Map<String, String> toMap(List<NameValuePair> params) {
		return params
			.stream()
			.collect(
				Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (a, b) ->
					a.equals(b) ? a : a + "," + b
				)
			);
	}
}
