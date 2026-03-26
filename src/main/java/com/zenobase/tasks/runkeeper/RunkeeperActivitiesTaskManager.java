package com.zenobase.tasks.runkeeper;

import java.util.ArrayList;
import java.util.List;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import jakarta.inject.Inject;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class RunkeeperActivitiesTaskManager extends RunkeeperTaskManagerSupport {

	private static final Logger logger = LoggerFactory.getLogger(RunkeeperActivitiesTaskManager.class);

	@Inject
	public RunkeeperActivitiesTaskManager(RunkeeperCredentialsManager credentialsManager) {
		super(RunkeeperActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		DateTimeZone zone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		Unit<Length> lengthUnit = MoreObjects.firstNonNull(new UnitField<Length>("unit").getValue(settings), Units.KM);
		return new RunkeeperActivitiesTask(bucketId, principal, zone, lengthUnit, Units.KCAL, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RunkeeperActivitiesTask.class), credentials);
	}

	private Command execute(RunkeeperActivitiesTask task, OAuthCredentials credentials) {
		String path = "/fitnessActivities";
		List<Event> events = new ArrayList<>();
		LocalDateTime from = parseMarker(task.getMarker());
		try {
			while (path != null) {
				var request = new OAuthRequest(Verb.GET, host + path);
				request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivityFeed+json");
				if (from != null) {
					request.addQuerystringParameter(
							"noEarlierThan", from.toLocalDate().toString());
				}
				request.addQuerystringParameter("pageSize", "100");
				Response response = send(request, credentials);
				RunkeeperActivitiesResult result = new RunkeeperActivitiesResult(
						parseObject(response),
						task.getPrincipal(),
						task.getDistanceUnit(),
						task.getEnergyUnit(),
						task.getTimezone());
				for (Event event : result.getEvents()) {
					if (from == null
							|| event.getValue(Event.TIMESTAMP).toLocalDateTime().isAfter(from)) {
						events.add(event);
					}
				}
				path = result.getNext();
			}
			for (Event event : events) {
				addDetails(event, task.getHeightUnit(), credentials);
			}
		} catch (InvalidStatusException e) {
			if (e.getStatus() == 429) { // reached rate limit
				logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
			} else {
				throw e;
			}
		}
		return createCommand(task, events);
	}

	private void addDetails(Event event, Unit<Length> heightUnit, OAuthCredentials credentials) {
		var request =
				new OAuthRequest(Verb.GET, host + event.getValue(Event.SOURCE).url());
		request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivity+json");
		Response response = send(request, credentials);
		new RunkeeperActivityResult(parseObject(response), heightUnit).addDetails(event);
	}
}
