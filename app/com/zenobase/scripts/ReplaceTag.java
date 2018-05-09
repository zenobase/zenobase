package com.zenobase.scripts;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

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
			event.setValues(Event.TAG, Iterables.transform(event.getValues(Event.TAG), tag -> from.equals(tag) ? to : tag));
			update(event);
		}
	}

	private List<Event> find(String query) throws IOException {
		List<Event> events = Lists.newArrayList();
		HttpGet request = new HttpGet(String.format("%s/buckets/%s/?code=%s&q=%s", host, bucketId, token, query));
		HttpResponse response = client.execute(request);
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
		HttpResponse response = client.execute(request);
		System.out.format("Update <%s>: %d\n%s\n", event.getId(), response.getStatusLine().getStatusCode(), event);
	}

	public static void main(String[] args) throws IOException {
		new ReplaceTag("u07qih0a27", "foo", "bar").run();
	}
}
