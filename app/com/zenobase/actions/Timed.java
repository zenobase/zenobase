package com.zenobase.actions;

import java.util.Collections;
import java.util.Enumeration;

import play.Logger;
import play.Logger.ALogger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import com.google.common.base.Stopwatch;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;

public class Timed extends Action.Simple {

	private final ALogger log = Logger.of("timer");

	@Override
	@Trace(dispatcher=true)
	public Result call(Context context) throws Throwable {
		NewRelic.setTransactionName("http", context.request().path());
		try {
			Stopwatch timer = new Stopwatch().start();
			Result result = delegate.call(context);
			timer.stop();
			NewRelic.recordResponseTimeMetric("action", timer.elapsedMillis());
			PlayRequest r = new PlayRequest(context);
			NewRelic.setRequestAndResponse(r, r);
			log.info(String.format("%s for %s", timer, context.request().uri()));
			return result;
		} catch (Throwable t) {
			NewRelic.noticeError(t);
			throw t;
		}
	}

	public static class PlayRequest implements Request, Response {

		private final Http.Context context;

		public PlayRequest(Context context) {
			this.context = context;
		}

		@Override
		public int getStatus() throws Exception {
			return 200;
		}

		@Override
		public String getStatusMessage() throws Exception {
			return null;
		}

		@Override
		public void setHeader(String name, String value) {

		}

		@Override
		public String getRequestURI() {
			return context.request().uri();
		}

		@Override
		public String getHeader(String name) {
			return context.request().getHeader(name);
		}

		@Override
		public String getRemoteUser() {
			return context.request().remoteAddress();
		}

		@Override
		public Enumeration getParameterNames() {
			return Collections.enumeration(context.request().queryString().keySet());
		}

		@Override
		public String[] getParameterValues(String name) {
			return context.request().queryString().get(name);
		}

		@Override
		public Object getAttribute(String name) {
			return null;
		}
	}
}
