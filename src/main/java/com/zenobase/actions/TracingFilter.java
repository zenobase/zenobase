package com.zenobase.actions;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.TransactionContext;
import io.sentry.TransactionOptions;
import java.util.List;

public class TracingFilter implements Filter {

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		String name = req.prologue().method().text() + " " + req.matchingPattern().orElse("unknown");
		TransactionContext ctx = continueTrace(req, name);
		TransactionOptions options = new TransactionOptions();
		options.setBindToScope(true);
		ITransaction txn = Sentry.startTransaction(ctx, options);
		try {
			chain.proceed();
			txn.setStatus(SpanStatus.fromHttpStatusCode(res.status().code()));
		} catch (RuntimeException e) {
			txn.setStatus(SpanStatus.INTERNAL_ERROR);
			txn.setThrowable(e);
			throw e;
		} finally {
			txn.finish();
		}
	}

	private static TransactionContext continueTrace(RoutingRequest req, String name) {
		String sentryTrace = req.headers().first(HeaderNames.create("sentry-trace")).orElse(null);
		List<String> baggage = req.headers().all(HeaderNames.create("baggage"), List::of);
		TransactionContext ctx = Sentry.continueTrace(sentryTrace, baggage);
		if (ctx == null) {
			return new TransactionContext(name, "http.server");
		}
		ctx.setName(name);
		ctx.setOperation("http.server");
		return ctx;
	}
}
