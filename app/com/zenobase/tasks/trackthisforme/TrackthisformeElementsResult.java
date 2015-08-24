package com.zenobase.tasks.trackthisforme;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.zenobase.common.Measures;
import com.zenobase.json.DecimalField;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.DurationField;
import com.zenobase.json.Field;
import com.zenobase.json.IntegerField;
import com.zenobase.json.PercentageField;
import com.zenobase.json.RatingField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Percentage;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;

class TrackthisformeElementsResult {

	private final JsonNode node;
	private final Identity author;
	private final DateTime begin;
	private final Category category;
	private final String field;
	private final String unit;
	private final boolean rating;

	public TrackthisformeElementsResult(JsonNode node, Identity author, DateTime begin, Category category, String field, String unit, boolean rating) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.begin = begin;
		this.category = category;
		this.field = field;
		this.unit = unit;
		this.rating = rating;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode elementNode : node.path("elements")) {
			Event event = newEvent(elementNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		Event event = null;
		DateTime t = dateTimeValue(node.path("date"));
		if (t.isAfter(begin)) {
			event = new Event();
			event.addValue(Event.TAG, category.getName());
			event.setValue(Event.TIMESTAMP, t);
			if (field != null) {
				setValue(event, node.path("value_str"));
			}
			if (rating) {
				event.setValue(Event.RATING, ratingValue(node.path("stars")));
			}
			event.setValue(Event.NOTE, Strings.emptyToNull(node.path("comment").textValue()));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, sourceValue(node.path("category_id")));
		}
		return event;
	}

	private DateTime dateTimeValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue());
	}

	private Resource sourceValue(JsonNode node) {
		return !isZero(node) ? new Resource("Trackthisforme", "https://www.trackthisfor.me/#/" + node.asText()) : null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setValue(Event event, JsonNode node) {
		Field field = Event.SCHEMA.getField(this.field);
		event.setValue(field, fieldValue(node, field));
	}

	private Object fieldValue(JsonNode node, Field<?> field) {
		BigDecimal value = new BigDecimal(node.textValue());
		if (field instanceof DecimalField) {
			return value;
		} else if (field instanceof DecimalMeasureField) {
			return Measures.valueOf(value, unit);
		} else if (field instanceof IntegerField) {
			return Integer.valueOf(value.intValue());
		} else if (field instanceof PercentageField) {
			return Percentage.valueOf(value.intValue());
		} else if (field instanceof RatingField) {
			return Rating.valueOf(value.intValue());
		} else if (field instanceof DurationField) {
			return durationValue(value, category.getSymbol());
		}
		return null;
	}

	private static Duration durationValue(BigDecimal value, String symbol) {
		long scale;
		if (symbol == null) {
			scale = 1;
		} else if (symbol.startsWith("s")) {
			scale = 1000;
		} else if (symbol.startsWith("m")) {
			scale = 1000 * 60;
		} else if (symbol.startsWith("h")) {
			scale = 1000 * 60 * 60;
		} else {
			return null;
		}
		return Duration.millis(value.multiply(BigDecimal.valueOf(scale)).longValue());
	}

	private Rating ratingValue(JsonNode node) {
		return !isZero(node) ? Rating.valueOf(node.intValue() * 20) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
