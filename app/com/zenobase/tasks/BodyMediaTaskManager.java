package com.zenobase.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public class BodyMediaTaskManager extends OAuthTaskManager {

	private final String apiKey;

	@Inject
	public BodyMediaTaskManager(@Named("bodymedia.api.key") String apiKey, @Named("bodymedia.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new BodyMediaApi(apiKey), apiKey, apiSecret, callbackUrl);
		this.apiKey = apiKey;
	}

	@Override
	public String getType() {
		return BodyMediaTask.TYPE;
	}

	@Override
	public Command execute(Task task) {
		try {
			Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
			return execute(task.as(BodyMediaTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(BodyMediaTask task) {
		OAuthRequest request = createRequest(task);
		getService(task).signRequest(task.getToken(), request);
		Response response = request.send();
		checkResponse(task, request, response);
		BodyMediaSummaryResult result = new BodyMediaSummaryResult(parseObject(response), task.getPrincipal());
		return createCommand(task, result);
	}

	private OAuthRequest createRequest(BodyMediaTask task) {
		return new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/summary/day/%s/%s?api_key=%s", format(getFromDate(task)), format(new LocalDate()), apiKey));
	}

	private static LocalDate getFromDate(Task task) {
		return task.getMarker() != null ? LocalDate.parse(task.getMarker()) : LocalDate.now().minusMonths(1);
	}

	private static String format(LocalDate date) {
		return date.toString("yyyyMMdd");
	}

	private static Command createCommand(BodyMediaTask task, BodyMediaSummaryResult result) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran bodymedia task", "reverted bodymedia task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), result.getLastSyncDate().toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : result.getEvents()) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
