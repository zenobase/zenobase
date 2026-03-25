package com.zenobase.controllers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.io.SpreadsheetPrinter;
import com.zenobase.models.Event;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.ListFacet;
import com.zenobase.search.Search;
import com.zenobase.services.EventRepository;

final class EventRows {

	private static final int LIMIT = 16000;

	private final EventRepository events;
	private final String bucketId;
	private final Iterable<String> constraints;

	public EventRows(EventRepository events, String bucketId, Iterable<String> constraints) {
		this.events = events;
		this.bucketId = bucketId;
		this.constraints = constraints;
	}

	public void writeTo(OutputStream out) {
		try {
			ObjectNode result = search(0);
			OutputStreamWriter writer = new OutputStreamWriter(out);
			new SpreadsheetPrinter(writer).print((ArrayNode) result.get((EventListController.EVENTS.getName())));
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't stream result", e);
		}
	}

	private ObjectNode search(int offset) {
		return events.find(bucketId, createSearch(constraints, offset));
	}

	private static Search createSearch(Iterable<String> constraints, int offset) {
		var facet = new ListFacet(EventListController.EVENTS.getName(),
			offset, LIMIT, Event.TIMESTAMP.getName(), null, Event.SCHEMA);
		return new EventSearchBuilder().addConstraints(constraints).addFacet(facet).buildSearch();
	}
}
