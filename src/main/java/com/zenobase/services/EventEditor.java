package com.zenobase.services;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.ListFacet;
import com.zenobase.search.OffsetDateTimeRangeConstraintBuilder;

public abstract class EventEditor {

	private static final ObjectField FIELD = new ObjectField("events");
	private static final int LIMIT = 5000;

	private final String bucketId;
	private final Identity principal;
	private final EventRepository events;
	private @Nullable DateTime last;
	private final List<Command> edits = new ArrayList<>();

	public EventEditor(String bucketId, Identity principal, EventRepository events, DateTime last) {
		this.bucketId = bucketId;
		this.principal = principal;
		this.events = events;
		this.last = last;
	}

	public void run() {
		for (ObjectNode node : FIELD.getValues(find())) {
			Event event = new Event(node);
			DateTime time = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			Event edited = edit(event);
			if (last == null || last.isBefore(time)) {
				last = time;
			}
			if (edited != null) {
				edits.add(new UpdateEventCommand(principal, bucketId, event, edited));
			}
		}
	}

	private ObjectNode find() {
		events.refresh(bucketId);
		var search = new EventSearchBuilder()
				.addFacet(new ListFacet(FIELD.getName(), 0, LIMIT, Event.TIMESTAMP.getName(), null, Event.SCHEMA));
		if (last != null) {
			search.addConstraint(
					new OffsetDateTimeRangeConstraintBuilder(Event.TIMESTAMP.getName()).build(Range.greaterThan(last)),
					false);
		}
		return events.find(bucketId, search.buildSearch());
	}

	protected abstract @Nullable Event edit(Event event);

	public @Nullable DateTime getLast() {
		return last;
	}

	public List<Command> getEdits() {
		return edits;
	}
}
