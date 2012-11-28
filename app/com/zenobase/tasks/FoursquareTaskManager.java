package com.zenobase.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import play.Logger;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task.State;

public class FoursquareTaskManager extends OAuthTaskManager {

	@Inject
	public FoursquareTaskManager(@Named("foursquare.api.key") String apiKey, @Named("foursquare.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(Foursquare2Api.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FoursquareTask.TYPE;
	}

	@Override
	public Task newTask(String bucketId, Identity principal) {
		FoursquareTask task = new FoursquareTask(bucketId, principal);
		task.setState(State.UNAUTHORIZED);
		return task;
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
		Preconditions.checkState(task.getState() == Task.State.READY, "Task is not ready: %s", task.getId());
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "imported events from foursquare", "removed events imported from foursquare");
		DateTime beforeTimestamp = new DateTime(DateTimeZone.UTC).minusMinutes(1);
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.foursquare.com/v2/users/self/checkins");
		request.addQuerystringParameter("v", "20121120");
		request.addQuerystringParameter("oauth_token", task.getToken().getToken());
		if (task.getMarker() != null) {
			request.addQuerystringParameter("afterTimestamp", Long.toString(task.getMarker().getMillis() / 1000));
		}
		request.addQuerystringParameter("beforeTimestamp", Long.toString(beforeTimestamp.getMillis() / 1000));
		request.addQuerystringParameter("limit", "100");
		FoursquareCheckinsNode result = new FoursquareCheckinsNode(parseObject(request.send()));
		FoursquareTask to = task.copy();
		to.setUpdated(new DateTime(DateTimeZone.UTC));
		to.setMarker(beforeTimestamp);
		command.add(new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to));
		for (Event event : result.getEvents()) {
			Logger.info("importing event: " + event.toJson());
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
