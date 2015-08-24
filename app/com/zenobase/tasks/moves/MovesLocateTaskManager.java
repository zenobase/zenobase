package com.zenobase.tasks.moves;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.LocationMap;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventEditor;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MovesLocateTaskManager extends MovesTaskManagerSupport {

	private final EventRepository events;

	@Inject
	public MovesLocateTaskManager(MovesCredentialsManager credentialsManager, EventRepository events) {
		super(MovesLocateTask.TYPE, credentialsManager);
		this.events = events;
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		return new MovesLocateTask(bucketId, principal);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MovesLocateTask.class), credentials);
	}

	private Command execute(MovesLocateTask task, final OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime from = task.getFrom();
		if (from == null) {
			MovesProfileResult profile = getProfile(credentials);
			from = profile.getFirstDate();
		}
		EventEditor editor = new LocationEditor(task.getBucketId(), task.getPrincipal(), from, credentials);
		editor.run();
		return createCommand(task, editor.getLast(), credentials, editor.getEdits(), token);
	}

	private class LocationEditor extends EventEditor {

		private final LocationMap locations = new LocationMap();
		private final OAuthCredentials credentials;

		public LocationEditor(String bucketId, Identity principal, DateTime last, OAuthCredentials credentials) {
			super(bucketId, principal, events, last);
			this.credentials = credentials;
		}

		@Override
		protected Event edit(Event event) {
			DateTime time = Ordering.natural().min(event.getValues(Event.TIMESTAMP));
			if (!locations.contains(time)) {
				LocalDate today = DateTime.now(time.getZone()).toLocalDate();
				if (time.toLocalDate().isAfter(today)) {
					return null;
				}
				find(time.toLocalDate(), today).update(locations);
			}
			locations.remove(time);
			Event updated = locations.update(event);
			if (updated != null) {
				updated.addValue(Event.SOURCE, MovesActivitiesResult.SOURCE);
			}
			return updated;
		}

		private MovesStorylineResult find(LocalDate from, LocalDate today) {
			OAuthRequest request = newRequest("/user/storyline/daily");
			request.addQuerystringParameter("trackPoints", "true");
			request.addQuerystringParameter("from", from.toString());
			request.addQuerystringParameter("to", Ordering.natural().min(today, from.plusDays(6)).toString());
			Response response = send(request, credentials);
			return new MovesStorylineResult(parseArray(response));
		}
	}

	private Command createCommand(Task task, DateTime marker, OAuthCredentials credentials, List<Command> updates, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran moves-locate task", "reverted moves-locate task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker != null ? marker.toString() : null)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!Objects.equal(credentials.getToken(), expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
				.build());
		}
		for (Command update : updates) {
			// System.out.println("[event] " + event);
			command.add(update);
		}
		return command;
	}
}
