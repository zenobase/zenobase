package com.zenobase.tasks.netatmo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.joda.time.DateTime;
import com.google.common.collect.Lists;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class NetatmoResult {

	public static final Resource SOURCE = new Resource("Netatmo", "http://www.netatmo.com/");

	private final Identity author;
	private final Device device;
	private final JsonNode node;

	public NetatmoResult(Identity author, Device device, JsonNode node) {
		this.author = author;
		this.device = device;
		this.node = node;
	}

	public String getStatus() {
		return node.path("status").getTextValue();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (Iterator<Map.Entry<String, JsonNode>> i = node.path("body").getFields(); i.hasNext();) {
			events.add(getEvent(i.next()));
		}
		return events;
	}

	public Event getEvent(Map.Entry<String, JsonNode> entry) {
		ArrayNode node = (ArrayNode) entry.getValue();
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, new DateTime(Long.parseLong(entry.getKey()) * 1000, device.getTimestamp().getZone()));
		event.addValue(Event.TAG, device.getLabel().toLowerCase());
		event.setValue(Event.LOCATION, device.getLocation());
		event.setValue(Event.TEMPERATURE, getMeasure(node.get(0), SI.CELSIUS));
		event.setValue(Event.PRESSURE, getMeasure(node.get(1), SI.HECTO(SI.PASCAL)));
		event.setValue(Event.SOUND, getMeasure(node.get(2), NonSI.DECIBEL));
		// TODO humidity (%), co2 (ppm)
		event.setValue(Event.AUTHOR, author);
		event.setValue(Event.SOURCE, SOURCE);
		return event;
	}

	private static <Q extends Quantity> DecimalMeasure<Q> getMeasure(JsonNode node, Unit<Q> unit) {
		return node.isNumber() ? Measures.<Q>valueOf(node.getDecimalValue(), unit) : null;
	}
}
