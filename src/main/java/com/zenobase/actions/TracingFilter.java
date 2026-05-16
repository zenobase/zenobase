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
		String method = req.prologue().method().text();
		// CORS preflight never reaches a user route, so matchingPattern() stays empty;
		// skip the transaction entirely to avoid noise.
		if ("OPTIONS".equals(method)) {
			chain.proceed();
			return;
		}
		// Start the transaction with a placeholder name — matchingPattern() isn't populated
		// until after chain.proceed() returns (Helidon sets it just before dispatching to
		// the route handler). The transaction needs to exist now so child spans (e.g.
		// task.refresh) can attach via bindToScope; we update the name once routing has
		// matched.
		TransactionContext ctx = continueTrace(req, method);
		TransactionOptions options = new TransactionOptions();
		options.setBindToScope(true);
		ITransaction txn = Sentry.startTransaction(ctx, options);
		try {
			chain.proceed();
			txn.setName(transactionName(req, method));
			txn.setStatus(SpanStatus.fromHttpStatusCode(res.status().code()));
		} catch (RuntimeException e) {
			txn.setName(transactionName(req, method));
			txn.setStatus(SpanStatus.INTERNAL_ERROR);
			txn.setThrowable(e);
			throw e;
		} finally {
			txn.finish();
		}
	}

	private static String transactionName(RoutingRequest req, String method) {
		String pattern = req.matchingPattern().orElse("");
		if (pattern.isEmpty()) {
			// No route matched. For "/" use it directly (low cardinality, more informative
			// than "unknown"); for everything else stay anonymous to bound cardinality.
			pattern = "/".equals(req.prologue().uriPath().rawPath()) ? "/" : "unknown";
		}
		return method + " " + pattern;
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
