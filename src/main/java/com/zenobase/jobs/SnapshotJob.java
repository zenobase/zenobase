package com.zenobase.jobs;

import jakarta.inject.Inject;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import com.zenobase.repositories.IndexManager;

public class SnapshotJob extends Job {

	private final IndexManager manager;

	@Inject
	public SnapshotJob(IndexManager manager) {
		super("snapshot", new LocalTime(1, 0), Period.days(1));
		this.manager = manager;
	}

	@Override
	public void run() {
		manager.getSnapshotManager().snapshot();
	}
}
