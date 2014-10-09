package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HazelcastManagerTest {

	private HazelcastManager hazelcast;

	@Before
	public void setUp() {
		hazelcast = new HazelcastManager();
	}

	@Test
	public void testToggleReadOnly() {
		assertThat(hazelcast.isReadOnly()).isFalse();
		hazelcast.setReadOnly(true);
		assertThat(hazelcast.isReadOnly()).isTrue();
		hazelcast.setReadOnly(false);
		assertThat(hazelcast.isReadOnly()).isFalse();
	}

	@After
	public void tearDown() {
		hazelcast.close();
	}
}
