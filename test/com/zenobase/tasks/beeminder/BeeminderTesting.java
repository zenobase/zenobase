package com.zenobase.tasks.beeminder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.search.Search;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.TaskTestingSupport;

public class BeeminderTesting extends TaskTestingSupport {

	@Test
	public void testCount() {
		run(new BeeminderTaskManager(newCredentialsManager(), fakeEventRepository()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("goal", "test")
			.put("field", "duration"));
	}

	@Override
	protected BeeminderCredentialsManager newCredentialsManager() {
		return new BeeminderCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}

	private static EventRepository fakeEventRepository() {
		EventRepository repository = Mockito.mock(EventRepository.class);
		ObjectNode result = Nodes.newObject();
		result.putArray("latest").add(newEvent("2015-01-04T12:00:00.000-08:00").toJson());
		ArrayNode stats = result.putArray("stats");
		add(stats, "2015-01-01T-08:00", 1, 60000L);
		add(stats, "2015-01-02T-08:00", 2, 120000L);
		add(stats, "2015-01-03T-08:00", 0, null);
		add(stats, "2015-01-04T-08:00", 1, 90000L);
		Mockito.when(repository.find(Matchers.anyString(), Matchers.any(Search.class))).thenReturn(result);
		return repository;
	}

	private static Event newEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		return event;
	}

	private static void add(ArrayNode node, String label, int count, Long sum) {
		node.add(Nodes.newObject("label", label).put("count", count).put("sum", sum));
	}
}
