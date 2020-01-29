package com.zenobase.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.mvc.BodyParser;
import play.mvc.Result;

import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.FacetOptions;
import com.zenobase.search.ListFacet;
import com.zenobase.search.Search;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.EventRepository;
import com.zenobase.services.UserLookup;
import com.zenobase.services.UserRepository;

public class EventListController extends ControllerSupport {

	public static final ObjectField EVENTS = new ObjectField("events");

	private final BucketRepository buckets;
	private final EventRepository events;
	private final UserRepository users;
	private final CommandDispatcher dispatcher;

	@Inject
	public EventListController(AuthorizationContext security, BucketRepository buckets, EventRepository events, UserRepository users, CommandDispatcher dispatcher) {
		super(security);
		this.buckets = buckets;
		this.events = events;
		this.users = users;
		this.dispatcher = dispatcher;
	}

	public Result find(String bucketId) {
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
		return q != null ? Arrays.asList(q) : ImmutableList.of();
	}

	private static List<FacetOptions> getFacets() {
		List<FacetOptions> options = Lists.newArrayList();
		String[] facets = request().queryString().get("facet");
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
		try {
			Search search = new EventSearchBuilder().addConstraints(constraints).addFacets(facets).buildSearch();
			return ok(events.find(bucketId, search));
		} catch (IllegalArgumentException e) {
			return badRequest("Invalid parameters");
		}
    }

	public Result get(String bucketId, List<String> constraints) {
		String accept = Objects.firstNonNull(request().getQueryString("accept"), "application/json");
		if (accept.equals("text/plain")) {
    		response().setContentType(accept);
        	return ok(new EventRows(events, bucketId, constraints));
    	}
		if (accept.equals("application/json")) {
			response().setContentType(accept);
			return ok(new EventChunks(events, bucketId, constraints));
		}
		return badRequest("Can't accept <" + accept + ">");
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
    		CreateEventsCommand command = newCreateEventsCommand(auth.getPrincipal(), bucketId, nodes);
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

	private static CreateEventsCommand newCreateEventsCommand(Identity principal, String bucketId, List<ObjectNode> nodes) {
		return new CreateEventsCommand(principal, bucketId, toEvents(principal, nodes));
	}

	private static CreateEventCommand newCreateEventCommand(Identity principal, String bucketId, ObjectNode node) {
		return new CreateEventCommand(principal, bucketId, toEvent(principal, node));
	}

	private static List<Event> toEvents(Identity principal, List<ObjectNode> nodes) {
		List<Event> events = Lists.newArrayListWithCapacity(nodes.size());
		for (ObjectNode node : nodes) {
			events.add(toEvent(principal, node));
		}
		return events;
	}

	private static Event toEvent(Identity principal, ObjectNode node) {
		Event event = new Event(node);
		event.setValue(Event.ID, Generator.id());
		event.setValue(Event.AUTHOR, principal);
		if (!event.contains(Event.TIMESTAMP)) {
			event.addValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		}
		return event;
	}

	public Result count(String userId) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null) {
    		return unauthorized();
    	}
    	if (auth.getScope() != null) {
    		return forbidden();
    	}
    	if (!users.isSuperuser(auth.getPrincipal())) {
    		return forbidden();
    	}
    	return userId != null ? count(new UserLookup(users).getIdentity(userId)) : count();
    }

	public Result count() {
    	return ok(toJson(events.size()));
    }

	public Result count(Identity principal) {
		if (principal == null) {
			return notFound("user not found");
		}
    	return ok(toJson(events.size(principal)));
    }

	private static ObjectNode toJson(long total) {
		return Nodes.newObject("total", total);
	}
}
