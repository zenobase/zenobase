package com.zenobase.tasks.bodymedia;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class BodyMediaStepsTaskManager extends BodyMediaTaskManagerSupport {

	@Inject
	public BodyMediaStepsTaskManager(BodyMediaCredentialsManager credentialsManager) {
		super(BodyMediaStepsTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		return new BodyMediaStepsTask(bucketId, principal, marker, tag);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(BodyMediaStepsTask.class), credentials);
	}

	private Command execute(BodyMediaStepsTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		TimezoneMap timezones = getTimezoneMap(credentials);
		LocalDate date = getLast(parseMarker(task.getMarker()), timezones.getBegin());
		int count = 0;
		while (date.isBefore(LocalDate.now().plusDays(1))) {
			try {
				BodyMediaStepsResult result = execute(task, credentials, date, timezones);
				if (!date.isBefore(result.getLastSyncDate().toLocalDate())) {
					break;
				}
				events.addAll(result.getEvents());
				date = date.plusDays(1);
				++count;
			} catch (InvalidStatusException e) {
				Logger.warn("Couldn't complete task: " + task.getId() + " (but got" + events.size() + " events after " + count + " requests)", e);
				break;
			}
		}
		return createCommand(task, credentials, date, events, token);
	}

	private LocalDate getLast(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? a : b;
	}

	private BodyMediaStepsResult execute(BodyMediaStepsTask task, OAuthCredentials credentials, LocalDate date, TimezoneMap timezones) {
		checkRateLimit();
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/step/day/hour/%s", formatMarker(date)));
		Response response = send(request, credentials);
		return new BodyMediaStepsResult(parseObject(response), task.getPrincipal(), timezones);
	}
}
