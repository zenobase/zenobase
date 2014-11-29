package com.zenobase.tasks.reporter;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;
import com.zenobase.tasks.dropbox.DropboxCredentialsManager;

public class ReporterTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new ReporterTaskManager(newCredentialsManager()), Nodes.newObject("folder", "Apps/Reporter-App"));
	}

	@Override
	protected DropboxCredentialsManager newCredentialsManager() {
		return new DropboxCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
