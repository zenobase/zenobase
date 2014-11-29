package com.zenobase.tasks.moves;

import org.joda.time.DateTime;
import org.junit.Assume;
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
import com.zenobase.tasks.foursquare.FoursquareVenues;

public class MovesTesting extends TaskTestingSupport {

	@Test
	public void testActivities() {
		run(new MovesActivitiesTaskManager(newCredentialsManager()), Nodes.newObject("marker", "2014-02-06"));
	}

	@Test
	@Ignore
	public void testPlaces() {
		run(new MovesPlacesTaskManager(newCredentialsManager(), newFoursquareVenues()), Nodes.newObject("marker", "2014-02-06"));
	}

	@Test
	@Ignore
	public void testLocate() {
		run(new MovesLocateTaskManager(newCredentialsManager(), fakeEventRepository()), Nodes.newObject("marker", "2014-03-01T12:00:00.000-07:00"));
	}

	private static FoursquareVenues newFoursquareVenues() {
		String apiKey = System.getProperty("foursquare.oauth.apiKey");
		String apiSecret = System.getProperty("foursquare.oauth.apiSecret");
		Assume.assumeNotNull(apiKey, apiSecret);
		return new FoursquareVenues(apiKey, apiKey);
	}

	private static EventRepository fakeEventRepository() {
		EventRepository repository = Mockito.mock(EventRepository.class);
		ObjectNode result = Nodes.newObject();
		ArrayNode eventNodes = result.putArray("events");
		eventNodes.add(newEvent("2014-03-17T12:00:00.000-07:00").toJson());
		eventNodes.add(newEvent("2014-03-17T13:00:00.000-07:00").toJson());
		eventNodes.add(newEvent("2014-03-17T13:00:00.000-07:00").toJson());
		Mockito.when(repository.find(Mockito.anyString(), Mockito.any(Search.class))).thenReturn(result);
		return repository;
	}

	private static Event newEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		return event;
	}

	@Override
	protected MovesCredentialsManager newCredentialsManager() {
		return new MovesCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
