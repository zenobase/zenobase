package com.zenobase.tasks.runkeeper;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class RunkeeperWeightTaskManager extends RunkeeperTaskManagerSupport {

	@Inject
	public RunkeeperWeightTaskManager(RunkeeperCredentialsManager credentialsManager) {
		super(RunkeeperWeightTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		Unit<Mass> unit = Objects.firstNonNull(new UnitField<Mass>("unit").getValue(settings), Units.KG);
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "Body");
		return new RunkeeperWeightTask(bucketId, principal, tag, unit, zone, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RunkeeperWeightTask.class), credentials);
	}

	private Command execute(RunkeeperWeightTask task, OAuthCredentials credentials) {
		String path = "/weight";
		List<Event> events = Lists.newArrayList();
		LocalDateTime from = parseMarker(task.getMarker());
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, host + path);
			request.addHeader("Accept", "application/vnd.com.runkeeper.WeightSetFeed+json");
			if (from != null) {
				request.addQuerystringParameter("noEarlierThan", from.toLocalDate().toString());
			}
			request.addQuerystringParameter("pageSize", "100");
			Response response = send(request, credentials);
			RunkeeperWeightResult result = new RunkeeperWeightResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
			for (Event event : result.getEvents()) {
				if (from == null || event.getValue(Event.TIMESTAMP).toLocalDateTime().isAfter(from)) {
					events.add(event);
				}
			}
			path = result.getNext();
		}
		return createCommand(task, credentials, events);
	}
}
