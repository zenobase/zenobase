package com.zenobase.tasks.foursquare;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;

import com.zenobase.json.Nodes;

public class FoursquareVenues {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(10);

	private final String apiKey;
	private final String apiSecret;

	private final LoadingCache<String, FoursquareVenue> cache = CacheBuilder.newBuilder()
		.maximumSize(1000L)
		.expireAfterAccess(5, TimeUnit.MINUTES)
		.build(new CacheLoader<String, FoursquareVenue>() {
			@Override
			public FoursquareVenue load(String venueId) {
				HttpResponse response = request(venueId);
				int status = response.getStatusLine().getStatusCode();
				if (status == 400) {
					return FoursquareVenue.UNKNOWN;
				}
				if (status == 502) {
					response = request(venueId);
					status = response.getStatusLine().getStatusCode();
				}
				Preconditions.checkState(status == 200, "Couldn't find venue <%s>: %s", venueId, status);
				try {
					String body = EntityUtils.toString(response.getEntity());
					JsonNode json = Nodes.read(body);
					return parse(json.path("response").path("venue"));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

	@Inject
	public FoursquareVenues(@Named("foursquare.api.key") String apiKey, @Named("foursquare.api.secret") String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	public FoursquareVenue find(String venueId) {
		return cache.getUnchecked(venueId);
	}

	private HttpResponse request(String venueId) {
		RATE_LIMITER.acquire();
		try {
			String url = new URIBuilder("https://api.foursquare.com/v2/venues/" + venueId)
				.addParameter("v", "20140206")
				.addParameter("client_id", apiKey)
				.addParameter("client_secret", apiSecret)
				.build()
				.toString();
			return Request.Get(url)
				.connectTimeout(5000)
				.socketTimeout(5000)
				.execute()
				.returnResponse();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static FoursquareVenue parse(JsonNode node) {
		String id = node.path("id").textValue();
		String name = node.path("name").textValue();
		var venue = new FoursquareVenue(id, name);
		for (JsonNode categoryNode : node.path("categories")) {
			venue.addCategory(categoryNode.path("shortName").textValue());
		}
		return venue;
	}
}
