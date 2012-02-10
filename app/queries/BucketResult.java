package queries;

import java.util.List;
import java.util.Map;

import models.Event;

import org.elasticsearch.common.collect.Maps;

import com.google.common.collect.Lists;

public class BucketResult {

	private final String bucketId;
	private final List<Event> events = Lists.newArrayList();
	private final Map<String, Iterable<?>> results = Maps.newHashMap();

	public BucketResult(String bucketId) {
		this.bucketId = bucketId;
	}

	public String getBucketId() {
		return bucketId;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void addEvent(Event event) {
		events.add(event);
	}

	public void addResult(String widget, Iterable<?> result) {
		results.put(widget, result);
	}

	public Iterable<?> getResult(String widget) {
		return results.get(widget);
	}
}
