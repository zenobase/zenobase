package com.zenobase.tasks.netatmo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.RateLimiter;
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
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetatmoTaskManager extends OAuthTaskManager {

	private static final Logger logger = LoggerFactory.getLogger(NetatmoTaskManager.class);

	private static final long WINDOW_SEC_MAX = Duration.ofDays(7).toSeconds();
	private static final long WINDOW_SEC_HOURLY = Duration.ofDays(60).toSeconds();
	private static final long MAX_BATCH_BYTES = 1_000_000;

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
		List<Event> events = new ArrayList<>();
		String to = Objects.requireNonNull(formatMarker(DateTime.now(DateTimeZone.UTC).minusMinutes(1)));
		for (Device device : getDevices(credentials, task.includeModules())) {
			events.addAll(getEvents(task, credentials, device, to));
		}
		return createCommand(task, credentials, events, Objects.requireNonNull(token));
	}

	static @Nullable DateTime parseMarker(@Nullable String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static @Nullable String formatMarker(@Nullable DateTime time) {
		return time != null ? Long.toString(time.getMillis() / 1000) : null;
	}

	private Collection<Device> getDevices(OAuthCredentials credentials, boolean includeModules) {
		return new DevicesQuery(credentials).execute(includeModules).getDevices();
	}

	private List<Event> getEvents(NetatmoTask task, OAuthCredentials credentials, Device device, String to) {
		String startMarker = task.getMarker() != null ? task.getMarker() : formatMarker(device.getCreated());
		if (startMarker == null) {
			return List.of();
		}
		MeasurementsQuery request = new MeasurementsQuery(task.getPrincipal(), credentials, device, task.isHourly());
		long fromSec = Long.parseLong(startMarker);
		long toSec = Long.parseLong(to);
		long windowSec = task.isHourly() ? WINDOW_SEC_HOURLY : WINDOW_SEC_MAX;
		List<Event> events = new ArrayList<>();
		long byteCount = 0;
		while (byteCount < MAX_BATCH_BYTES && fromSec < toSec) {
			long windowToSec = Math.min(toSec, fromSec + windowSec);
			List<Event> page = request.find(Long.toString(fromSec), Long.toString(windowToSec)).getEvents();
			events.addAll(page);
			byteCount += totalJsonSize(page);
			fromSec =
				page.size() < 1000
					? windowToSec
					: Long.parseLong(Objects.requireNonNull(getMarker(events, task.isHourly())));
		}
		if (byteCount >= MAX_BATCH_BYTES) {
			logger.warn("Reached maximum batch size: {} events, {} bytes", events.size(), byteCount);
		} else if (!events.isEmpty() && task.isHourly()) {
			events.removeLast(); // data for the last hour can still change
		}
		return events;
	}

	private static long totalJsonSize(List<Event> events) {
		long total = 0;
		for (Event event : events) {
			total += event.toJson().toString().length();
		}
		return total;
	}

	private class DevicesQuery {

		private final OAuthCredentials credentials;

		public DevicesQuery(OAuthCredentials credentials) {
			this.credentials = credentials;
		}

		public StationsResult execute(boolean includeModules) {
			var request = new OAuthRequest(Verb.GET, "https://api.netatmo.com/api/getstationsdata");
			Response response = send(request, credentials);
			return new StationsResult(parseObject(response), includeModules);
		}
	}

	private class MeasurementsQuery {

		private final RateLimiter rate = RateLimiter.create(4);
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

		public MeasurementsResult find(@Nullable String from, String to) {
			var request = new OAuthRequest(Verb.GET, "https://api.netatmo.com/api/getmeasure");
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
			request.addQuerystringParameter(
				"type",
				"Temperature,Pressure,Noise,Humidity,CO2,GustStrength," + (hourly ? "sum_rain" : "Rain")
			);
			rate.acquire();
			Response response = send(request, credentials);
			return new MeasurementsResult(parseObject(response), principal, device, hourly);
		}
	}

	private Command createCommand(
		NetatmoTask task,
		OAuthCredentials credentials,
		List<Event> events,
		Token expiredToken
	) {
		var command = new CompoundCommand(task.getPrincipal(), "ran netatmo task", "reverted netatmo task");
		command.add(
			UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(
					Task.MARKER,
					task.getMarker(),
					events.isEmpty() ? task.getMarker() : getMarker(events, task.isHourly())
				)
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build()
		);
		if (!Objects.equals(credentials.getToken(), expiredToken)) {
			command.add(
				UpdateCredentialsCommand.builder(credentials)
					.with(Credentials.CREDENTIALS)
					.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
					.build()
			);
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static @Nullable String getMarker(List<Event> events, boolean hourly) {
		DateTime last = Objects.requireNonNull(
			Objects.requireNonNull(Iterables.getLast(events)).getValue(Event.TIMESTAMP)
		);
		return formatMarker(hourly ? last.plusHours(1) : last.plusSeconds(1));
	}
}
