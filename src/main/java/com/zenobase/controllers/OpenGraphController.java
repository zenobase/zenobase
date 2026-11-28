package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.io.OpenGraph;
import com.zenobase.io.PublicAddressPolicy;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the title of a web page on behalf of signed-in users. Connections are only made to public addresses: the
 * check happens in the DNS resolver, so it applies to every redirect and to the address actually connected to. Only
 * the start of an HTML response is read (the title lives in the head), and the whole exchange has a deadline.
 */
public class OpenGraphController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(OpenGraphController.class);

	private static final Timeout TIMEOUT = Timeout.ofSeconds(5);
	private static final int MAX_REDIRECTS = 5;
	private static final long DEADLINE_SECONDS = 10;
	private static final int MAX_BYTES = 512 * 1024;
	private static final Set<String> HTML_TYPES = Set.of("text/html", "application/xhtml+xml");
	private static final ScheduledThreadPoolExecutor DEADLINES = new ScheduledThreadPoolExecutor(
		1,
		Thread.ofPlatform().name("og-deadline").daemon().factory()
	);

	static {
		DEADLINES.setRemoveOnCancelPolicy(true);
	}

	private final CloseableHttpClient http;

	@Inject
	public OpenGraphController(AuthorizationContext security, PublicAddressPolicy policy) {
		super(security);
		var resolver = new SystemDefaultDnsResolver() {
			@Override
			public InetAddress[] resolve(String host) throws UnknownHostException {
				InetAddress[] addresses = super.resolve(host);
				for (InetAddress address : addresses) {
					if (!policy.isAllowed(address)) {
						throw new UnknownHostException("Non-public address for " + host);
					}
				}
				return addresses;
			}
		};
		this.http = HttpClients.custom()
			.setConnectionManager(
				PoolingHttpClientConnectionManagerBuilder.create()
					.setDnsResolver(resolver)
					.setDefaultConnectionConfig(
						ConnectionConfig.custom().setConnectTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build()
					)
					.build()
			)
			.setDefaultRequestConfig(
				RequestConfig.custom().setResponseTimeout(TIMEOUT).setMaxRedirects(MAX_REDIRECTS).build()
			)
			.build();
	}

	public void get(ServerRequest req, ServerResponse res) {
		requireFirstParty(requireAuthorization(req));
		String url = req.query().first("url").orElse(null);
		if (url == null) {
			throw ResponseStatusException.badRequest("URL is required");
		}
		get(res, url.contains("://") ? url : "https://" + url);
	}

	private void get(ServerResponse res, String url) {
		if (!isValid(url)) {
			throw ResponseStatusException.badRequest("Invalid URL: " + url);
		}
		HttpGet request = new HttpGet(url);
		ScheduledFuture<?> deadline = DEADLINES.schedule(request::cancel, DEADLINE_SECONDS, TimeUnit.SECONDS);
		JsonNode json;
		try {
			json = http.execute(request, response -> {
				try {
					return parse(url, response);
				} finally {
					// Drop the connection rather than letting the client drain the rest of the body
					request.cancel();
				}
			});
		} catch (IOException e) {
			throw ResponseStatusException.badRequest("Couldn't retrieve resource: " + url);
		} finally {
			deadline.cancel(false);
		}
		sendOk(res, json);
	}

	private static JsonNode parse(String url, ClassicHttpResponse response) throws IOException {
		if (response.getCode() != HttpStatus.SC_OK) {
			throw ResponseStatusException.badRequest("Couldn't retrieve resource: " + url);
		}
		HttpEntity entity = response.getEntity();
		if (entity == null || !isHtml(entity.getContentType())) {
			throw ResponseStatusException.badRequest("Couldn't parse resource: " + url);
		}
		byte[] head = entity.getContent().readNBytes(MAX_BYTES);
		try {
			return OpenGraph.parse(url, new ByteArrayInputStream(head)).toJson();
		} catch (IOException e) {
			String message = "Couldn't parse resource: " + url;
			logger.warn(message);
			throw ResponseStatusException.badRequest(message);
		}
	}

	private static boolean isHtml(String contentType) {
		ContentType type = ContentType.parseLenient(contentType);
		return type == null || HTML_TYPES.contains(type.getMimeType().toLowerCase(Locale.ROOT));
	}

	private static boolean isValid(String url) {
		try {
			URI uri = URI.create(url);
			return (
				("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) &&
				uri.getHost() != null
			);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
