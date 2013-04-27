package com.zenobase.tasks.cosm;

import java.math.BigDecimal;
import java.util.List;

import javax.measure.unit.Dimension;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.json.Field;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class FeedResult {

	public static final Resource SOURCE = new Resource("Cosm", "https://cosm.com/");

	private final Identity author;
	private final JsonNode node;

	public FeedResult(Identity author, JsonNode node) {
		Preconditions.checkNotNull(author);
		Preconditions.checkNotNull(node);
		this.author = author;
		this.node = node;
	}

	public boolean isSuccess() {
		return node.path("errors").isMissingNode();
	}

	public List<Event> getEvents() {
		Preconditions.checkState(isSuccess(), "Expected a successful response but got <%s>", node);
		List<String> tags = getTags(node);
		List<Event> events = Lists.newArrayList();
		for (JsonNode datastream : node.path("datastreams")) {
			events.addAll(getDatastreamEvents(tags, datastream));
		}
		return events;
	}

	private static List<String> getTags(JsonNode node) {
		List<String> tags = Lists.newArrayList();
		for (JsonNode tag : node.path("tags")) {
			tags.add(tag.getTextValue());
		}
		return tags;
	}

	private List<Event> getDatastreamEvents(Iterable<String> feedTags, JsonNode node) {
		Iterable<String> tags = Iterables.concat(feedTags, getTags(node));
		Field<Object> field = getDatastreamField(node);
		Unit<?> unit = getDatastreamUnit(node);
		List<Event> events = Lists.newArrayList();
		for (JsonNode datapoint : node.path("datapoints")) {
			events.add(getEventForDatapoint(tags, field, unit, datapoint));
		}
		return events;
	}

	private static Field<Object> getDatastreamField(JsonNode node) {
		String fieldName = node.path("id").getTextValue();
		Preconditions.checkNotNull(fieldName, "Can't extract a field name from: <%s>", node);
		return (Field<Object>) Event.getField(fieldName.toLowerCase());
	}

	private static Unit<?> getDatastreamUnit(JsonNode node) {
		try {
			String symbol = node.path("unit").path("symbol").getTextValue();
			return symbol != null ? dimensionlessToNull(Measures.parseUnit(symbol)) : null;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(String.format("Can't extract a valid unit from: <%s>", node));
		}
	}

	private static Unit<?> dimensionlessToNull(Unit<?> unit) {
		return unit == null || Dimension.NONE.equals(unit.getDimension()) ? null : unit;
	}

	public Event getEventForDatapoint(Iterable<String> tags, Field<Object> field, Unit<?> unit, JsonNode node) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(node.path("at").getTextValue()));
		for (String tag : tags) {
			event.addValue(Event.TAG, tag);
		}
		event.setValue(field, getDatapointValue(node, unit));
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static Object getDatapointValue(JsonNode node, Unit<?> unit) {
		Preconditions.checkArgument(node.path("value").isTextual(), "Expected text node but found <%s>", node);
		return getDatapointValue(new BigDecimal(node.path("value").getTextValue()), unit);
	}

	private static Object getDatapointValue(BigDecimal value, Unit<?> unit) {
		return unit != null ? Measures.valueOf(value, unit) : value.intValueExact();
	}
}
