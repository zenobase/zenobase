package com.zenobase.services;

import java.util.Map;

import org.elasticsearch.common.collect.Iterables;
import play.Logger;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.Member;

public class HazelcastBus implements Bus {

	private static final String KEY_READ_ONLY = "readOnly";

	private final HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance();
	private Map<Object, Object> map;

	public HazelcastBus() {
		map = hazelcast.getMap("map");
	}

	@Override
	public boolean isMaster() {
		try {
			Member member = hazelcast.getCluster().getLocalMember();
			return member.equals(Iterables.getFirst(hazelcast.getCluster().getMembers(), member));
		} catch (HazelcastInstanceNotActiveException e) {
			return false;
		}
	}

	@Override
	public boolean isReadOnly() {
		return map.containsKey(KEY_READ_ONLY);
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		if (readOnly) {
			Logger.warn("Enabling read-only mode...");
			map.put(KEY_READ_ONLY, Boolean.TRUE);
		} else {
			Logger.warn("Disabling read-only mode...");
			map.remove(KEY_READ_ONLY);
		}
	}

	@Override
	public int count() {
		return hazelcast.getCluster().getMembers().size();
	}

	@Override
	public void close() {
		hazelcast.shutdown();
	}
}
