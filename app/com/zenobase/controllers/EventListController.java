package com.zenobase.controllers;


import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.collect.ImmutableList;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;

@With(Timed.class)
public class EventListController extends ControllerSupport {

	static final ObjectField EVENTS = new ObjectField("events");

	@Inject
	static BucketRepository buckets;

	@Inject
	static CommandDispatcher dispatcher;

	public static Result get(String bucketId) {
		Identity principal = auth.getPrincipal();
		Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return principal == null ? unauthorized() : forbidden();
    	}
    	String[] widgets = request().queryString().get("w");
    	String[] filters = request().queryString().get("q");
    	if (widgets != null) {
    		EventSearch search = new EventSearch().addWidgets(widgets).addFilters(filters);
    		return ok(buckets.findEvents(bucketId, search));
    	} else {
        	response().setContentType("application/json");
        	return ok(new EventChunks(buckets, bucketId, filters));
    	}
    }

	@BodyParser.Of(value = BodyParser.Json.class)
	public static Result post(String bucketId) {
		Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	ObjectNode body = body();
    	ImmutableList<ObjectNode> nodes = EVENTS.getValues(body);
    	if (!nodes.isEmpty()) {
    		CompoundCommand cmd = new CompoundCommand(principal, "added events", "removed events");
    		for (ObjectNode node : nodes) {
        		cmd.add(newCreateEventCommand(principal, bucketId, node));
    		}
    		String commandId = dispatcher.dispatch(cmd);
            return ok(receipt(commandId));
    	}
    	else {
    		CreateEventCommand command = newCreateEventCommand(principal, bucketId, body);
    		String commandId = dispatcher.dispatch(command);
            response().setHeader(LOCATION, com.zenobase.controllers.routes.EventController.get(bucket.getId(), command.getEvent().getId()).toString());
            return created(receipt(commandId));
    	}
    }

	private static CreateEventCommand newCreateEventCommand(Identity principal, String bucketId, ObjectNode node) {
		Event event = new Event(node);
		event.setValue(Event.ID, Generator.id());
		event.setValue(Event.AUTHOR, principal);
		if (!event.contains(Event.TIMESTAMP)) {
			event.addValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		}
		return new CreateEventCommand(principal, bucketId, event);

	}
}
