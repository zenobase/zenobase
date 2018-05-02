package com.zenobase.tasks.netatmo;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
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
		boolean includeModules = settings.path("modules").booleanValue();
		boolean hourly = settings.path("hourly").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new NetatmoTask(bucketId, principal, includeModules, hourly, marker);
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
		for (Device device : getDevices(credentials, task.includeModules())) {
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

	private Collection<Device> getDevices(OAuthCredentials credentials, boolean includeModules) {
		return new DevicesQuery(credentials).execute(includeModules).getDevices();
	}

	private List<Event> getEvents(NetatmoTask task, OAuthCredentials credentials, Device device, String to) {
		List<Event> events = Lists.newArrayList();
		MeasurementsQuery request = new MeasurementsQuery(task.getPrincipal(), credentials, device, task.isHourly());
		while (events.size() < 10000) {
			String from = null;
			if (!events.isEmpty()) {
				from = getMarker(events, task.isHourly());
			} else if (task.getMarker() != null) {
				from = task.getMarker();
			} else {
				from = formatMarker(device.getCreated());
			}
			if (!events.addAll(request.find(from, to).getEvents())) {
				break;
			}
		}
		if (events.size() >= 10000) {
			Logger.warn("Reached maximum number of measurements: {}", events.size());
		} else if (events.size() > 0 && task.isHourly()) {
			events.remove(events.size() - 1); // data for the last hour can still change
		}
		return events;
	}

	private class DevicesQuery {

		private final OAuthCredentials credentials;

		public DevicesQuery(OAuthCredentials credentials) {
			this.credentials = credentials;
		}

		public StationsResult execute(boolean includeModules) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.netatmo.com/api/getstationsdata");
			Response response = send(request, credentials);
			return new StationsResult(parseObject(response), includeModules);
		}
	}

	private class MeasurementsQuery {

		private final 	RateLimiter rate = RateLimiter.create(4);
		private final Identity principal;
		private final OAuthCredentials credentials;
		private final Device device;
		private final boolean hourly;

		public MeasurementsQuery(Identity principal, OAuthCredentials credentials, Device device, boolean hourly) {
			this.principal = principal;
			this.credentials = credentials;
			this.device = device;
			this.hourly = hourly;
		}

		public MeasurementsResult find(String from, String to) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.netatmo.com/api/getmeasure");
			request.addQuerystringParameter("device_id", device.getId());
			if (device.getModuleId() != null) {
				request.addQuerystringParameter("module_id", device.getModuleId());
			}
			if (from != null) {
				request.addQuerystringParameter("date_begin", from);
			}
			request.addQuerystringParameter("date_end", to);
			request.addQuerystringParameter("limit", "1000");
			request.addQuerystringParameter("scale", hourly ? "1hour" : "max");
			request.addQuerystringParameter("optimize", "false");
			request.addQuerystringParameter("type", "Temperature,Pressure,Noise,Humidity,CO2,GustStrength," + (hourly ? "sum_rain" : "Rain"));
			rate.acquire();
			Response response = send(request, credentials);
			return new MeasurementsResult(parseObject(response), principal, device, hourly);
		}
	}

	private Command createCommand(NetatmoTask task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran netatmo task", "reverted netatmo task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events, task.isHourly()))
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

	private static String getMarker(List<Event> events, boolean hourly) {
		DateTime last = Iterables.getLast(events).getValue(Event.TIMESTAMP);
		return formatMarker(hourly ? last.plusHours(1) : last.plusSeconds(1));
	}
}
