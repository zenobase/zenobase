package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

public class BucketListControllerFindAllTest extends BucketListControllerTestSupport {

	@Test
	public void test() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(buckets.find(new BucketQuery().queryString("foo"), BucketQuery.DEFAULT_ORDER, 0, 10)).thenReturn(list);
		try (Http1ClientResponse result = call("foo", 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(BucketList.toJson(list));
		}
	}

	@Test
	public void testWithoutAuthorization() {
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testWithScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testWithNonSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(null, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String q, int offset, int limit) {
		var request = client
			.get("/buckets/")
			.queryParam("offset", String.valueOf(offset))
			.queryParam("limit", String.valueOf(limit));
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
