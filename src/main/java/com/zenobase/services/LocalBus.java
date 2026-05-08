package com.zenobase.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalBus implements Bus {

	private boolean readOnly;
	private boolean schedulerDisabled;
	private final Set<String> locks = ConcurrentHashMap.newKeySet();

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public boolean isSchedulerDisabled() {
		return schedulerDisabled;
	}

	@Override
	public void setSchedulerDisabled(boolean schedulerDisabled) {
		this.schedulerDisabled = schedulerDisabled;
	}

	@Override
	public boolean tryLock(String id) {
		return locks.add(id);
	}

	@Override
	public void unlock(String id) {
		locks.remove(id);
	}

	@Override
	public void close() {}
}
