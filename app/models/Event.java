package models;

import org.codehaus.jackson.node.ObjectNode;

import schema.DateTimeField;
import schema.DurationField;
import schema.IdentityField;
import schema.LengthField;
import schema.LocationField;
import schema.RatingField;
import schema.ResourceField;
import schema.Schema;
import schema.SchemaBuilder;
import schema.TokenField;
import schema.Field;

import com.google.common.collect.ImmutableSet;
import common.Generator;
import common.Nodes;

public class Event {

	public static final String TYPE_NAME = "event";
	public static final IdentityField AUTHOR = new IdentityField("author");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final DurationField DURATION = new DurationField("duration");
	public static final LocationField LOCATION = new LocationField("location");
	public static final TokenField TAG = new TokenField("tag");
	public static final ResourceField RESOURCE = new ResourceField("resource");
	public static final LengthField DISTANCE = new LengthField("distance");
	public static final LengthField HEIGHT = new LengthField("height");
	public static final RatingField RATING = new RatingField("rating");

	private static final ImmutableSet<Field<?>> FIELDS = 
		ImmutableSet.<Field<?>>of(AUTHOR, TIMESTAMP, DURATION, LOCATION, TAG, RESOURCE, DISTANCE, HEIGHT, RATING);

	private final String id;
	private final String bucket;
	private final ObjectNode content;

	public Event(String id, String bucket) {
		this.id = id;
		this.bucket = bucket;
		content = Nodes.newObject();
	}

	public Event(String id, String bucket, ObjectNode content) {
		this.id = id;
		this.bucket = bucket;
		this.content = Nodes.copy(content);
	}

	public String getId() {
		return id;
	}

	public String getBucket() {
		return bucket;
	}

	public <T> void add(Field<T> field, T value) {
		field.addValue(content, value);
	}

	public <T> Iterable<T> get(Field<T> field) {
		return field.getValues(content);
	}

	public <T> void set(Field<T> field, T value) {
		field.setValue(content, value);
	}

	public <T> boolean contains(Field<T> field) {
		return content.has(field.getName());
	}

	public ObjectNode getContent() {
		return content;
	}

	public ObjectNode toJson() {
		ObjectNode object = Nodes.newObject();
		object.putAll(content);
		object.put("@id", id);
		object.put("bucket", bucket);
		return object;
	}

	public void prePersist() {
		for (Field<?> field : FIELDS) {
			if (contains(field)) {
				field.prePersist(content);
			}
		}
	}

	public void postLoad() {
		for (Field<?> field : FIELDS) {
			if (contains(field)) {
				field.postLoad(content);
			}
		}
	}

	public static Schema getSchema() {
		SchemaBuilder schema = new SchemaBuilder(TYPE_NAME);
		for (Field<?> field : FIELDS) {
			schema.add(field);
		}
		return schema.build();
    }

	public static Event newEvent(String bucket, ObjectNode content) {
    	return new Event(Generator.id(), bucket, content);
	}

	@Override
	public String toString() {
		return content.toString();
	}
}
