package com.zenobase.tasks.google;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.Token;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.LocationMap;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.services.EventEditor;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class GoogleFitLocateTaskManager extends GoogleFitTaskManagerSupport<GoogleFitLocateTask> {

	private final EventRepository events;

	@Inject
	public GoogleFitLocateTaskManager(GoogleCredentialsManager credentialsManager, EventRepository events) {
		super(GoogleFitLocateTask.TYPE, credentialsManager, GoogleFitLocateTask.class);
		this.events = events;
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		return new GoogleFitLocateTask(bucketId, principal);
	}

	@Override
	protected Command execute(GoogleFitLocateTask task, final Map<String, DataStream> streams, final OAuthCredentials credentials, Token token) {

		EventEditor editor = new EventEditor(task.getBucketId(), task.getPrincipal(), events, task.getFrom()) {

			final LocationMap locations = new LocationMap();

			@Override
			protected Event edit(Event event) {
				Range<DateTime> t = getRange(event);
				if (!locations.contains(t)) {
					addLocations(locations, t, credentials, streams);
				}
				return locations.update(event);
			}
		};

		editor.run();

		return createCommand(task, editor.getLast(), credentials, editor.getEdits(), token);
	}

	private void addLocations(LocationMap locations, Range<DateTime> t, OAuthCredentials credentials, Map<String, DataStream> streams) {
		DataStream stream = new DataStream("derived:com.google.location.sample:com.google.android.gms:merge_location_samples", "com.google.location.sample", null);
		Location beginLocation = null;
		DateTime begin = null;
		for (DataPoint point : getDataPoints(t.lowerEndpoint().minusHours(1), t.upperEndpoint().plusHours(3), DateTimeZone.UTC, credentials, stream)) {
			Location location = new Location(point.getValue(0), point.getValue(1));
			if (begin != null) {
				locations.put(begin, point.getEnd(), beginLocation);
			}
			begin = point.getBegin();
			beginLocation = location;
		}
	}

	private Command createCommand(Task task, DateTime marker, OAuthCredentials credentials, List<Command> updates, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
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
