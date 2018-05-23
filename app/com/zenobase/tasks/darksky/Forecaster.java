package com.zenobase.tasks.darksky;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class Forecaster {

	private final RateLimiter rateLimit = RateLimiter.create(10);
	private final String apiKey;

	@Inject
	public Forecaster(@Named("darksky.api.key") String apiKey) {
		this.apiKey = apiKey;
	}

	public Forecast find(Location location, DateTime time, Set<String> fields, boolean standardUnits) {
		rateLimit.acquire();
		WSRequestHolder request = newRequest(location, time, fields.contains(Event.MOON.getName()), standardUnits);
		WSResponse response = request.get().get(10000L);
		Preconditions.checkState(response.getStatus() == 200,
			"Couldn't request <%s>: %s", response.getUri(), response.getBody());
		ObjectNode node = Nodes.readObject(response.asByteArray());
		return new ForecastResult(node, standardUnits).get();
	}

	private WSRequestHolder newRequest(Location location, DateTime time, boolean includeDaily, boolean standardUnits) {
		String url = String.format("https://api.darksky.net/forecast/%s/%s,%s,%s",
			apiKey, location.getLatitude(), location.getLongitude(),
			time.toString(ISODateTimeFormat.dateTimeNoMillis()));
		String exclude = "minutely,hourly,alerts,flags";
		if (!includeDaily) {
			exclude += ",daily";
		}
		return WS.url(url)
			.setQueryParameter("units", standardUnits ? "si" : "us")
			.setQueryParameter("exclude", exclude);
	}
}
