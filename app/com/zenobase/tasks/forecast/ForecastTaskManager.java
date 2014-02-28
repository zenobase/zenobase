package com.zenobase.tasks.forecast;

import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Facet;
import com.zenobase.search.ListFacet;
import com.zenobase.search.OffsetDateTimeRangeConstraintBuilder;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;

public class ForecastTaskManager extends TaskManager {

	private final EventRepository events;
	private final Forecaster forecaster;

	@Inject
	public ForecastTaskManager(EventRepository events, Forecaster forecaster) {
		super(ForecastTask.TYPE);
		this.events = events;
		this.forecaster = forecaster;
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		return new ForecastTask(bucketId, principal, settings.path("si").booleanValue());
	}

	@Override
	public Command execute(Task task) {
		return execute(task.as(ForecastTask.class));
	}

	private Command execute(ForecastTask task) {
		List<Command> updates = Lists.newArrayList();
		ObjectField objects = new ObjectField("events");
		String field = "timestamp";
		Facet list = new ListFacet(objects.getName(), 0, 1000, field, SortOrder.ASC);
		DateTime from = task.getFrom();
		QueryBuilder query = new OffsetDateTimeRangeConstraintBuilder(field).build(Range.<ReadableInstant>greaterThan(from));
		ObjectNode result = events.find(task.getBucketId(), new EventSearchBuilder().addConstraint(query, false).addFacet(list).buildSearch());
		DateTime marker = null;
		for (ObjectNode node : objects.getValues(result)) {
			Event event = new Event(node);
			DateTime timestamp = event.getValue(Event.TIMESTAMP);
			Event updated = update(event, timestamp, task.useStandardUnits());
			if (marker == null || marker.isBefore(timestamp)) {
				marker = timestamp;
			}
			if (updated != null) {
				updates.add(new UpdateEventCommand(task.getPrincipal(), task.getBucketId(), event, updated));
			}
		}
		return createCommand(task, marker, updates);
	}

	private Event update(Event event, DateTime timestamp, boolean standardUnits) {
		Location location = event.getValue(Event.LOCATION);
		if (location == null) {
			return null;
		}
		Forecast forecast = forecaster.find(location, timestamp, standardUnits);
		Event updated = event.copy();
		forecast.apply(updated);
		// Logger.info("< " + event);
		// Logger.info("> " + updated);
		return updated;
	}

	private Command createCommand(Task task, DateTime marker, List<Command> updates) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran forecast task", "reverted forecast task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker != null ? marker.toString() : null)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Command update : updates) {
			// System.out.println("[event] " + event);
			command.add(update);
		}
		return command;
	}
}
