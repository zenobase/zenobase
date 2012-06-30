package com.zenobase.actions;

import play.Logger;
import play.Logger.ALogger;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import com.google.common.base.Stopwatch;
import com.newrelic.api.agent.NewRelic;

public class Timed extends Action.Simple {

	private final ALogger log = Logger.of("timer");

	@Override
	public Result call(Context context) throws Throwable {
		NewRelic.setTransactionName("play", context.request().path());
		try {
			Stopwatch timer = new Stopwatch().start();
			Result result = delegate.call(context);
			timer.stop();
			NewRelic.recordResponseTimeMetric("action", timer.elapsedMillis());
			log.info(String.format("%s for %s", timer, context.request().uri()));
			return result;
		} catch (Throwable t) {
			NewRelic.noticeError(t);
			throw t;
		}
	}
}
