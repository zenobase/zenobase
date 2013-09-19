package com.zenobase.tasks.bodymedia;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.RangeMap;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidTokenException;
import com.zenobase.tasks.OAuthTask;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class BodyMediaBurnTaskManager extends OAuthTaskManager {

	private final String apiKey;

	@Inject
	public BodyMediaBurnTaskManager(@Named("bodymedia.api.key") String apiKey, @Named("bodymedia.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new BodyMediaApi(apiKey), apiKey, apiSecret, callbackUrl);
		this.apiKey = apiKey;
	}

	@Override
	public String getType() {
		return BodyMediaBurnTask.TYPE;
	}

	@Override
	public OAuthTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		OAuthTask task = super.newTask(bucketId, principal, settings);
		task.setMarker(parseMarker(settings.path("marker").getTextValue()).toString());
		return task;
	}

	@Override
	public Command execute(Task task) {
		try {
			Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
			return execute(task.as(BodyMediaBurnTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(BodyMediaBurnTask task) {
		if (task.isExpired()) {
			Logger.info("Refreshing token...");
			task.setToken(getAccessToken(task, ""));
		}
		List<Event> events = Lists.newArrayList();
		RangeMap<LocalDateTime, DateTimeZone> timezones = getTimezones(task);
		LocalDate date = parseMarker(task.getMarker());
		while (true) {
			BodyMediaBurnResult result = execute(task, date, timezones);
			if (!events.addAll(result.getEvents())) {
				break;
			}
			date = date.plusDays(1);
		}
		return createCommand(task, date, events);
	}

	private RangeMap<LocalDateTime, DateTimeZone> getTimezones(BodyMediaBurnTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/timezone?api_key=%s", apiKey));
		getService(task).signRequest(task.getToken(), request);
		Response response = request.send();
		checkResponse(task, request, response);
		return new BodyMediaTimezonesResult(parseObject(response)).getTimezones();
	}

	private BodyMediaBurnResult execute(BodyMediaBurnTask task, LocalDate date, RangeMap<LocalDateTime, DateTimeZone> timezones) {
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/burn/day/minute/%s?api_key=%s", formatMarker(date), apiKey));
		getService(task).signRequest(task.getToken(), request);
		Response response = request.send();
		checkResponse(task, request, response);
		return new BodyMediaBurnResult(parseObject(response), task.getPrincipal(), timezones);
	}

	private static LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	private static String formatMarker(LocalDate date) {
		return date.toString("yyyyMMdd");
	}

	private static Command createCommand(BodyMediaBurnTask task, LocalDate lastSync, Iterable<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran bodymedia task", "reverted bodymedia task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), lastSync.toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
