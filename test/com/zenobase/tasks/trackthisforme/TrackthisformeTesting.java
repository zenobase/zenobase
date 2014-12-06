package com.zenobase.tasks.trackthisforme;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class TrackthisformeTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new TrackthisformeTaskManager(newCredentialsManager()), Nodes.newObject()
			.put("marker", "2014-12-01T00:00:00-08:00")
			.put("category", "eat")
			.put("field", "energy")
			.put("unit", "kcal")
			.put("rating", true));
	}

	@Override
	protected TrackthisformeCredentialsManager newCredentialsManager() {
		return new TrackthisformeCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
