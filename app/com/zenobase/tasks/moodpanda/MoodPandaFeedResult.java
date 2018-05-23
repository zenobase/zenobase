package com.zenobase.tasks.moodpanda;

import java.util.List;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;
import com.zenobase.tasks.XmlResultSupport;

class MoodPandaFeedResult extends XmlResultSupport {

	public static final Resource SOURCE = new Resource("MoodPanda", "https://moodpanda.com/");

	private final Identity author;
	private final double offset;
	private final String tag;

	public MoodPandaFeedResult(Document document, Identity author, double offset, String tag) {
		super(document);
		this.author = author;
		this.offset = offset;
		this.tag = tag;
	}

	public List<Event> getEvents(DateTime from) {
		List<Event> events = Lists.newArrayList();
		NodeList moodNodes = selectNodes("/Feed/MoodRating");
		for (int i = 0; i < moodNodes.getLength(); ++i) {
			Event event = newEvent(moodNodes.item(i));
			if (event.getValue(Event.TIMESTAMP).isAfter(from)) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(Node node) {
		Event event = new Event();
		event.addValue(Event.TAG, tag);
		event.setValue(Event.NOTE, selectText("Reason", node));
		event.setValue(Event.RATING, Rating.valueOf(Integer.parseInt(selectText("Rating", node)) * 10));
		event.setValue(Event.TIMESTAMP, selectDateTime("Date", node));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private DateTime selectDateTime(String path, Object node) {
		DateTime t = DateTime.parse(selectText(path, node));
		DateTimeZone zone = DateTimeZone.forOffsetMillis(t.getZone().getOffset(t) + (int) (offset * 60 * 60 * 1000));
		return t.withZoneRetainFields(zone);
	}
}
