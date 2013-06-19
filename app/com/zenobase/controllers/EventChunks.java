package com.zenobase.controllers;

import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.search.sort.SortOrder;
import com.google.common.base.Preconditions;

import com.zenobase.json.JsonChunks;
import com.zenobase.json.JsonStream;
import com.zenobase.models.Event;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.ListFacet;
import com.zenobase.search.Search;
import com.zenobase.services.EventRepository;

final class EventChunks extends JsonChunks {

	private static final int LIMIT = 100;

	private final EventRepository events;
	private final String bucketId;
	private final String[] constraints;

	public EventChunks(EventRepository events, String bucketId, String[] constraints) {
		this.events = events;
		this.bucketId = bucketId;
		this.constraints = constraints;
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
		return events.find(bucketId, createSearch(constraints, offset));
	}

	private static Search createSearch(String[] filters, int offset) {
		ListFacet facet = new ListFacet(EventListController.EVENTS.getName(),
			offset, LIMIT, Event.TIMESTAMP.getName(), SortOrder.ASC);
		return new EventSearchBuilder().addConstraints(filters).addFacet(facet).build();
	}

	private static int getTotal(ObjectNode result) {
		Integer total = Search.TOTAL.getValue(result);
		Preconditions.checkNotNull(total, "missing total: %s", result);
		return total;
	}
}
