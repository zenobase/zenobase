package com.zenobase.services;

import javax.inject.Inject;

import org.joda.time.LocalTime;
import org.joda.time.Period;

public class SnapshotJob extends Job {

	private final IndexManager manager;

	@Inject
	public SnapshotJob(IndexManager manager) {
		super("snapshot", new LocalTime(1, 0), Period.hours(8));
		this.manager = manager;
	}

	@Override
	public void run() {
		manager.getSnapshotManager().snapshot();
	}
}
