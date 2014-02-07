package com.zenobase.tasks.moves;

import org.junit.Assume;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskTestSupport;
import com.zenobase.tasks.foursquare.FoursquareVenues;

public class MovesPlacesTest extends TaskTestSupport {

	@Test
	public void test() {
		MovesPlacesTaskManager manager = new MovesPlacesTaskManager(newCredentialsManager(), newFoursquareVenues());
		ObjectNode settings = Nodes.newObject();
		settings.put("marker", "2014-02-06");
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	@Override
	protected MovesCredentialsManager newCredentialsManager() {
		return new MovesCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}

	private FoursquareVenues newFoursquareVenues() {
		String apiKey = System.getProperty("foursquare.oauth.apiKey");
		String apiSecret = System.getProperty("foursquare.oauth.apiSecret");
		Assume.assumeNotNull(apiKey, apiSecret);
		return new FoursquareVenues(apiKey, apiKey);
	}
}
