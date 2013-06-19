package com.zenobase.controllers;


import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
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
import com.zenobase.search.FacetOptions;
import com.zenobase.search.ListFacet;
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
    	List<String> constraints = getConstraints();
    	List<FacetOptions> facets = getFacets();
    	return !facets.isEmpty()
    		? get(bucketId, constraints, facets)
    		: get(bucketId, constraints);
    }

	private static List<String> getConstraints() {
		String[] q = request().queryString().get("q");
		return q != null ? Arrays.asList(q) : ImmutableList.<String>of();
	}

	private static List<FacetOptions> getFacets() {
		List<FacetOptions> options = Lists.newArrayList();
		String[] facets = request().queryString().get("facet");
		if (facets == null) {
			facets = request().queryString().get("w");
		}
		if (facets != null) {
			for (String facet : facets) {
				options.add(FacetOptions.parse(facet));
			}
		}
    	if (options.isEmpty() && request().getQueryString("limit") != null) {
    		Map<String, String> map = Maps.newLinkedHashMap();
			map.put("id", "events");
			map.put("type", ListFacet.TYPE);
			copyRequestParameter("offset", map);
			copyRequestParameter("limit", map);
			copyRequestParameter("sort", map);
			copyRequestParameter("order", map);
    		options.add(new FacetOptions(map));
    	}
		return options;
	}

	private static void copyRequestParameter(String key, Map<String, String> target) {
		String value = request().getQueryString(key);
		if (value != null) {
			target.put(key, value);
		}
	}

	public Result get(String bucketId, List<String> constraints, List<FacetOptions> facets) {
		Search search = new EventSearchBuilder().addConstraints(constraints).addFacets(facets).build();
		ObjectNode result = events.find(bucketId, search);
    	String extract = request().getQueryString("x");
		if (extract != null) {
			StringWriter out = new StringWriter();
			new SpreadsheetPrinter(out).print((ArrayNode) result.get(extract));
			return ok(out.toString());
		} else {
			return ok(result);
		}
    }

	public Result get(String bucketId, List<String> constraints) {
    	response().setContentType("application/json");
    	return ok(new EventChunks(events, bucketId, constraints));
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
