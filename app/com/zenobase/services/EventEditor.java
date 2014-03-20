package com.zenobase.services;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Facet;
import com.zenobase.search.ListFacet;
import com.zenobase.search.OffsetDateTimeRangeConstraintBuilder;

public abstract class EventEditor {

	private static final ObjectField FIELD = new ObjectField("events");
	private static final int LIMIT = 5000;

	private final String bucketId;
	private final Identity principal;
	private final EventRepository events;
	private DateTime last;
	private List<Command> edits = Lists.newArrayList();

	public EventEditor(String bucketId, Identity principal, EventRepository events, DateTime last) {
		this.bucketId = bucketId;
		this.principal = principal;
		this.events = events;
		this.last = last;
	}

	public void run() {
		for (ObjectNode node : FIELD.getValues(find())) {
			Event event = new Event(node);
			DateTime time = event.getValue(Event.TIMESTAMP);
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
		Facet facet = new ListFacet(FIELD.getName(), 0, LIMIT, Event.TIMESTAMP.getName(), SortOrder.ASC);
		QueryBuilder query = new OffsetDateTimeRangeConstraintBuilder(Event.TIMESTAMP.getName()).build(Range.<ReadableInstant>greaterThan(last));
		return events.find(bucketId, new EventSearchBuilder().addConstraint(query, false).addFacet(facet).buildSearch());
	}

	protected abstract Event edit(Event event);

	public DateTime getLast() {
		return last;
	}

	public List<Command> getEdits() {
		return edits;
	}
}
