package com.zenobase.tasks.lastfm;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.InvalidCredentialsException;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class LastFmTaskManager extends OAuthTaskManager {

	private static final Logger logger = LoggerFactory.getLogger(LastFmTaskManager.class);

	private final RateLimiter RATE_LIMIT = RateLimiter.create(5);

	@Inject
	public LastFmTaskManager(LastFmCredentialsManager credentialsManager) {
		super(LastFmTask.TYPE, credentialsManager);
	}

	@Override
	public LastFmTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone timezone = DateTimeZone.forID(
				MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "body");
		var task = new LastFmTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setTimezone(timezone);
		return task;
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null
				? Long.toString(LocalDateTime.parse(marker.replaceAll("Z", ""))
								.toDateTime(timezone)
								.getMillis()
						/ 1000)
				: null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(LastFmTask.class), credentials);
	}

	private Command execute(LastFmTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		DateTime now = DateTime.now().minusMinutes(5);
		try {
			for (int page = 1; page < 10; ++page) {
				LastFmRequest request = createRequest(task, now, credentials, page);
				Response response = send(request, credentials);
				RecentTracksResult result = new RecentTracksResult(
						parseObject(response), task.getPrincipal(), task.getTag(), task.getTimezone());
				Preconditions.checkState(result.isSuccess(), "Request for %s failed", request.getCompleteUrl());
				events.addAll(result.getEvents());
				if (!result.hasNext()) {
					break;
				}
			}
			resolveTracks(events, credentials);
			return createCommand(task, events, now);
		} catch (InvalidStatusException e) {
			if (e.getStatus() == 403) {
				throw new InvalidCredentialsException(credentials);
			} else {
				throw e;
			}
		}
	}

	private LastFmRequest createRequest(LastFmTask task, DateTime now, OAuthCredentials credentials, int page) {
		var request = new LastFmRequest();
		request.addQuerystringParameter("user", credentials.getScope());
		request.addQuerystringParameter("method", "user.getrecenttracks");
		request.addQuerystringParameter("extended", "0");
		request.addQuerystringParameter("limit", "200");
		request.addQuerystringParameter("page", Integer.toString(page));
		request.addQuerystringParameter("to", Long.toString(now.getMillis() / 1000));
		if (task.getMarker() != null) {
			request.addQuerystringParameter("from", task.getMarker());
		}
		return request;
	}

	private void resolveTracks(List<Event> events, OAuthCredentials credentials) {
		for (Event event : events) {
			Resource resource = event.getValue(Event.RESOURCE);
			if (resource.url().startsWith(RecentTracksResult.MUSICBRAINZ_URL)) {
				String mbid = resource.url().substring(resource.url().lastIndexOf('/') + 1);
				LastFmRequest request = createTrackInfoRequest(mbid);
				Response response = send(request, credentials);
				TrackInfoResult result = new TrackInfoResult(parse(response));
				if (result.isSuccess()) {
					result.get().apply(event);
				} else if (!result.isNotFound()) {
					logger.warn("Request for {} failed: {}", request.getCompleteUrl(), response.getBody());
				}
			}
		}
	}

	private LastFmRequest createTrackInfoRequest(String mbid) {
		var request = new LastFmRequest();
		request.addQuerystringParameter("method", "track.getinfo");
		request.addQuerystringParameter("mbid", mbid);
		return request;
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMIT.acquire();
		request.addQuerystringParameter("format", "json");
		return super.send(request, credentials);
	}

	private static Command createCommand(Task task, List<Event> events, DateTime to) {
		var command = new CompoundCommand(task.getPrincipal(), "ran lastfm task", "reverted lastfm task");
		command.add(UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), Long.toString(to.getMillis() / 1000))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
