package com.zenobase.tasks.google;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.search.Search;
import com.zenobase.services.EventRepository;
import com.zenobase.tasks.TaskTestingSupport;

public class GoogleFitTesting extends TaskTestingSupport {

	@Test
	@Ignore
	public void testActivities() {
		run(new GoogleFitActivitiesTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-13")
			.put("timezone", "America/Los_Angeles")
			.put("metric", true)
			.put("derived", false));
	}

	@Test
	@Ignore
	public void testWeight() {
		run(new GoogleFitWeightTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("metric", true)
			.put("tag", "foo"));
	}

	@Test
	@Ignore
	public void testCardio() {
		run(new GoogleFitCardioTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-10-31")
			.put("timezone", "America/Los_Angeles")
			.put("tag", "bar"));
	}

	@Test
	@Ignore
	public void testFood() {
		run(new GoogleFitFoodTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-13")
			.put("timezone", "America/Los_Angeles")
			.put("tag", "bar"));
	}

	@Test
	public void testLocate() {
		run(new GoogleFitLocateTaskManager(newCredentialsManager(), fakeEventRepository()), Nodes.newObject());
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}

	private static EventRepository fakeEventRepository() {
		EventRepository repository = Mockito.mock(EventRepository.class);
		ObjectNode result = Nodes.newObject();
		ArrayNode eventNodes = result.putArray("events");
		eventNodes.add(newEvent("2014-11-26T15:10:00.000-08:00").toJson());
		eventNodes.add(newEvent("2014-11-26T15:15:00.000-08:00").toJson());
		eventNodes.add(newEvent("2014-12-13T14:00:00.000-08:00").toJson());
		Mockito.when(repository.find(Mockito.anyString(), Mockito.any(Search.class))).thenReturn(result);
		return repository;
	}

	private static Event newEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		return event;
	}
}
