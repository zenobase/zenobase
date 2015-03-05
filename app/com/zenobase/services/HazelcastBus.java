package com.zenobase.services;

import java.util.Map;

import org.elasticsearch.common.collect.Iterables;
import play.Logger;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.Member;
import com.hazelcast.core.OperationTimeoutException;

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
		} catch (OperationTimeoutException|HazelcastInstanceNotActiveException e) {
			return false;
		}
	}

	@Override
	public boolean isReadOnly() {
		try {
			return map.containsKey(KEY_READ_ONLY);
		} catch (OperationTimeoutException|HazelcastInstanceNotActiveException e) {
			return false;
		}
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
		try {
			return hazelcast.getCluster().getMembers().size();
		} catch (OperationTimeoutException|HazelcastInstanceNotActiveException e) {
			return -1;
		}
	}

	@Override
	public void close() {
		hazelcast.shutdown();
	}
}
