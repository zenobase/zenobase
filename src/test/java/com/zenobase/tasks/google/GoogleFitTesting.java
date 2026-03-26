package com.zenobase.tasks.google;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.tasks.TaskTestingSupport;

public class GoogleFitTesting extends TaskTestingSupport {

	@Test
	public void testActivities() {
		runInApplication(
				new GoogleFitActivitiesTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("marker", "2016-01-27")
						.put("timezone", "America/Los_Angeles")
						.put("metric", true)
						.put("derived", false));
	}

	@Test
	@Ignore
	public void testWeight() {
		runInApplication(
				new GoogleFitWeightTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("marker", "2014-10-31")
						.put("timezone", "America/Los_Angeles")
						.put("metric", true)
						.put("tag", "foo"));
	}

	@Test
	@Ignore
	public void testCardio() {
		runInApplication(
				new GoogleFitCardioTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("marker", "2015-01-17")
						.put("timezone", "America/Los_Angeles")
						.put("tag", "bar"));
	}

	@Test
	@Ignore
	public void testFood() {
		runInApplication(
				new GoogleFitFoodTaskManager(newCredentialsManager()),
				Nodes.newObject()
						.put("marker", "2016-06-01")
						.put("timezone", "America/Los_Angeles")
						.put("tag", "bar"));
	}

	@Override
	protected GoogleCredentialsManager newCredentialsManager() {
		return new GoogleCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}

	private static Event newEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		return event;
	}
}
