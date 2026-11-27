package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.io.OpenGraph;
import com.zenobase.io.PublicAddressPolicy;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the title of a web page on behalf of signed-in users. Connections are only made to public addresses: the
 * check happens in the DNS resolver, so it applies to every redirect and to the address actually connected to.
 */
public class OpenGraphController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(OpenGraphController.class);

	private static final Timeout TIMEOUT = Timeout.ofSeconds(5);
	private static final int MAX_REDIRECTS = 5;

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
		JsonNode json;
		try {
			json = http.execute(new HttpGet(url), response -> {
				if (response.getCode() != HttpStatus.SC_OK) {
					throw ResponseStatusException.badRequest("Couldn't retrieve resource: " + url);
				}
				try {
					return OpenGraph.parse(url, response.getEntity().getContent()).toJson();
				} catch (IOException e) {
					String message = "Couldn't parse resource: " + url;
					logger.warn(message);
					throw ResponseStatusException.badRequest(message);
				}
			});
		} catch (IOException e) {
			throw ResponseStatusException.badRequest("Couldn't retrieve resource: " + url);
		}
		sendOk(res, json);
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
