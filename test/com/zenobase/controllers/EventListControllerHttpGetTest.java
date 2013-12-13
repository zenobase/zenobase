package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		Search expected = new EventSearchBuilder().addConstraint(constraint).addFacet(facet).buildSearch();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(events.find(bucket.getId(), expected)).thenReturn(fakeResult);
		Result result = call(bucket, String.format("?q=%s&facet=%s", constraint, facet));
		assertThat(result).hasStatus(OK).hasContent(fakeResult);
	}

	@Test
	public void testListEvents() {
		String constraint = "tag:value";
		String facet = "id:events,type:list,limit:25";
		Search expected = new EventSearchBuilder().addConstraint(constraint).addFacet(facet).buildSearch();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(events.find(bucket.getId(), expected)).thenReturn(fakeResult);
		Result result = call(bucket, String.format("?q=%s&limit=25", constraint));
		assertThat(result).hasStatus(OK).hasContent(fakeResult);
	}

	@Test
	public void testExportEventsToJson() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(Mockito.eq(bucket.getId()), Mockito.any(Search.class))).thenReturn(Nodes.newObject());
		Result result = call(bucket, "");
		assertThat(result).hasStatus(OK).hasContentType("application/json");
	}

	@Test
	public void testExportEventsToCsv() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(Mockito.eq(bucket.getId()), Mockito.any(Search.class))).thenReturn(Nodes.newObject());
		Result result = call(bucket, "?accept=text/plain");
		assertThat(result).hasStatus(OK).hasContentType("text/plain");
	}

	@Test
	public void testExportEventsToInvalidFormat() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.find(Mockito.eq(bucket.getId()), Mockito.any(Search.class))).thenReturn(Nodes.newObject());
		Result result = call(bucket, "?accept=foo/bar");
		assertThat(result).hasStatus(BAD_REQUEST);
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
		return callAction(com.zenobase.controllers.routes.ref.EventListController.find(bucket.getId()), fakeRequest(GET, query));
	}
}
