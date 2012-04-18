package com.zenobase.controllers;

import play.Logger;
import play.Logger.ALogger;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;
import play.mvc.Result;

public class Timed extends Simple {

	private final ALogger log = Logger.of("timer");

	@Override
	public Result call(Context context) throws Throwable {
		long t0 = System.nanoTime();
		Result result = delegate.call(context);
		long t1 = System.nanoTime();
		log.info(String.format("%,dms for %s", (t1 - t0) / (1000 * 1000), context.request().uri()));
		return result;
	}
}
