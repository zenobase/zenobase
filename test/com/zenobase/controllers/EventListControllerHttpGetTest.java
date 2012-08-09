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
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;

public class EventListControllerHttpGetTest extends EventListControllerTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void testSearchEvents() {
		String filterExpression = "tag:value";
		String widgetExpression = "id:xyz,type:list";
		EventSearch expected = new EventSearch().addFilter(filterExpression).addWidget(widgetExpression);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(buckets.findEvents(bucket.getId(), expected)).thenReturn(fakeResult);
		Result result = call(bucket, String.format("?q=%s&w=%s", filterExpression, widgetExpression));
		assertThat(result).hasStatus(OK).hasContent(fakeResult);
	}

	@Test
	public void testExportEvents() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvents(Mockito.eq(bucket.getId()), Mockito.any(EventSearch.class))).thenReturn(Nodes.newObject());
		Result result = call(bucket, "");
		assertThat(result).hasStatus(OK).hasContentType("application/json");
	}

	@Test
	public void testSearchEventsBucketNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, "");
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testSearchEventsUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, "");
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testSearchEventsForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, "");
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, String query) {
		return callAction(com.zenobase.controllers.routes.ref.EventListController.get(bucket.getId()), fakeRequest(GET, query));
	}
}
