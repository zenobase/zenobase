package com.zenobase.scripts;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class DeleteEvents extends ClientSupport {

	private final String bucketId;
	private final String constraint;

	public DeleteEvents(String bucketId, String constraint) {
		this.bucketId = bucketId;
		this.constraint = constraint;
	}

	public static void main(String[] args) throws IOException {
		new DeleteEvents("u07qih0a27", "tag:foo").run();
	}

	@Override
	protected void doRun() throws IOException {
		for (Event event : find(constraint)) {
			delete(event.getId());
		}
	}

	private List<Event> find(String query) throws IOException {
		List<Event> events = new ArrayList<>();
		HttpGet request = new HttpGet(String.format("%s/buckets/%s/?code=%s&q=%s", host, bucketId, token, query));
		HttpResponse response = client.execute(request);
		ObjectNode node = Nodes.readObject(EntityUtils.toByteArray(response.getEntity()));
		for (JsonNode eventNode : node.path("events")) {
			events.add(new Event((ObjectNode) eventNode));
		}
		System.out.format("Found %d events for <%s>\n", events.size(), query);
		return events;
	}

	private void delete(String eventId) throws IOException {
		HttpDelete request = new HttpDelete(String.format("%s/buckets/%s/%s?code=%s", host, bucketId, eventId, token));
		HttpResponse response = client.execute(request);
		System.out.format("Delete <%s>: %d\n", eventId, response.getStatusLine().getStatusCode());
	}
}
