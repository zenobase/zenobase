package com.zenobase.tasks.foursquare;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.zenobase.json.Nodes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;

public class FoursquareVenues {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(10);

	private final String apiKey;
	private final String apiSecret;

	private final LoadingCache<String, FoursquareVenue> cache = CacheBuilder.newBuilder()
		.maximumSize(1000L)
		.expireAfterAccess(5, TimeUnit.MINUTES)
		.build(
			new CacheLoader<>() {
				@Override
				public FoursquareVenue load(String venueId) {
					try (var response = request(venueId)) {
						if (response.getCode() == 400) {
							return FoursquareVenue.UNKNOWN;
						}
						if (response.getCode() == 502) {
							try (var retry = request(venueId)) {
								return readVenue(venueId, retry);
							}
						}
						return readVenue(venueId, response);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		);

	private static FoursquareVenue readVenue(String venueId, ClassicHttpResponse response) throws Exception {
		Preconditions.checkState(
			response.getCode() == 200,
			"Couldn't find venue <%s>: %s",
			venueId,
			response.getCode()
		);
		var body = EntityUtils.toString(response.getEntity());
		var json = Nodes.read(body);
		return parse(json.path("response").path("venue"));
	}

	@Inject
	public FoursquareVenues(
		@Named("foursquare.api.key") String apiKey,
		@Named("foursquare.api.secret") String apiSecret
	) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	public FoursquareVenue find(String venueId) {
		return cache.getUnchecked(venueId);
	}

	private ClassicHttpResponse request(String venueId) {
		RATE_LIMITER.acquire();
		try {
			String url = new URIBuilder("https://api.foursquare.com/v2/venues/" + venueId)
				.addParameter("v", "20140206")
				.addParameter("client_id", apiKey)
				.addParameter("client_secret", apiSecret)
				.build()
				.toString();
			return (ClassicHttpResponse) Request.get(url)
				.connectTimeout(Timeout.ofSeconds(5))
				.responseTimeout(Timeout.ofSeconds(5))
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
