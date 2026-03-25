package com.zenobase.tasks.goodreads;

import java.util.List;
import java.util.ArrayList;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Rating;
import com.zenobase.models.Resource;
import com.zenobase.tasks.XmlResultSupport;

class GoodreadsReviewListResult extends XmlResultSupport {

	public static final Resource SOURCE = new Resource("Goodreads", "https://www.goodreads.com/");

	private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
		.appendDayOfWeekShortText()
		.appendLiteral(' ')
		.appendMonthOfYearShortText()
		.appendLiteral(' ')
		.appendDayOfMonth(2)
		.appendLiteral(' ')
		.appendHourOfDay(2)
		.appendLiteral(':')
		.appendMinuteOfHour(2)
		.appendLiteral(':')
		.appendSecondOfMinute(2)
		.appendLiteral(' ')
		.appendTimeZoneOffset(null, false, 2, 2)
		.appendLiteral(' ')
		.appendYearOfEra(4, 4)
		.toFormatter()
		.withOffsetParsed();

	private final Identity author;
	private final String tag;

	public GoodreadsReviewListResult(Document document, Identity author, String tag) {
		super(document);
		this.author = author;
		this.tag = tag;
	}

	public int getStartPage() {
		return Integer.parseInt(selectText("/GoodreadsResponse/reviews/@start"));
	}

	public int getEndPage() {
		return Integer.parseInt(selectText("/GoodreadsResponse/reviews/@end"));
	}

	public List<Event> getEvents(DateTime from) {
		List<Event> events = new ArrayList<>();
		NodeList reviewNodes = selectNodes("/GoodreadsResponse/reviews/review");
		for (int i = 0; i < reviewNodes.getLength(); ++i) {
			Event event = newEvent(reviewNodes.item(i), from);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(Node node, DateTime from) {
		DateTime begin = selectDateTime("started_at", node);
		DateTime end = selectDateTime("read_at", node);
		if (begin == null || end == null || from != null && !end.isAfter(from)) {
			return null;
		}
		var event = new Event();
		event.setValues(Event.TIMESTAMP, List.of(begin, end));
		event.setValue(Event.DURATION, new Duration(begin, end));
		event.addValue(Event.TAG, tag);
		event.setValue(Event.RATING, selectRating("rating", node));
		event.setValue(Event.COUNT, selectInteger("book/num_pages", node));
		String title = MoreObjects.firstNonNull(selectText("book/title", node), selectText("id", node));
		event.setValue(Event.RESOURCE, new Resource(title, selectText("url", node)));
		event.setValue(Event.SOURCE, SOURCE);
		event.setValue(Event.AUTHOR, author);
		return event;
	}

	private Rating selectRating(String path, Node node) {
		Integer value = selectInteger(path, node);
		return value != null && value > 0 ? Rating.valueOf(value * 20) : null;
	}

	private Integer selectInteger(String path, Node node) {
		String value = selectText(path, node);
		return value != null ? Integer.parseInt(value) : null;
	}

	/**
	 * Parses dates like <pre>Sun Apr 01 20:57:22 -0700 2018</pre>
	 */
	private DateTime selectDateTime(String path, Node node) {
		String value = selectText(path, node);
		return value != null ? DateTime.parse(value, DATE_FORMAT) : null;
	}
}
