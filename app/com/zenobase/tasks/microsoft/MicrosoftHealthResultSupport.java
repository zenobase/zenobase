package com.zenobase.tasks.microsoft;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Energy;
import javax.measure.quantity.Frequency;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Percentage;
import com.zenobase.models.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

abstract class MicrosoftHealthResultSupport {

	static final Resource SOURCE = new Resource("Microsoft Health", "https://www.microsoft.com/microsoft-health/");

	protected final JsonNode node;
	protected final Identity author;
	protected final DateTimeZone zone;

	public MicrosoftHealthResultSupport(JsonNode node, Identity author, DateTimeZone zone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.zone = zone;
	}

	public String next() {
		return node.path("nextPage").textValue();
	}

	protected DateTime dateTimeValue(JsonNode node) {
		return DateTime.parse(node.textValue()).withZone(zone);
	}

	protected Integer countValue(JsonNode node) {
		return node.isNumber() ? node.intValue() : null;
	}

	protected DecimalMeasure<Frequency> frequencyValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(Measures.round(node.decimalValue(), 0), Units.BPM) : null;
	}

	protected DecimalMeasure<Energy> energyValue(JsonNode node) {
		return node.isNumber() ? Measures.valueOf(Measures.round(node.decimalValue(), 0), Units.KCAL) : null;
	}

	protected Percentage percentageValue(JsonNode node) {
		return node.isNumber() ? Percentage.valueOf(node.intValue()) : null;
	}

	protected static Location locationValue(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		Preconditions.checkState(node.path("latitude").isNumber(), "expected a numeric latitude in <%s>", node);
		Preconditions.checkState(node.path("longitude").isNumber(), "expected a numeric longitude in <%s>", node);
		return new Location(node.path("latitude").decimalValue(), node.path("longitude").decimalValue());
	}
}
