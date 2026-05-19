package com.zenobase.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.zenobase.services.QuotaException;
import io.helidon.http.HeaderName;
import io.helidon.http.Status;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import org.junit.jupiter.api.Test;

public class QuotaExceptionFilterTest {

	private final QuotaExceptionFilter filter = new QuotaExceptionFilter();

	@Test
	public void testQuotaExceptionIsConvertedTo403() {
		FilterChain chain = mock(FilterChain.class);
		doThrow(new QuotaException(0, 1)).when(chain).proceed();

		RoutingResponse res = mock(RoutingResponse.class);
		when(res.status(any(Status.class))).thenReturn(res);
		when(res.header(any(HeaderName.class), any(String[].class))).thenReturn(res);

		filter.filter(chain, mock(RoutingRequest.class), res);

		verify(res).status(Status.FORBIDDEN_403);
	}

	@Test
	public void testUnrelatedExceptionPropagates() {
		FilterChain chain = mock(FilterChain.class);
		IllegalStateException boom = new IllegalStateException("not a quota problem");
		doThrow(boom).when(chain).proceed();

		RoutingResponse res = mock(RoutingResponse.class);
		try {
			filter.filter(chain, mock(RoutingRequest.class), res);
			org.assertj.core.api.Assertions.fail("expected IllegalStateException to propagate");
		} catch (IllegalStateException e) {
			org.assertj.core.api.Assertions.assertThat(e).isSameAs(boom);
		}
		verify(res, never()).status(any(Status.class));
	}

	@Test
	public void testNormalProceedDoesNotTouchResponse() {
		FilterChain chain = mock(FilterChain.class);
		doNothing().when(chain).proceed();

		RoutingResponse res = mock(RoutingResponse.class);

		filter.filter(chain, mock(RoutingRequest.class), res);

		verify(res, never()).status(any(Status.class));
		verify(chain).proceed();
	}
}
