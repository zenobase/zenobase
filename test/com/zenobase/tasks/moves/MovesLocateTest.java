package com.zenobase.tasks.moves;

import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.search.Search;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;

public class MovesLocateTest extends TaskTestSupport {

	@Test
	public void test() {
		Event e1 = newEvent("2014-03-17T12:00:00.000-07:00");
		Event e2 = newEvent("2014-03-17T13:00:00.000-07:00");
		Event e3 = newEvent("2014-03-17T13:00:00.000-07:00");
		EventRepository events = newRepository(e1, e2, e3);
		MovesLocateTaskManager manager = new MovesLocateTaskManager(newCredentialsManager(), events);
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-03-01T12:00:00.000-07:00");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	private static Event newEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		return event;
	}

	private static EventRepository newRepository(Event... events) {
		EventRepository repository = Mockito.mock(EventRepository.class);
		ObjectNode result = Nodes.newObject();
		ArrayNode eventNodes = result.putArray("events");
		for (Event event : events) {
			eventNodes.add(event.toJson());
		}
		Mockito.when(repository.find(Mockito.anyString(), Mockito.any(Search.class))).thenReturn(result);
		return repository;
	}

	@Override
	protected MovesCredentialsManager newCredentialsManager() {
		return new MovesCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
