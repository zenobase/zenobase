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
	private static final String KEY_SCHEDULER_DISABLED = "schedulerDisabled";

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
		return is(KEY_READ_ONLY);
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		set(KEY_READ_ONLY, readOnly);
	}

	@Override
	public boolean isSchedulerDisabled() {
		return is(KEY_SCHEDULER_DISABLED);
	}

	@Override
	public void setSchedulerDisabled(boolean schedulerDisabled) {
		set(KEY_SCHEDULER_DISABLED, schedulerDisabled);
	}

	private boolean is(String key) {
		try {
			return map.containsKey(key);
		} catch (OperationTimeoutException|HazelcastInstanceNotActiveException e) {
			return false;
		}
	}

	private void set(String key, boolean value) {
		Logger.warn("Setting {} to {}...", key, value);
		if (value) {
			map.put(key, Boolean.TRUE);
		} else {
			map.remove(key);
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
