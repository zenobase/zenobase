package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;

public class EventControllerFindEventsTest extends EventListControllerTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void testFindEvents() {
		String filterExpression = "tag:value";
		String widgetExpression = "id:xyz,type:list";
		EventSearch expected = new EventSearch().addFilter(filterExpression).addWidget(widgetExpression);
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		ObjectNode fakeResult = Nodes.newObject();
		fakeResult.put("test", true);
		when(buckets.findEvents(bucket.getId(), expected)).thenReturn(fakeResult);
		Result result = call(bucket, filterExpression, widgetExpression);
		assertThat(result).hasStatus(OK).hasContent(fakeResult);
	}

	@Test
	public void testMissingBucket() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, "", "");
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, "", "");
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		Result result = call(bucket, "", "");
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(Bucket bucket, String q, String w) {
		return callAction(com.zenobase.controllers.routes.ref.EventListController.find(bucket.getId()), fakeRequest(GET, String.format("?q=%s&w=%s", q, w)));
	}
}
