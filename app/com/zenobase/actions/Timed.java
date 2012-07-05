package com.zenobase.actions;

import java.util.Collections;
import java.util.Enumeration;

import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;

public class Timed extends Action.Simple {

	@Override
	@Trace(dispatcher=true)
	public Result call(Context context) throws Throwable {
		try {
			Result result = delegate.call(context);
			NewRelic.setRequestAndResponse(new PlayRequest(context), new PlayResponse(result));
			return result;
		} catch (Throwable t) {
			NewRelic.noticeError(t);
			throw t;
		}
	}

	public static class PlayRequest implements Request {

		private final Http.Context context;

		public PlayRequest(Context context) {
			this.context = context;
		}

		@Override
		public String getRequestURI() {
			return context.request().path();
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

	public static class PlayResponse implements Response {

		private final Result result;

		public PlayResponse(Result result) {
			this.result = result;
		}

		@Override
		public int getStatus() {
			return result != null ? 200 : 500;
		}

		@Override
		public String getStatusMessage() {
			return null;
		}

		@Override
		public void setHeader(String name, String value) {

		}
	}
}
