package com.zenobase.tasks.beeminder;

import java.math.BigDecimal;

import javax.inject.Inject;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Range;
import org.elasticsearch.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.ListFacet;
import com.zenobase.search.LocalTimelineFacet;
import com.zenobase.search.OffsetDateTimeRangeConstraintBuilder;
import com.zenobase.search.SearchBuilderSupport;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class BeeminderTaskManager extends OAuthTaskManager {

	private static final String BASE = "https://www.beeminder.com/api/v1";
	private static final ObjectField FIELD_STATS = new ObjectField("stats");
	private static final ObjectField FIELD_LATEST = new ObjectField("latest");

	private final EventRepository events;

	@Inject
	public BeeminderTaskManager(BeeminderCredentialsManager credentialsManager, EventRepository events) {
		super(BeeminderTask.TYPE, credentialsManager);
		this.events = events;
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String goal = settings.path("goal").textValue();
		String filter = settings.path("filter").textValue();
		String keyField = Objects.firstNonNull(settings.path("key_field").textValue(), Event.TIMESTAMP.getName());
		String field = settings.path("field").textValue();
		String unit = settings.path("unit").textValue();
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new BeeminderTask(bucketId, principal, goal, filter, keyField, field, unit, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(BeeminderTask.class), credentials);
	}

	private Command execute(BeeminderTask task, OAuthCredentials credentials) {
		UserResult user = getUser(credentials);
		if (!user.hasGoal(task.getGoal())) {
			Logger.warn("Can't run task {} because goal does not exist: {}", task.getId(), task.getGoal());
			return null;
		}
		ObjectNode result = find(task.getBucketId(), task.getKeyField(), task.getField(), task.getUnit(), task.getFrom(), user.getTimezone(), task.getFilter());
		DateTime to = getLatest(result);
		ArrayNode datapoints = getDatapoints(result, task.getField() != null, Event.DURATION.getName().equals(task.getField()), user.getTimezone());
		if (to != null && datapoints.size() > 0) {
			send(datapoints, task.getGoal(), credentials);
			return createCommand(task, to, credentials);
		}
		return null;
	}

	private UserResult getUser(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("%s/users/me.json", BASE));
		Response response = send(request, credentials);
		return new UserResult(parseObject(response));
	}

	private ObjectNode find(String bucketId, String keyField, String field, Unit<?> unit, DateTime from, DateTimeZone zone, String filter) {
		events.refresh(bucketId);
		SearchBuilderSupport search = new EventSearchBuilder()
			.addFacet(new ListFacet(FIELD_LATEST.getName(), 0, 1, '-' + Event.TIMESTAMP.getName(), null, Event.SCHEMA))
			.addFacet(new LocalTimelineFacet(FIELD_STATS.getName(), keyField, field, "day", null, unit, null))
			.addConstraint(new OffsetDateTimeRangeConstraintBuilder(Event.TIMESTAMP.getName()).build(Range.greaterThan(from)), false);
		if (filter != null) {
			search.addConstraints(filter);
		}
		return events.find(bucketId, search.buildSearch());
	}

	private static DateTime getLatest(ObjectNode result) {
		for (ObjectNode node : FIELD_LATEST.getValues(result)) {
			return Ordering.natural().max(new Event(node).getValues(Event.TIMESTAMP));
		}
		return null;
	}

	private static ArrayNode getDatapoints(ObjectNode result, boolean useSum, boolean asDuration, DateTimeZone zone) {
		ArrayNode datapoints = Nodes.newArray();
		for (ObjectNode node : FIELD_STATS.getValues(result)) {
			DateTime time = LocalDate.parse(node.path("label").textValue()).toDateTime(new LocalTime(12, 0), zone);
			BigDecimal value = getValue(node, useSum);
			datapoints.add(new Datapoint(time, value).toJson(asDuration));
		}
		return datapoints;
	}

	private static BigDecimal getValue(ObjectNode node, boolean useSum) {
		JsonNode valueNode = null;
		if (useSum) {
			valueNode = node.path("sum");
			if (valueNode.has("@value")) {
				valueNode = valueNode.path("@value");
			}
		} else {
			valueNode = node.path("count");
		}
		return valueNode.decimalValue();
	}

	private void send(ArrayNode datapoints, String goal, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.POST, String.format("%s/users/me/goals/%s/datapoints/create_all.json", BASE, goal));
		request.addBodyParameter("datapoints", datapoints.toString());
		send(request, credentials);
	}

	private Command createCommand(Task task, DateTime marker, OAuthCredentials credentials) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker != null ? marker.toString() : null)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		return command;
	}
}
