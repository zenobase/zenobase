package com.zenobase.tasks;

import java.util.List;

import org.scribe.builder.api.Foursquare2Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;

public class FoursquareTaskManager extends OAuthTaskManager<FoursquareTask> {

	public FoursquareTaskManager(String apiKey, String apiSecret, String callbackUrl) {
		super(Foursquare2Api.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public Command execute(FoursquareTask task) {
		List<Event> events = Lists.newArrayList();
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.foursquare.com/v2/users/self/checkins");
		request.addQuerystringParameter("v", "20121120");
		request.addQuerystringParameter("oauth_token", task.getToken().getToken());
		if (task.getMarker() != null) {
			request.addQuerystringParameter("afterTimestamp", Long.toString(task.getMarker().getMillis() / 1000));
		}
		request.addQuerystringParameter("limit", "100");
		events.addAll(new FoursquareCheckinsNode(parseObject(request.send())).getEvents());
		for (Event event : events) {
			System.out.println("event: " + event.toJson());
		}
		return null; // TODO
	}
}
