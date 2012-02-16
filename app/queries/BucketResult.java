package queries;

import java.util.List;
import java.util.Map;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import common.Nodes;

public class BucketResult {

	private final Bucket bucket;
	private final List<Event> events = Lists.newArrayList();
	private final Map<String, Multiset<?>> facets = Maps.newHashMap();
	private int total; 

	public BucketResult(Bucket bucket) {
		this.bucket = bucket;
	}

	public Bucket getBucket() {
		return bucket;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void addEvent(Event event) {
		events.add(event);
	}

	public void addFacet(String name, Multiset<?> facet) {
		facets.put(name, facet);
	}

	public Multiset<?> getFacet(String name) {
		return facets.get(name);
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public ObjectNode toJson() {
		ObjectNode object = Nodes.newObject();
		object.putAll(bucket.toJson());
		object.put("total", total);
		ArrayNode eventsNode = object.putArray("events");
		for (Event event : events) {
			ObjectNode eventNode = event.toJson();
			eventNode.put("id", event.getId());
			eventsNode.add(eventNode);
		}
		for (Map.Entry<String, Multiset<?>> facet : facets.entrySet()) {
			ArrayNode facetNode = object.putArray(facet.getKey());
			for (Multiset.Entry<?> entry : facet.getValue().entrySet()) {
				ObjectNode entryNode = facetNode.addObject();
				entryNode.put("label", entry.getElement().toString());
				entryNode.put("count", entry.getCount());
			}
		}
		return object;
	}
}
