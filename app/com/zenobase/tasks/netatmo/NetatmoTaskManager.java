package com.zenobase.tasks.netatmo;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthConstants;
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
import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.tasks.InvalidTokenException;
import com.zenobase.tasks.OAuthTask;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class NetatmoTaskManager extends OAuthTaskManager {

	@Inject
	public NetatmoTaskManager(@Named("netatmo.api.key") String apiKey, @Named("netatmo.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new NetatmoApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return NetatmoTask.TYPE;
	}

	@Override
	public OAuthTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		OAuthTask task = super.newTask(bucketId, principal, settings);
		task.setMarker(formatMarker(parseMarker(settings.path("marker").textValue())));
		return task;
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
		String code = config.get("code").textValue();
		if (code == null) {
			Logger.warn(String.format("Couldn't authorize %s task <%s>: %s",
				task.getType(), task.getId(), config));
			return null;
		}
		ExpiringToken token = (ExpiringToken) getAccessToken(task, code);
		return UpdateTaskCommand.builder(task)
			.set(Task.AUTHORIZATION_URL, task.getAuthorizationUrl(), null)
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, task.getToken(), token)
			.build();
	}

	@Override
	public void reauthorize(Task task) {
		reauthorize(task.as(OAuthTask.class));
	}

	private void reauthorize(OAuthTask task) {
		OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.netatmo.net/oauth2/token");
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter("refresh_token", ((ExpiringToken) task.getToken()).getRefreshToken());
		request.addBodyParameter(OAuthConstants.CLIENT_ID, apiKey);
		request.addBodyParameter(OAuthConstants.CLIENT_SECRET, apiSecret);
		Response response = send(request);
		task.setToken(new OAuth2TokenExtractor().extract(response.getBody()));
	}

	@Override
	public Command execute(Task task) {
		try {
			Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
			return execute(task.as(NetatmoTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(NetatmoTask task) {
		Token token = task.getToken();
		if (((ExpiringToken) task.getToken()).isExpired()) {
			reauthorize(task);
		}
		List<Event> events = Lists.newArrayList();
		String to = formatMarker(new DateTime(DateTimeZone.UTC).minusMinutes(1));
		for (Device device : getDevices(task)) {
			events.addAll(getEvents(task, device, to));
		}
		return createCommand(task, events, token);
	}

	static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static String formatMarker(DateTime time) {
		return time != null ? Long.toString(time.getMillis() / 1000) : null;
	}

	private List<Device> getDevices(NetatmoTask task) {
		return new DevicesQuery(task).execute().getDevices();
	}

	private List<Event> getEvents(NetatmoTask task, Device device, String to) {
		List<Event> events = Lists.newArrayList();
		MeasurementsQuery request = new MeasurementsQuery(task, device);
		while (true) {
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
		return events;
	}

	private class DevicesQuery {

		private final OAuthTask task;

		public DevicesQuery(OAuthTask task) {
			this.task = task;
		}

		public DevicesResult execute() {
			OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.netatmo.net/api/devicelist");
			request.addQuerystringParameter("access_token", task.getToken().getToken());
			Response response = send(request);
			checkResponse(task, request, response);
			return new DevicesResult(parseObject(response));
		}
	}

	private class MeasurementsQuery {

		private final OAuthTask task;
		private final Device device;

		public MeasurementsQuery(OAuthTask task, Device device) {
			this.task = task;
			this.device = device;
		}

		public MeasurementsResult find(String from, String to) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.netatmo.net/api/getmeasure");
			request.addQuerystringParameter("access_token", task.getToken().getToken());
			request.addQuerystringParameter("device_id", device.getId());
			if (from != null) {
				request.addQuerystringParameter("date_begin", from);
			}
			request.addQuerystringParameter("date_end", to);
			request.addQuerystringParameter("limit", "1000");
			request.addQuerystringParameter("scale", "max");
			request.addQuerystringParameter("optimize", "false");
			request.addQuerystringParameter("type", "Temperature,Pressure,Noise,Humidity,CO2");
			Response response = send(request);
			checkResponse(task, request, response);
			return new MeasurementsResult(task.getPrincipal(), device, parseObject(response));
		}
	}

	private Command createCommand(NetatmoTask task, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran netatmo task", "reverted netatmo task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, expiredToken, task.getToken())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static String getMarker(List<Event> events) {
		return formatMarker(Iterables.getLast(events).getValue(Event.TIMESTAMP).plusSeconds(1));
	}

	Response send(OAuthRequest request) {
		return request.send();
	}
}
