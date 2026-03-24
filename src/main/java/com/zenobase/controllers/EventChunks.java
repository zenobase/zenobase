package com.zenobase.controllers;

import java.io.IOException;

import com.zenobase.json.JsonChunks;
import com.zenobase.json.JsonStream;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.services.EventRepository;

final class EventChunks extends JsonChunks {

	private final EventRepository events;
	private final String bucketId;
	private final Iterable<String> constraints;

	public EventChunks(EventRepository events, String bucketId, Iterable<String> constraints) {
		this.events = events;
		this.bucketId = bucketId;
		this.constraints = constraints;
	}

	@Override
	public void onReady(JsonStream out) throws IOException {
		out.writeArrayFieldStart(EventListController.EVENTS.getName());
		events.find(bucketId, new EventSearchBuilder().addConstraints(constraints).buildSearch(), node -> {
			try {
				out.write(node);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		out.writeEndArray();
	}
}
