package com.zenobase.tasks.openmhealth;

import org.junit.Test;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.TaskTestingSupport;
import com.zenobase.tasks.dropbox.DropboxCredentialsManager;

public class HipboneTesting extends TaskTestingSupport {

	@Test
	public void test() {
		run(new HipboneTaskManager(newCredentialsManager()), Nodes.newObject("folder", "/Apps/Hipbone/data"));
	}

	@Override
	protected DropboxCredentialsManager newCredentialsManager() {
		return new DropboxCredentialsManager(repository, apiKey, apiSecret, callbackUrl);
	}
}
