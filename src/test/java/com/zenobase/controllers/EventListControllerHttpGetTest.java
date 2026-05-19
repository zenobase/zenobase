package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

public class EventListControllerHttpGetTest extends EventListControllerTestSupport {

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testSearchEvents() {
		String constraint = "tag:value";
		String facet = "id:xyz,type:list";
		Search expected = new EventSearchBuilder().addConstraint(constraint).addFacet(facet).buildSearch();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(events.find(bucket.getId(), expected)).thenReturn(fakeResult);
		try (Http1ClientResponse result = call(bucket, String.format("?q=%s&facet=%s", constraint, facet))) {
			assertThat(result).hasStatus(200).hasContent(fakeResult);
		}
	}

	@Test
	public void testListEvents() {
		String constraint = "tag:value";
		String facet = "id:events,type:list,limit:25";
		Search expected = new EventSearchBuilder().addConstraint(constraint).addFacet(facet).buildSearch();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(events.find(bucket.getId(), expected)).thenReturn(fakeResult);
		try (Http1ClientResponse result = call(bucket, String.format("?q=%s&limit=25", constraint))) {
			assertThat(result).hasStatus(200).hasContent(fakeResult);
		}
	}

	@Test
	public void testExportEventsToJson() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(ArgumentMatchers.eq(bucket.getId()), ArgumentMatchers.any(Search.class))).thenReturn(
			Nodes.newObject()
		);
		try (Http1ClientResponse result = call(bucket, "")) {
			assertThat(result).hasStatus(200).hasContentType("application/json");
		}
	}

	@Test
	public void testExportEventsToCsv() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		ObjectNode csvResult = Nodes.newObject();
		csvResult.putArray(EventListController.EVENTS.getName());
		when(events.find(ArgumentMatchers.eq(bucket.getId()), ArgumentMatchers.any(Search.class))).thenReturn(
			csvResult
		);
		try (Http1ClientResponse result = call(bucket, "?accept=text/plain")) {
			assertThat(result).hasStatus(200).hasContentType("text/plain");
		}
	}

	@Test
	public void testExportEventsToInvalidFormat() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(ArgumentMatchers.eq(bucket.getId()), ArgumentMatchers.any(Search.class))).thenReturn(
			Nodes.newObject()
		);
		try (Http1ClientResponse result = call(bucket, "?accept=foo/bar")) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testSearchEventsRejectsInvalidQuery() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket, "?q=*")) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testSearchEventsBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(bucket, "")) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testSearchEventsUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket, "")) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testSearchEventsForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		try (Http1ClientResponse result = call(bucket, "")) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(Bucket bucket, String query) {
		var request = client.get("/buckets/" + bucket.getId() + "/");
		if (query.startsWith("?")) {
			for (String param : query.substring(1).split("&")) {
				String[] kv = param.split("=", 2);
				request = request.queryParam(kv[0], kv.length > 1 ? kv[1] : "");
			}
		}
		return request.request();
	}
}
