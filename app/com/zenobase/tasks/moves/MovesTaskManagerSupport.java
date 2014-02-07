package com.zenobase.tasks.moves;

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.Token;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.RateLimiter;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

abstract class MovesTaskManagerSupport extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(1);

	protected MovesTaskManagerSupport(String type, MovesCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	protected void removeDuplicates(List<Event> events) {
		DateTime t0 = null;
		for (Iterator<Event> i = events.iterator(); i.hasNext();) {
			Event event = i.next();
			DateTime t1 = event.getValue(Event.TIMESTAMP);
			if (t0 == null || !t0.equals(t1)) {
				t0 = t1;
			} else {
				i.remove();
			}
		}
	}

	protected void removeLast(List<Event> events) {
		if (!events.isEmpty()) {
			events.remove(events.size() - 1);
		}
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
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
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	protected static String getMarker(List<Event> events) {
		Event latest = Iterables.getLast(events);
		return latest.getValue(Event.TIMESTAMP).plus(latest.getValue(Event.DURATION)).toString();
	}

	protected static void checkRateLimit() {
		RATE_LIMITER.acquire();
	}

	protected static LocalDate min(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? b : a;
	}
}
