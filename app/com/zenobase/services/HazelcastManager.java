package com.zenobase.services;

import java.util.Map;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastManager {

	private static final String KEY_READ_ONLY = "readOnly";

	private final HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
	private Map<Object, Object> map;

	public HazelcastManager() {
		map = hazelcast.getMap("map");
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
