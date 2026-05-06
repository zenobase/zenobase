package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.common.Generator;
import com.zenobase.io.SpreadsheetPrinter;
import com.zenobase.json.JsonStream;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.search.facets.FacetOptions;
import com.zenobase.search.facets.ListFacet;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(EventListController.class);

	public static final ObjectField EVENTS = new ObjectField("events");

	private static final int EXPORT_LIMIT = 10000;

	private final BucketRepository buckets;
	private final EventRepository events;
	private final UserRepository users;
	private final CommandDispatcher dispatcher;

	@Inject
	public EventListController(
		AuthorizationContext security,
		BucketRepository buckets,
		EventRepository events,
		UserRepository users,
		CommandDispatcher dispatcher
	) {
		super(security);
		this.buckets = buckets;
		this.events = events;
		this.users = users;
		this.dispatcher = dispatcher;
	}

	public void find(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		Authorization auth = getCurrentAuthorization(req);
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res);
			return;
		}
		if (!bucket.hasRole(auth, Role.VIEWER)) {
			if (auth == null) {
				sendUnauthorized(res);
			} else {
				sendForbidden(res);
			}
			return;
		}
		List<String> constraints = getConstraints(req);
		List<FacetOptions> facets = getFacets(req);
		if (!facets.isEmpty()) {
			getWithFacets(res, bucketId, constraints, facets);
		} else {
			getStreaming(req, res, bucketId, constraints);
		}
	}

	private static List<String> getConstraints(ServerRequest req) {
		try {
			List<String> q = req.query().all("q");
			return q != null && !q.isEmpty() ? q : List.of();
		} catch (java.util.NoSuchElementException e) {
			return List.of();
		}
	}

	private static List<FacetOptions> getFacets(ServerRequest req) {
		List<FacetOptions> options = new ArrayList<>();
		try {
			List<String> facets = req.query().all("facet");
			if (facets != null) {
				for (String facet : facets) {
					options.add(FacetOptions.parse(facet));
				}
			}
		} catch (java.util.NoSuchElementException e) {
			// no query params at all
		}
		if (options.isEmpty() && req.query().first("limit").orElse(null) != null) {
			Map<String, String> map = Maps.newLinkedHashMap();
			map.put("id", "events");
			map.put("type", ListFacet.TYPE);
			copyRequestParameter(req, "offset", map);
			copyRequestParameter(req, "limit", map);
			copyRequestParameter(req, "order", map);
			options.add(new FacetOptions(map));
		}
		return options;
	}

	private static void copyRequestParameter(ServerRequest req, String key, Map<String, String> target) {
		String value = req.query().first(key).orElse(null);
		if (value != null) {
			target.put(key, value);
		}
	}

	private void getWithFacets(
		ServerResponse res,
		String bucketId,
		List<String> constraints,
		List<FacetOptions> facets
	) {
		try {
			Search search = new EventSearchBuilder().addConstraints(constraints).addFacets(facets).buildSearch();
			sendOk(res, events.find(bucketId, search));
		} catch (IllegalArgumentException e) {
			sendBadRequest(res, "Invalid parameters");
		} catch (OpenSearchException e) {
			if (Search.hasCauseOfType(e, "too_many_buckets")) {
				logger.warn("Search failed due to too many buckets in <{}>", bucketId, e);
				sendBadRequest(res, "One or more aggregations create too many bins; try setting larger intervals");
			} else {
				throw e;
			}
		}
	}

	private void getStreaming(ServerRequest req, ServerResponse res, String bucketId, List<String> constraints) {
		String accept = MoreObjects.firstNonNull(req.query().first("accept").orElse(null), "application/json");
		if (accept.equals("text/plain")) {
			setHeader(res, "Content-Type", accept);
			streamEventRows(res, bucketId, constraints);
			return;
		}
		if (accept.equals("application/json")) {
			setHeader(res, "Content-Type", accept);
			streamEventChunks(res, bucketId, constraints);
			return;
		}
		sendBadRequest(res, "Can't accept <" + accept + ">");
	}

	private void streamEventChunks(ServerResponse res, String bucketId, Iterable<String> constraints) {
		try (var out = res.outputStream()) {
			JsonStream stream = new JsonStream(out);
			stream.writeArrayFieldStart(EVENTS.getName());
			events.find(bucketId, new EventSearchBuilder().addConstraints(constraints).buildSearch(), node -> {
				try {
					stream.write(node);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			stream.writeEndArray();
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void streamEventRows(ServerResponse res, String bucketId, Iterable<String> constraints) {
		try (var out = res.outputStream()) {
			ObjectNode result = events.find(bucketId, createExportSearch(constraints, 0));
			OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
			new SpreadsheetPrinter(writer).print((ArrayNode) result.get(EVENTS.getName()));
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't stream result", e);
		}
	}

	private static Search createExportSearch(Iterable<String> constraints, int offset) {
		var facet = new ListFacet(
			EVENTS.getName(),
			offset,
			EXPORT_LIMIT,
			Event.TIMESTAMP.getName(),
			null,
			Event.SCHEMA
		);
		return new EventSearchBuilder().addConstraints(constraints).addFacet(facet).buildSearch();
	}

	public void post(ServerRequest req, ServerResponse res) {
		String bucketId = req.path().pathParameters().get("bucketId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			sendNotFound(res);
			return;
		}
		if (!bucket.hasRole(auth, Role.OWNER)) {
			sendForbidden(res);
			return;
		}
		if (bucket.isArchived()) {
			sendConflict(res, "bucket is archived");
			return;
		}
		if (!requireNotSuspended(auth, res)) {
			return;
		}
		ObjectNode body = body(req);
		var nodes = EVENTS.getValues(body);
		if (!nodes.isEmpty()) {
			CreateEventsCommand command = newCreateEventsCommand(auth.getPrincipal(), bucketId, nodes);
			String commandId = dispatcher.dispatch(command);
			setHeader(res, COMMAND_ID, commandId);
			sendNoContent(res);
		} else {
			CreateEventCommand command = newCreateEventCommand(auth.getPrincipal(), bucketId, body);
			String commandId = dispatcher.dispatch(command);
			setHeader(res, LOCATION, "/buckets/" + bucketId + "/" + command.getEvent().getId());
			setHeader(res, COMMAND_ID, commandId);
			sendCreated(res, command.getEvent().toJson());
		}
	}

	private static CreateEventsCommand newCreateEventsCommand(
		Identity principal,
		String bucketId,
		List<ObjectNode> nodes
	) {
		return new CreateEventsCommand(principal, bucketId, toEvents(principal, nodes));
	}

	private static CreateEventCommand newCreateEventCommand(Identity principal, String bucketId, ObjectNode node) {
		return new CreateEventCommand(principal, bucketId, toEvent(principal, node));
	}

	private static List<Event> toEvents(Identity principal, List<ObjectNode> nodes) {
		List<Event> events = new ArrayList<>(nodes.size());
		for (ObjectNode node : nodes) {
			events.add(toEvent(principal, node));
		}
		return events;
	}

	private static Event toEvent(Identity principal, ObjectNode node) {
		var event = new Event(node);
		event.setValue(Event.ID, Generator.id());
		event.setValue(Event.AUTHOR, principal);
		if (!event.contains(Event.TIMESTAMP)) {
			event.addValue(Event.TIMESTAMP, DateTime.now(DateTimeZone.UTC));
		}
		return event;
	}

	public void countAll(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		sendOk(res, toJson(events.size()));
	}

	public void countByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			sendNotFound(res, "user not found");
			return;
		}
		sendOk(res, toJson(events.size(principal)));
	}

	private static ObjectNode toJson(long total) {
		return Nodes.newObject("total", total);
	}
}
