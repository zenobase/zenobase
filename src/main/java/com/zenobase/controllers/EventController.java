package com.zenobase.controllers;

import com.zenobase.commands.DeleteEventCommand;
import com.zenobase.commands.UpdateEventCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.opensearch.client.opensearch._types.OpenSearchException;

public class EventController extends ControllerSupport {

	private final BucketRepository buckets;
	private final EventRepository events;
	private final CommandDispatcher dispatcher;

	@Inject
	public EventController(
		AuthorizationContext security,
		BucketRepository buckets,
		EventRepository events,
		CommandDispatcher dispatcher
	) {
		super(security);
		this.buckets = buckets;
		this.events = events;
		this.dispatcher = dispatcher;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		String eventId = req.path().pathParameters().get("eventId");
		Authorization auth = requireAuthorization(req);
		Bucket bucket = requireFound(buckets.find(bucketId));
		requireRole(bucket, auth, Role.VIEWER);
		Event event = requireFound(events.find(bucketId, eventId));
		sendOk(res, event.toJson());
	}

	public void update(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		String eventId = req.path().pathParameters().get("eventId");
		Authorization auth = requireAuthorization(req);
		Bucket bucket = requireFound(buckets.find(bucketId), "bucket not found");
		requireRole(bucket, auth, Role.OWNER);
		requireNotArchived(bucket);
		Event event = requireFound(events.find(bucketId, eventId), "event not found");
		Event updated = new Event(body(req));
		updated.setValue(Event.AUTHOR, auth.getPrincipal());
		requireNotSuspended(auth);
		try {
			String commandId = dispatcher.dispatch(
				new UpdateEventCommand(auth.getPrincipal(), bucketId, event, updated)
			);
			setHeader(res, COMMAND_ID, commandId);
			sendNoContent(res);
		} catch (OpenSearchException e) {
			if (e.status() == 409) {
				sendConflict(res, "event is stale");
				return;
			}
			throw e;
		}
	}

	public void delete(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		String eventId = req.path().pathParameters().get("eventId");
		Authorization auth = requireAuthorization(req);
		Bucket bucket = requireFound(buckets.find(bucketId));
		requireRole(bucket, auth, Role.OWNER);
		requireNotArchived(bucket);
		Event event = requireFound(events.find(bucket.getId(), eventId));
		requireNotSuspended(auth);
		String commandId = dispatcher.dispatch(new DeleteEventCommand(auth.getPrincipal(), bucketId, event));
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}
}
