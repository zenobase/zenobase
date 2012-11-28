package com.zenobase.tasks;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import com.google.common.collect.Lists;
import com.google.inject.name.Named;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class FoursquareTaskManager extends OAuthTaskManager {

	public FoursquareTaskManager(@Named("fourquare.api.key") String apiKey, @Named("foursquare.api.secret") String apiSecret, @Named("hostname") String callbackUrl) {
		super(Foursquare2Api.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FoursquareTask.TYPE;
	}

	@Override
	public Task newTask(String bucketId, Identity principal) {
		return new FoursquareTask(bucketId, principal);
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		OAuthTask to = new OAuthTask(task.copy().toJson());
		String verifier = config.get("code").getTextValue();
		to.setToken(getAccessToken(to, verifier));
		to.setState(Task.State.READY);
		return new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to);
	}

	@Override
	public Command execute(Task task) {
		return execute(new FoursquareTask(task.toJson()));
	}

	private Command execute(FoursquareTask task) {
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
