package com.zenobase.services;

import org.joda.time.LocalTime;
import org.joda.time.Period;

public class SnapshotJob extends Job {

	private final IndexManager manager;

	public SnapshotJob(IndexManager manager) {
		super("snapshot", new LocalTime(1, 0), Period.days(1));
		this.manager = manager;
	}

	@Override
	public void run() {
		manager.getSnapshotManager().snapshot();
	}
}
