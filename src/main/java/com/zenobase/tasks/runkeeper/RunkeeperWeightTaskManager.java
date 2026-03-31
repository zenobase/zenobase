package com.zenobase.tasks.runkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
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

public class RunkeeperWeightTaskManager extends RunkeeperTaskManagerSupport {

	private static final Logger logger = LoggerFactory.getLogger(RunkeeperWeightTaskManager.class);

	public RunkeeperWeightTaskManager(RunkeeperCredentialsManager credentialsManager) {
		super(RunkeeperWeightTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		DateTimeZone zone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		Unit<Mass> unit = MoreObjects.firstNonNull(new UnitField<Mass>("unit").getValue(settings), Units.KG);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "Body");
		return new RunkeeperWeightTask(bucketId, principal, tag, unit, zone, Objects.requireNonNull(marker));
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RunkeeperWeightTask.class), credentials);
	}

	private Command execute(RunkeeperWeightTask task, OAuthCredentials credentials) {
		String path = "/weight";
		List<Event> events = new ArrayList<>();
		LocalDateTime from = parseMarker(task.getMarker());
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, host + path);
			request.addHeader("Accept", "application/vnd.com.runkeeper.WeightSetFeed+json");
			if (from != null) {
				request.addQuerystringParameter(
						"noEarlierThan", from.toLocalDate().toString());
			}
			request.addQuerystringParameter("pageSize", "100");
			try {
				Response response = send(request, credentials);
				RunkeeperWeightResult result = new RunkeeperWeightResult(
						parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
				for (Event event : result.getEvents()) {
					if (from == null
							|| Objects.requireNonNull(event.getValue(Event.TIMESTAMP))
									.toLocalDateTime()
									.isAfter(from)) {
						events.add(event);
					}
				}
				path = result.getNext();
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
					break;
				} else {
					throw e;
				}
			}
		}
		return createCommand(task, events);
	}
}
