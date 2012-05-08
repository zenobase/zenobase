package com.zenobase.services;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class ElasticSearchTestSupport {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private String clusterName = "test";
	private IndexManager manager;

	@Before
	public void init() {
		manager = new IndexManager(clusterName, false, true, ImmutableSettings.settingsBuilder()
			.put("path.home", folder.getRoot().getAbsolutePath())
			.put("gateway.type", "none")
			.put("index.store.type", "memory").build());
	}

	protected IndexManager getManager() {
		return manager;
	}

	protected String getClusterName() {
		return clusterName;
	}

	@After
	public void close() {
		manager.close();
	}
}
