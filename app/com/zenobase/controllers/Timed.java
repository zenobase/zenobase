package com.zenobase.controllers;

import play.Logger;
import play.Logger.ALogger;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;
import play.mvc.Result;
import com.google.common.base.Stopwatch;

public class Timed extends Simple {

	private final ALogger log = Logger.of("timer");

	@Override
	public Result call(Context context) throws Throwable {
		Stopwatch timer = new Stopwatch().start();
		Result result = delegate.call(context);
		timer.stop();
		log.info(String.format("%s for %s", timer, context.request().uri()));
		return result;
	}
}
