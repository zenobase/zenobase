package com.zenobase.tasks.ihealth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

abstract class IHealthTaskManagerSupport<T extends IHealthTaskSupport> extends OAuthTaskManager {

	protected static final String HOST = "https://api.ihealthlabs.com:8443/openapiv2";

	private final Map<String, ResultHandler<T>> handlers = Maps.newLinkedHashMap();
	private final Map<String, String> svs = Maps.newLinkedHashMap();
	private final Class<T> taskClass;

	protected IHealthTaskManagerSupport(String type, IHealthCredentialsManager credentialsManager, Class<T> taskClass) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	protected void register(String path, String sv, ResultHandler<T> handler) {
		handlers.put(path, handler);
		svs.put(path, sv);
	}

	interface ResultHandler<T extends IHealthTaskSupport> {
		IHealthResultSupport process(T task, ObjectNode result);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(taskClass), credentials);
	}

	private Command execute(T task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = new ArrayList<>();
		for (Map.Entry<String, ResultHandler<T>> entry : handlers.entrySet()) {
			execute(entry.getKey(), task, entry.getValue(), credentials, events);
		}
		return createCommand(task, credentials, events, token);
	}

	private void execute(
			String resource, T task, ResultHandler<T> handler, OAuthCredentials credentials, List<Event> events) {
		DateTime begin = task.getBegin();
		DateTime end = DateTime.now();
		for (int page = 1; page <= 50; ++page) {
			String path = String.format("%s/user/%s/%s.json", HOST, credentials.getScope(), resource);
			OAuthRequest request = new OAuthRequest(Verb.GET, path);
			request.addQuerystringParameter("start_time", Long.toString(begin.getMillis() / 1000 + 1));
			request.addQuerystringParameter("end_time", Long.toString(end.getMillis() / 1000));
			request.addQuerystringParameter("page_index", Integer.toString(page));
			request.addQuerystringParameter("locale", "user");
			request.addQuerystringParameter("sv", svs.get(resource));
			Response response = send(request, credentials);
			ObjectNode body = parseObject(response);
			IHealthResultSupport result = handler.process(task, body);
			Preconditions.checkState(result.isSuccess(), "Couldn't request <%s>: %s", request.getCompleteUrl(), body);
			if (!events.addAll(result.getEvents()) || !result.hasNext()) {
				break;
			}
		}
	}

	private Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		var command = new CompoundCommand(
				task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build());
		if (!Objects.equal(credentials.getToken(), expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
					.with(Credentials.CREDENTIALS)
					.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
					.build());
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
