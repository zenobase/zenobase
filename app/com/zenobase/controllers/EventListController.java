package com.zenobase.controllers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import javax.inject.Inject;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Results.Chunks.Out;
import play.mvc.With;
import com.google.common.collect.ImmutableList;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.RandomEventsCommandBuilder;
import com.zenobase.common.Generator;
import com.zenobase.json.IntegerField;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.search.EventSearch;
import com.zenobase.search.ListWidget;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;

@With(Timed.class)
public class EventListController extends ControllerSupport {

	static final IntegerField RANDOM = new IntegerField("random");
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
    	EventSearch search = new EventSearch()
			.addWidgets(request().queryString().get("w"))
			.addFilters(request().queryString().get("q"));
    	return ok(buckets.findEvents(bucketId, search));
    }

	public static class ChunksOutputStream extends OutputStream {

		private final Out<byte[]> out;

		public ChunksOutputStream(Out<byte[]> out) {
			this.out = out;
		}

		@Override
		public void write(int b) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(byte[] b) {
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			write(Arrays.copyOfRange(b, off, off + len));
		}
	}

	public static Result getAll(final String bucketId) {
		Logger.info("Downloading " + bucketId + "...");
		Identity principal = auth.getPrincipal();
		Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) == Permission.NONE) {
    		return principal == null ? unauthorized() : forbidden();
    	}
    	final String[] q = request().queryString().get("q");
    	response().setContentType("application/json");
    	Chunks<byte[]> chunks = new ByteChunks() {
			@Override
			public void onReady(Out<byte[]> out) {
				try {
					JsonGenerator generator = new JsonFactory().createJsonGenerator(new ChunksOutputStream(out));
					generator.setCodec(new ObjectMapper());
					generator.useDefaultPrettyPrinter();
					generator.writeStartObject();
					int offset = 0;
					int limit = 10;
					while (true) {
						ListWidget list = new ListWidget(EVENTS.getName(), offset, limit, Event.TIMESTAMP.getName(), SortOrder.ASC);
						EventSearch search = new EventSearch()
							.addFilters(q)
							.addWidget(list);
						ObjectNode node = buckets.findEvents(bucketId, search);
						Integer total = EventSearch.TOTAL.getValue(node);
						if (offset == 0) {
							generator.writeNumberField(EventSearch.TOTAL.getName(), EventSearch.TOTAL.getValue(node));
							if (q != null && q.length > 0) {
								generator.writeArrayFieldStart("filters");
								for (String filter : q) {
									generator.writeString(filter);
								}
								generator.writeEndArray();
							}
							generator.writeArrayFieldStart(EVENTS.getName());
						}
						for (JsonNode event : node.get(EVENTS.getName())) {
							generator.writeTree(event);
						}
						if (total == null || total <= offset + limit) {
							Logger.info("Done!");
							break;
						}
						offset += limit;
					}
					generator.writeEndArray();
					generator.writeEndObject();
					generator.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					out.close();
				}

			}
		};
    	return ok(chunks);
    }

	@BodyParser.Of(value = BodyParser.Json.class)
	public static Result post(String bucketId) {
		Identity principal = auth.getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		ObjectNode body = body();
    	Bucket bucket = buckets.findBucket(bucketId);
    	if (bucket == null) {
    		return notFound();
    	}
    	if (bucket.getPermission(principal) != Permission.ALL) {
    		return forbidden();
    	}
    	Integer random = RANDOM.getValue(body);
    	if (random != null) {
    		String commandId = dispatcher.dispatch(new RandomEventsCommandBuilder(principal, bucketId).build(random));
            return ok(receipt(commandId));
    	}
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
			event.addValue(Event.TIMESTAMP, new DateTime());
		}
		return new CreateEventCommand(principal, bucketId, event);

	}
}
