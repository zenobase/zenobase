package com.zenobase.tasks.misfit;

import java.math.RoundingMode;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

abstract class MisfitResultSupport {

	static final Resource SOURCE = new Resource("Misfit", "http://misfit.com/");

	protected final String root;
	protected final JsonNode node;
	protected final Identity author;

	protected MisfitResultSupport(String root, JsonNode node, Identity author) {
		this.root = Preconditions.checkNotNull(root);
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode sessionNode : node.path(root)) {
			Event event = newEvent(sessionNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	protected abstract Event newEvent(JsonNode node);

	protected DateTime dateTimeValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue());
	}

	protected LocalDate dateValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a date: <%s>", node);
		return LocalDate.parse(node.textValue());
	}

	protected Duration durationValue(JsonNode node) {
		return !isZero(node) ? Duration.standardSeconds(node.intValue()) : null;
	}

	protected Integer intValue(JsonNode node) {
		return !isZero(node) ? node.intValue() : null;
	}

	protected DecimalMeasure<Length> distanceValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), Units.MI) : null;
	}

	protected DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue().setScale(0, RoundingMode.HALF_UP), Units.KCAL) : null;
	}

	private static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
