package com.zenobase.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ReplaceTag extends ClientSupport {

	private final String bucketId;
	private final String from;
	private final String to;

	public ReplaceTag(String bucketId, String from, String to) {
		this.bucketId = bucketId;
		this.from = from;
		this.to = to;
	}

	@Override
	protected void doRun() throws IOException {
		for (Event event : find("tag:" + from)) {
			event.setValues(
					Event.TAG, Iterables.transform(event.getValues(Event.TAG), tag -> from.equals(tag) ? to : tag));
			update(event);
		}
	}

	private List<Event> find(String query) throws IOException {
		List<Event> events = new ArrayList<>();
		HttpGet request = new HttpGet(String.format("%s/buckets/%s/?code=%s&q=%s", host, bucketId, token, query));
		ClassicHttpResponse response = client.execute(request);
		ObjectNode node = Nodes.readObject(EntityUtils.toByteArray(response.getEntity()));
		for (JsonNode eventNode : node.path("events")) {
			events.add(new Event((ObjectNode) eventNode));
		}
		System.out.format("Found %d events for <%s>\n", events.size(), query);
		return events;
	}

	private void update(Event event) throws IOException {
		HttpPut request = new HttpPut(String.format("%s/buckets/%s/%s?code=%s", host, bucketId, event.getId(), token));
		request.setHeader("Content-Type", "application/json");
		request.setEntity(new StringEntity(event.toString()));
		ClassicHttpResponse response = client.execute(request);
		System.out.format("Update <%s>: %d\n%s\n", event.getId(), response.getCode(), event);
	}

	public static void main(String[] args) throws IOException {
		new ReplaceTag("u07qih0a27", "foo", "bar").run();
	}
}
