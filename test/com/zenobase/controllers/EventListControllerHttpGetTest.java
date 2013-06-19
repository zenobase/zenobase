package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Result;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;

public class EventListControllerHttpGetTest extends EventListControllerTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testSearchEvents() {
		String constraint = "tag:value";
		String facet = "id:xyz,type:list";
		Search expected = new EventSearchBuilder().addConstraint(constraint).addFacet(facet).build();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(events.find(bucket.getId(), expected)).thenReturn(fakeResult);
		Result result = call(bucket, String.format("?q=%s&w=%s", constraint, facet));
		assertThat(result).hasStatus(OK).hasContent(fakeResult);
	}

	@Test
	public void testExportEvents() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(Mockito.eq(bucket.getId()), Mockito.any(Search.class))).thenReturn(Nodes.newObject());
		Result result = call(bucket, "");
		assertThat(result).hasStatus(OK).hasContentType("application/json");
	}

	@Test
	public void testSearchEventsBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(null);
		Result result = call(bucket, "");
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testSearchEventsUnauthorized() {
		when(auth.current()).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, "");
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testSearchEventsForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, "");
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, String query) {
		return callAction(com.zenobase.controllers.routes.ref.EventListController.get(bucket.getId()), fakeRequest(GET, query));
	}
}
