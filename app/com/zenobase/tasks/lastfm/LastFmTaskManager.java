package com.zenobase.tasks.lastfm;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class LastFmTaskManager extends OAuthTaskManager {

	private final RateLimiter RATE_LIMIT = RateLimiter.create(5);

	@Inject
	public LastFmTaskManager(LastFmCredentialsManager credentialsManager) {
		super(LastFmTask.TYPE, credentialsManager);
	}

	@Override
	public LastFmTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "body");
		LastFmTask task = new LastFmTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setTimezone(timezone);
		return task;
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? Long.toString(LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).getMillis() / 1000) : null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(LastFmTask.class), credentials);
	}

	private Command execute(LastFmTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		DateTime now = DateTime.now().minusMinutes(5);
		for (int page = 1; page < 100; ++page) {
			LastFmRequest request = createRequest(task, now, credentials, page);
			Response response = send(request, credentials);
			RecentTracksResult result = new RecentTracksResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getTimezone());
			Preconditions.checkState(result.isSuccess(), "Request for %s failed: %s", request.getCompleteUrl(), response.getBody());
			events.addAll(result.getEvents());
			if (!result.hasNext()) {
				break;
			}
		}
		resolveTracks(events, credentials);
		return createCommand(task, events, now);
	}

	private LastFmRequest createRequest(LastFmTask task, DateTime now, OAuthCredentials credentials, int page) {
		LastFmRequest request = new LastFmRequest();
		request.addQuerystringParameter("user", credentials.getScope());
		request.addQuerystringParameter("method", "user.getrecenttracks");
		request.addQuerystringParameter("extended", "0");
		request.addQuerystringParameter("limit", "100");
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
			if (resource.getUrl().startsWith(RecentTracksResult.MUSICBRAINZ_URL)) {
				String mbid = resource.getUrl().substring(resource.getUrl().lastIndexOf('/') + 1);
				LastFmRequest request = createTrackInfoRequest(mbid);
				Response response = send(request, credentials);
				TrackInfoResult result = new TrackInfoResult(parse(response));
				if (result.isSuccess()) {
					result.get().apply(event);
				} else if (!result.isNotFound()) {
					Logger.warn(String.format("Request for %s failed: %s", request.getCompleteUrl(), response.getBody()));
				}
			}
		}
	}

	private LastFmRequest createTrackInfoRequest(String mbid) {
		LastFmRequest request = new LastFmRequest();
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

	private static Command createCommand(Task task, Iterable<Event> events, DateTime to) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran lastfm task", "reverted lastfm task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), Long.toString(to.getMillis() / 1000))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
