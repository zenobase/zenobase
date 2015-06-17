package com.zenobase.services;

import javax.inject.Inject;

import org.joda.time.Period;
import play.Logger;

public class TestJob extends Job {

	@Inject
	public TestJob() {
		super("test", Period.minutes(5));
	}

	@Override
	public void run() {
		Logger.info("Running test job...");
	}
}
