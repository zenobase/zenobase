package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.models.StatusInfo;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class StatusControllerHttpGetTest extends StatusControllerTestSupport {

	@Test
	public void testReadOnly() {
		when(bus.isReadOnly()).thenReturn(true);
		when(bus.isSchedulerDisabled()).thenReturn(false);
		try (Http1ClientResponse result = client.get("/status").request()) {
			assertThat(result).hasStatus(200).hasContent(new StatusInfo(true, false).toJson());
		}
	}

	@Test
	public void testSchedulerDisabled() {
		when(bus.isReadOnly()).thenReturn(false);
		when(bus.isSchedulerDisabled()).thenReturn(true);
		try (Http1ClientResponse result = client.get("/status").request()) {
			assertThat(result).hasStatus(200).hasContent(new StatusInfo(false, true).toJson());
		}
	}
}
