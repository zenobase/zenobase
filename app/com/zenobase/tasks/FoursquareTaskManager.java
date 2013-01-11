package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public class FoursquareTaskManager extends OAuthTaskManager {

	private static final String API_VERSION = "20121128";
	private static final int LIMIT = 100;

	@Inject
	public FoursquareTaskManager(@Named("foursquare.api.key") String apiKey, @Named("foursquare.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new Foursquare2Api(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FoursquareTask.TYPE;
	}

	@Override
	protected Token getRequestToken(OAuthTask task) {
		return Token.empty();
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		Preconditions.checkState(!task.isEnabled(), "Task is already enabled: %s", task.getId());
		return authorize(task.as(OAuthTask.class), config);
	}

	private Command authorize(OAuthTask task, ObjectNode config) {
		String code = config.get("code").getTextValue();
		if (code == null) {
			Logger.warn(String.format("Couldn't authorize %s task <%s>: %s",
				task.getType(), task.getId(), config));
			return null;
		}
		Token token = getAccessToken(task, code);
		return UpdateTaskCommand.builder(task)
			.set(Task.AUTHORIZATION_URL, task.getAuthorizationUrl(), null)
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, task.getToken(), token)
			.build();
	}

	@Override
	public Command execute(Task task) {
		try {
			Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
			return execute(task.as(FoursquareTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(FoursquareTask task) {
		String marker = formatMarker(new DateTime(DateTimeZone.UTC).minusMinutes(1));
		List<Event> events = Lists.newArrayList();
		for (int offset = 0; execute(task, marker, offset, events); offset += LIMIT) {}
		return createCommand(task, marker, events);
	}

	static String formatMarker(DateTime time) {
		return Long.toString(time.getMillis() / 1000);
	}

	private boolean execute(FoursquareTask task, String marker, int offset, List<Event> events) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.foursquare.com/v2/users/self/checkins");
		request.addQuerystringParameter("v", API_VERSION);
		request.addQuerystringParameter("oauth_token", task.getToken().getToken());
		if (task.getMarker() != null) {
			request.addQuerystringParameter("afterTimestamp", task.getMarker());
		}
		request.addQuerystringParameter("beforeTimestamp", marker);
		request.addQuerystringParameter("offset", Integer.toString(offset));
		request.addQuerystringParameter("limit", Integer.toString(LIMIT));
		Response response = request.send();
		checkResponse(task, request, response);
		FoursquareResult result = new FoursquareResult(task.getPrincipal(), parseObject(response));
		List<Event> found = result.getEvents();
		events.addAll(found);
		return found.size() == LIMIT && result.getTotal() > offset + LIMIT;
	}

	private Command createCommand(FoursquareTask task, String marker, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran foursquare task", "reverted foursquare task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
