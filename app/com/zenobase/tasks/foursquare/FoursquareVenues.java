package com.zenobase.tasks.foursquare;

import javax.inject.Inject;
import javax.inject.Named;

import play.libs.WS;
import play.libs.WS.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

public class FoursquareVenues {

	private final String apiKey;
	private final String apiSecret;

	@Inject
	public FoursquareVenues(@Named("foursquare.api.key") String apiKey, @Named("foursquare.api.secret") String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	public FoursquareVenue find(String venueId) {
		Response response = WS.url("https://api.foursquare.com/v2/venues/" + venueId)
			.setQueryParameter("v", "20140206")
			.setQueryParameter("client_id", apiKey)
			.setQueryParameter("client_secret", apiSecret)
			.get().get(5000L);
		if (response.getStatus() == 400) {
			return null;
		}
		Preconditions.checkState(response.getStatus() == 200, "Couldn't find venue <%s>: %s", venueId, response.getBody());
		JsonNode venueNode = response.asJson().path("response").path("venue");
		String name = venueNode.path("name").textValue();
		FoursquareVenue venue = new FoursquareVenue(venueId, name);
		for (JsonNode categoryNode : venueNode.path("categories")) {
			venue.addCategory(categoryNode.path("shortName").textValue());
		}
		return venue;
	}
}
