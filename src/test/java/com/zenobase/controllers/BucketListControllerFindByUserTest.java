package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.services.SearchOrder;

public class BucketListControllerFindByUserTest extends BucketListControllerTestSupport {

	private static final SearchOrder ORDER_BY = SearchOrder.valueOf("label", Bucket.SCHEMA);

	@Test
	public void test() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()), ORDER_BY, 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(BucketList.toJson(list));
		}
	}

	@Test
	public void testLabelsOnly() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()), ORDER_BY, 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10, true, true)) {
			assertThat(result).hasStatus(200).hasContent(BucketList.toJson(list));
		}
	}

	@Test
	public void testExcludeArchived() {
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(
						new BucketQuery().principalEqualTo(user.asIdentity()).includeArchived(false), ORDER_BY, 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10, true, false)) {
			assertThat(result).hasStatus(200).hasContent(BucketList.toJson(list));
		}
	}

	@Test
	public void testAsSuperuser() {
		Identity superuser = new Identity();
		PartialList<Bucket> list = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()), ORDER_BY, 0, 10))
				.thenReturn(list);
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10)) {
			assertThat(result).hasStatus(200).hasContent(BucketList.toJson(list));
		}
	}

	@Test
	public void testLimitTooLow() {
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, -1)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testLimitTooHigh() {
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 1000)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooLow() {
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, -1, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testOffsetTooHigh() {
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 10000, 0)) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testMissingAuthorization() {
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testScopedAuthorization() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity(), new Identity(), "someScope"));
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testUserNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call("@none", ORDER_BY, 0, 10)) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testAsNotOwner() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		try (Http1ClientResponse result = call(user.getId(), ORDER_BY, 0, 10)) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String userId, SearchOrder order, int offset, int limit) {
		return call(userId, order, offset, limit, false, true);
	}

	private Http1ClientResponse call(
			String userId, SearchOrder order, int offset, int limit, boolean labelsOnly, boolean includeArchived) {
		return client.get("/users/" + userId + "/buckets/")
				.queryParam("order", String.valueOf(order))
				.queryParam("offset", String.valueOf(offset))
				.queryParam("limit", String.valueOf(limit))
				.queryParam("labels_only", String.valueOf(labelsOnly))
				.queryParam("include_archived", String.valueOf(includeArchived))
				.request();
	}
}
