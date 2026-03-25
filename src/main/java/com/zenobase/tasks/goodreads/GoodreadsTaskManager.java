package com.zenobase.tasks.goodreads;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;

import jakarta.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class GoodreadsTaskManager extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(1);
	private static final String HOST = "https://www.goodreads.com";
	private static final int MAX_PAGES = 10;

	@Inject
	public GoodreadsTaskManager(GoodreadsCredentialsManager credentialsManager) {
		super(GoodreadsTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTime marker = parseMarker(settings.path("marker").textValue());
		String tag = Preconditions.checkNotNull(settings.path("tag").textValue());
		String shelf = Preconditions.checkNotNull(settings.path("shelf").textValue());
		return new GoodreadsTask(bucketId, principal, marker != null ? marker.toString() : null, tag, shelf);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(GoodreadsTask.class), credentials);
	}

	private Command execute(GoodreadsTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		DateTime from = parseMarker(task.getMarker());
		String userId = getUser(credentials).getId();
		for (int page = 1; page <= MAX_PAGES; ++page) {
			GoodreadsReviewListResult result = getReviewList(credentials, task, userId, page);
			if (!events.addAll(result.getEvents(from)) || page == result.getEndPage()) {
				break;
			}
		}
		return createCommand(task, events);
	}

	private GoodreadsUserResult getUser(OAuthCredentials credentials) {
		var request = new OAuthRequest(Verb.GET, HOST + "/api/auth_user");
		Response response = send(request, credentials);
		return new GoodreadsUserResult(parseDocument(response));
	}

	private GoodreadsReviewListResult getReviewList(OAuthCredentials credentials, GoodreadsTask task, String userId, int page) {
		var request = new OAuthRequest(Verb.GET, HOST + "/review/list.xml");
		request.addQuerystringParameter("v", "2");
		request.addQuerystringParameter("id", userId);
		if (task.getShelf() != null) {
			request.addQuerystringParameter("shelf", task.getShelf());
		}
		request.addQuerystringParameter("sort", "date_read");
		request.addQuerystringParameter("per_page", "200");
		request.addQuerystringParameter("page", Integer.toString(page));
		Response response = send(request, credentials);
		return new GoodreadsReviewListResult(parseDocument(response), task.getPrincipal(), task.getTag());
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	private static Document parseDocument(Response response) {
		Preconditions.checkState(response.getCode() == 200,
			"Expected 200 but got <%s> for <%s>", response.getCode());
		Preconditions.checkState(response.getHeader("Content-Type").startsWith("application/xml"),
			"Expected application/xml but got <%s> for <%s>", response.getHeader("Content-Type"));
		try {
			InputSource source = new InputSource(new StringReader(getBody(response)));
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new RuntimeException("Couldn't parse response", e);
		}
	}

	private static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime end = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (latest == null || end.isAfter(latest)) {
				latest = end.plusMillis(1);
			}
		}
		return latest != null ? latest.toString() : null;
	}

	private Command createCommand(Task task, List<Event> events) {
		var command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
