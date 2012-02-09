package queries;

import java.util.List;

import org.joda.time.YearMonth;

import models.Event;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

public class BucketResult {

	private final String bucketId;
	private final List<Event> events = Lists.newArrayList();
	private final Multiset<String> tags = LinkedHashMultiset.create();
	private final Multiset<String> ratings = LinkedHashMultiset.create();
	private final Multiset<YearMonth> months = LinkedHashMultiset.create();

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

	public Multiset<String> getTags() {
		return tags;
	}

	public void addTag(String tag, int count) {
		tags.add(tag, count);
	}

	public void addRating(String rating, int count) {
		ratings.add(rating, count);
	}

	public Multiset<String> getRatings() {
		return ratings;
	}

	public void addMonth(YearMonth month, int count) {
		months.add(month, count);
	}

	public Multiset<YearMonth> getMonths() {
		return months;
	}
}
