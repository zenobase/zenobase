package com.zenobase.tasks.foursquare;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

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
				WSResponse response = request(venueId);
				if (response.getStatus() == 400) {
					return FoursquareVenue.UNKNOWN;
				}
				if (response.getStatus() == 502) {
					response = request(venueId);
				}
				Preconditions.checkState(response.getStatus() == 200, "Couldn't find venue <%s>: %s", venueId, response.getStatus());
				return parse(response.asJson().path("response").path("venue"));
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

	private WSResponse request(String venueId) {
		RATE_LIMITER.acquire();
		return WS.url("https://api.foursquare.com/v2/venues/" + venueId)
			.setQueryParameter("v", "20140206")
			.setQueryParameter("client_id", apiKey)
			.setQueryParameter("client_secret", apiSecret)
			.get().get(5000L);
	}

	private static FoursquareVenue parse(JsonNode node) {
		String id = node.path("id").textValue();
		String name = node.path("name").textValue();
		FoursquareVenue venue = new FoursquareVenue(id, name);
		for (JsonNode categoryNode : node.path("categories")) {
			venue.addCategory(categoryNode.path("shortName").textValue());
		}
		return venue;
	}
}
