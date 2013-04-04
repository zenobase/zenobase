package com.zenobase.tasks.netatmo;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
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
		String code = config.get("code").getTextValue();
		if (code == null) {
			Logger.warn(String.format("Couldn't authorize %s task <%s>: %s",
				task.getType(), task.getId(), config));
			return null;
		}
		Token token = getAccessToken(task, code);
		return UpdateTaskCommand.builder(task)
			.set(Task.AUTHORIZATION_URL, task.getAuthorizationUrl(), null)
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, task.getToken(), token)
			.build();
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
		String marker = formatMarker(new DateTime(DateTimeZone.UTC).minusMinutes(1));
		List<Event> events = Lists.newArrayList();
		for (Device device : getDevices(task)) {
			events.addAll(getEvents(task, device));
		}
		return createCommand(task, marker, events);
	}

	static String formatMarker(DateTime time) {
		return Long.toString(time.getMillis());
	}

	private List<Device> getDevices(NetatmoTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.netatmo.net/api/devicelist");
		request.addQuerystringParameter("access_token", task.getToken().getToken());
		Response response = send(request);
		checkResponse(task, request, response);
		return new NetatmoDeviceListResult(parseObject(response)).getDevices();
	}

	private List<Event> getEvents(NetatmoTask task, Device device) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.netatmo.net/api/getmeasure");
		request.addQuerystringParameter("access_token", task.getToken().getToken());
		request.addQuerystringParameter("device_id", device.getId());
		// request.addQuerystringParameter("module_id", );
		request.addQuerystringParameter("scale", "max");
		request.addQuerystringParameter("type", "Temperature,Pressure,Noise,Humidity,CO2");
		//request.addQuerystringParameter("date_begin", "");
		//request.addQuerystringParameter("date_end", "");
		request.addQuerystringParameter("optimize", "false");
		Response response = send(request);
		checkResponse(task, request, response);
		return new NetatmoResult(task.getPrincipal(), device, parseObject(response)).getEvents();
	}

	protected Response send(OAuthRequest request) {
		return request.send();
	}

	private Command createCommand(NetatmoTask task, String marker, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran netatmo task", "reverted netatmo task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			//System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
