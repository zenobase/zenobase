package com.zenobase.services;

import java.util.Map;

import play.Logger;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastManager {

	private static final String KEY_READ_ONLY = "readOnly";

	private final HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
	private Map<Object, Object> map;

	public HazelcastManager() {
		Logger.warn("obtaining map for {}...", hazelcast.getName());
		map = hazelcast.getMap("map");
		Logger.warn("map ready");
	}

	public boolean isReadOnly() {
		Logger.warn("accessing map...");
		boolean readOnly = map.containsKey(KEY_READ_ONLY);
		Logger.warn("readOnly is {}", readOnly);
		return readOnly;
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
