package com.zenobase.services;

import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(TestJob.class);

	public TestJob() {
		super("test", new LocalTime(0, 0), Period.minutes(5));
	}

	@Override
	public void run() {
		logger.info("Running test job...");
	}
}
