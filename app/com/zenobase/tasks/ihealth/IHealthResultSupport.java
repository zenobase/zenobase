package com.zenobase.tasks.ihealth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

abstract class IHealthResultSupport {

	static final Resource SOURCE = new Resource("iHealth", "http://ihealthlabs.com/");

	protected final String root;
	protected final JsonNode node;
	protected final Identity author;

	protected IHealthResultSupport(String root, JsonNode node, Identity author) {
		this.root = Preconditions.checkNotNull(root);
		this.node = Preconditions.checkNotNull(node);
		this.author = Preconditions.checkNotNull(author);
	}

	public boolean isSuccess() {
		return !node.path(root).isMissingNode();
	}

	public boolean hasNext() {
		return textValue(node.path("NextPageUrl")) != null;
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

	protected DateTime dateTimeValue(JsonNode node, DateTimeZone zone) {
		Preconditions.checkState(!isZero(node), "expected a node with a time: <%s>", node);
		return new DateTime(node.longValue() * 1000, DateTimeZone.UTC).withZoneRetainFields(zone);
	}

	protected Location locationValue(JsonNode node) {
		BigDecimal lat = node.path("Lat").decimalValue();
		BigDecimal lon = node.path("Lon").decimalValue();
		return isLatLon(lat) || isLatLon(lon) ? new Location(lat, lon) : null;
	}

	private boolean isLatLon(BigDecimal value) {
		return !BigDecimal.ZERO.equals(value) && !BigDecimal.ONE.equals(value.abs());
	}

	protected String textValue(JsonNode node) {
		String value = node.textValue();
		if (value != null) {
			value = value.trim();
		}
		return Strings.emptyToNull(value);
	}

	protected Percentage percentageValue(JsonNode node) {
		return !isZero(node) ? Percentage.valueOf(node.intValue()) : null;
	}

	protected DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue(), Units.BPM) : null;
	}

	protected DecimalMeasure<Energy> energyValue(JsonNode node) {
		return !isZero(node) ? Measures.valueOf(node.decimalValue().setScale(0, RoundingMode.HALF_UP), Units.KCAL) : null;
	}

	protected static boolean isZero(JsonNode node) {
		Preconditions.checkArgument(node.isMissingNode() || node.isNull() || node.isNumber());
		return node.doubleValue() == 0.0;
	}
}
