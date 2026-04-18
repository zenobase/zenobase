package com.zenobase.actions;

import com.zenobase.controllers.ControllerSupport;
import com.zenobase.services.QuotaException;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import jakarta.inject.Inject;

public class QuotaExceptionFilter implements Filter {

	@Inject
	public QuotaExceptionFilter() {}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		try {
			chain.proceed();
		} catch (QuotaException e) {
			ControllerSupport.sendForbidden(res, String.valueOf(e.getMessage()));
		}
	}
}
