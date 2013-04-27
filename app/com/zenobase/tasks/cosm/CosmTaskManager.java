package com.zenobase.tasks.cosm;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.oauth.OAuth2Token;
import com.zenobase.tasks.InvalidTokenException;
import com.zenobase.tasks.OAuthTask;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class CosmTaskManager extends OAuthTaskManager {

	@Inject
	public CosmTaskManager(@Named("cosm.api.key") String apiKey, @Named("cosm.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new CosmApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return CosmTask.TYPE;
	}

	@Override
	protected Token getRequestToken(OAuthTask task) {
		return Token.empty();
	}

	@Override
	protected OAuthService getService(OAuthTask task) {
		return super.getService(task);
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
		OAuth2Token token = (OAuth2Token) getAccessToken(task, code);
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
			return execute(task.as(CosmTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(CosmTask task) {
		Token token = task.getToken();
		DateTime to = new DateTime(DateTimeZone.UTC).minusMinutes(1);
		List<Event> events = getEvents(task, to);
		return createCommand(task, events, token);
	}

	private List<Event> getEvents(CosmTask task, DateTime to) {
		List<Event> events = Lists.newArrayList();
		FeedQuery request = new FeedQuery(task);
		while (true) {
			DateTime from = null;
			if (!events.isEmpty()) {
				from = getNextTimestamp(events);
			} else if (task.getMarker() != null) {
				from = DateTime.parse(task.getMarker());
			} else {
				from = to.minusHours(6);
			}
			if (!events.addAll(request.find(from, to).getEvents())) {
				break;
			}
		}
		return events;
	}

	private class FeedQuery {

		private final CosmTask task;

		public FeedQuery(CosmTask task) {
			this.task = task;
		}

		public FeedResult find(DateTime from, DateTime to) {
			String url = String.format("http://api.cosm.com/v2/feeds/%s.json", task.getFeedId());
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			request.addQuerystringParameter("key", task.getToken().getToken());
			if (from != null) {
				request.addQuerystringParameter("start", from.toString());
			}
			request.addQuerystringParameter("end", to.toString());
			request.addQuerystringParameter("limit", "1000");
			request.addQuerystringParameter("interval", "0");
			Response response = send(request);
			checkResponse(task, request, response);
			return new FeedResult(task.getPrincipal(), parseObject(response));
		}
	}

	private Command createCommand(CosmTask task, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran netatmo task", "reverted netatmo task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getNextTimestamp(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, expiredToken, task.getToken())
			.build());
		for (Event event : events) {
			System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static DateTime getNextTimestamp(List<Event> events) {
		return Iterables.getLast(events).getValue(Event.TIMESTAMP).plusMillis(1);
	}

	Response send(OAuthRequest request) {
		return request.send();
	}
}
