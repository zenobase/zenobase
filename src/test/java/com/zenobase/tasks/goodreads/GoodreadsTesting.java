package com.zenobase.tasks.goodreads;

import org.junit.jupiter.api.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;

public class GoodreadsTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(
				new GoodreadsTaskManager(newCredentialsManager()),
				Nodes.newObject().put("tag", "Book").put("shelf", "read"));
	}

	@Override
	protected GoodreadsCredentialsManager newCredentialsManager() {
		return new GoodreadsCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
