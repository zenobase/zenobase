package com.zenobase.services;

import java.util.Map;

import play.Logger;
import com.google.common.collect.Maps;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastManager {

	private static final String KEY_READ_ONLY = "readOnly";

	private final HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
	private final Map<Object, Object> map = Maps.newHashMap(); // hazelcast.getMap("map");

	public HazelcastManager() {
		Logger.info("Hazelcast: " + hazelcast.getName());
	}

	public boolean isReadOnly() {
		return map.containsKey(KEY_READ_ONLY);
	}

	public void setReadOnly(boolean readOnly) {
		if (readOnly) {
			map.put(KEY_READ_ONLY, Boolean.TRUE);
		} else {
			map.remove(KEY_READ_ONLY);
		}
	}

	public int count() {
		return hazelcast.getCluster().getMembers().size();
	}

	public void close() {
		hazelcast.shutdown();
	}
}
