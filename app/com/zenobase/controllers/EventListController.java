package com.zenobase.controllers;


import java.io.StringWriter;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.mvc.BodyParser;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.scripts.SpreadsheetPrinter;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;

public class EventListController extends ControllerSupport {

	static final ObjectField EVENTS = new ObjectField("events");

	private final BucketRepository buckets;
	private final EventRepository events;
	private final CommandDispatcher dispatcher;

	@Inject
	public EventListController(AuthorizationContext security, BucketRepository buckets, EventRepository events, CommandDispatcher dispatcher) {
		super(security);
		this.buckets = buckets;
		this.events = events;
		this.dispatcher = dispatcher;
	}

	public Result get(String bucketId) {
		Authorization auth = getCurrentAuthorization();
		Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.hasRole(auth, Role.VIEWER)) {
    		return auth == null ? unauthorized() : forbidden();
    	}
    	String[] widgets = request().queryString().get("w");
    	String[] constraints = request().queryString().get("q");
    	String extract = request().getQueryString("x");
    	if (widgets != null) {
    		Search search = new EventSearchBuilder().addWidgets(widgets).addConstraints(constraints).build();
    		ObjectNode result = events.find(bucketId, search);
    		if (extract != null) {
    			StringWriter out = new StringWriter();
    			new SpreadsheetPrinter(out).print((ArrayNode) result.get(extract));
    			return ok(out.toString());
    		} else {
    			return ok(result);
    		}
    	} else {
        	response().setContentType("application/json");
        	return ok(new EventChunks(events, bucketId, constraints));
    	}
    }

	@BodyParser.Of(value = BodyParser.Json.class)
	public Result post(String bucketId) {
		Authorization auth = getCurrentAuthorization();
		if (auth == null) {
			return unauthorized();
		}
    	Bucket bucket = buckets.find(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (!bucket.hasRole(auth, Role.OWNER)) {
    		return forbidden();
    	}
    	ObjectNode body = body();
    	ImmutableList<ObjectNode> nodes = EVENTS.getValues(body);
    	if (!nodes.isEmpty()) {
    		CompoundCommand command = new CompoundCommand(auth.getPrincipal(), "added events", "removed events");
    		for (ObjectNode node : nodes) {
        		command.add(newCreateEventCommand(auth.getPrincipal(), bucketId, node));
    		}
    		String commandId = dispatcher.dispatch(command);
    		response().setHeader(COMMAND_ID, commandId);
            return noContent();
    	}
    	else {
    		CreateEventCommand command = newCreateEventCommand(auth.getPrincipal(), bucketId, body);
    		String commandId = dispatcher.dispatch(command);
            response().setHeader(LOCATION, com.zenobase.controllers.routes.EventController.get(bucket.getId(), command.getEvent().getId()).toString());
    		response().setHeader(COMMAND_ID, commandId);
            return created(command.getEvent().toJson());
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
