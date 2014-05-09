package com.zenobase.tasks.netatmo;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class NetatmoTaskManager extends OAuthTaskManager {

	@Inject
	public NetatmoTaskManager(NetatmoCredentialsManager credentialsManager) {
		super(NetatmoTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new NetatmoTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(NetatmoTask.class), credentials);
	}

	private Command execute(NetatmoTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		String to = formatMarker(new DateTime(DateTimeZone.UTC).minusMinutes(1));
		for (Device device : getDevices(credentials)) {
			events.addAll(getEvents(task, credentials, device, to));
		}
		return createCommand(task, credentials, events, token);
	}

	static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static String formatMarker(DateTime time) {
		return time != null ? Long.toString(time.getMillis() / 1000) : null;
	}

	private List<Device> getDevices(OAuthCredentials credentials) {
		return new DevicesQuery(credentials).execute().getDevices();
	}

	private List<Event> getEvents(NetatmoTask task, OAuthCredentials credentials, Device device, String to) {
		List<Event> events = Lists.newArrayList();
		MeasurementsQuery request = new MeasurementsQuery(task.getPrincipal(), credentials, device);
		while (events.size() < 30000) {
			String from = null;
			if (!events.isEmpty()) {
				from = getMarker(events);
			} else if (task.getMarker() != null) {
				from = task.getMarker();
			} else {
				from = formatMarker(device.getCreated());
			}
			if (!events.addAll(request.find(from, to).getEvents())) {
				break;
			}
		}
		if (events.size() >= 30000) {
			Logger.warn("Reached maximum number of measurements");
		}
		return events;
	}

	private class DevicesQuery {

		private final OAuthCredentials credentials;

		public DevicesQuery(OAuthCredentials credentials) {
			this.credentials = credentials;
		}

		public DevicesResult execute() {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.netatmo.net/api/devicelist");
			Response response = send(request, credentials);
			return new DevicesResult(parseObject(response));
		}
	}

	private class MeasurementsQuery {

		private final Identity principal;
		private final OAuthCredentials credentials;
		private final Device device;

		public MeasurementsQuery(Identity principal, OAuthCredentials credentials, Device device) {
			this.principal = principal;
			this.credentials = credentials;
			this.device = device;
		}

		public MeasurementsResult find(String from, String to) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.netatmo.net/api/getmeasure");
			request.addQuerystringParameter("device_id", device.getId());
			if (from != null) {
				request.addQuerystringParameter("date_begin", from);
			}
			request.addQuerystringParameter("date_end", to);
			request.addQuerystringParameter("limit", "1000");
			request.addQuerystringParameter("scale", "max");
			request.addQuerystringParameter("optimize", "false");
			request.addQuerystringParameter("type", "Temperature,Pressure,Noise,Humidity,CO2");
			Response response = send(request, credentials);
			return new MeasurementsResult(principal, device, parseObject(response));
		}
	}

	private Command createCommand(NetatmoTask task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran netatmo task", "reverted netatmo task");
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

	private static String getMarker(List<Event> events) {
		return formatMarker(Iterables.getLast(events).getValue(Event.TIMESTAMP).plusSeconds(1));
	}
}
