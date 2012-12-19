package com.zenobase.controllers;

import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.search.sort.SortOrder;
import com.google.common.base.Preconditions;

import com.zenobase.json.JsonChunks;
import com.zenobase.json.JsonStream;
import com.zenobase.models.Event;
import com.zenobase.search.EventSearch;
import com.zenobase.search.ListWidget;
import com.zenobase.services.EventRepository;

final class EventChunks extends JsonChunks {

	private static final int LIMIT = 100;

	private final EventRepository events;
	private final String bucketId;
	private final String[] filters;

	public EventChunks(EventRepository events, String bucketId, String[] filters) {
		this.events = events;
		this.bucketId = bucketId;
		this.filters = filters;
	}

	@Override
	public void onReady(JsonStream out) throws IOException {
		out.writeArrayFieldStart(EventListController.EVENTS.getName());
		for (int offset = 0;; offset += LIMIT) {
			ObjectNode result = search(offset);
			for (ObjectNode event : EventListController.EVENTS.getValues(result)) {
				out.write(event);
			}
			if (getTotal(result) <= offset + LIMIT) {
				break;
			}
		}
		out.writeEndArray();
	}

	private ObjectNode search(int offset) {
		return events.find(bucketId, createSearch(filters, offset));
	}

	private static EventSearch createSearch(String[] filters, int offset) {
		ListWidget widget = new ListWidget(EventListController.EVENTS.getName(),
			offset, LIMIT, Event.TIMESTAMP.getName(), SortOrder.ASC);
		return new EventSearch().addFilters(filters).addWidget(widget);
	}

	private static int getTotal(ObjectNode result) {
		Integer total = EventSearch.TOTAL.getValue(result);
		Preconditions.checkNotNull(total, "missing total: %s", result);
		return total;
	}
}
