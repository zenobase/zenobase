package models;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import schema.DateTimeType;
import schema.Field;
import schema.IdentityType;
import schema.LengthType;
import schema.LocationType;
import schema.RatingType;
import schema.ResourceType;
import schema.SchemaBuilder;
import schema.TokenType;
import secure.Identity;

import com.google.common.collect.ImmutableSet;
import common.Generator;
import common.Nodes;

public class Event {

	public static final String TYPE_NAME = "event";
	public static final Field<Identity> CREATOR = Field.of("creator", new IdentityType());
	public static final Field<DateTime> TIMESTAMP = Field.of("timestamp", new DateTimeType());
	public static final Field<Location> LOCATION = Field.of("location", new LocationType());
	public static final Field<Token> TAG = Field.of("tag", new TokenType());
	public static final Field<Resource> RESOURCE = Field.of("resource", new ResourceType());
	public static final Field<Length> DISTANCE = Field.of("distance", new LengthType());
	public static final Field<Length> HEIGHT = Field.of("height", new LengthType());
	public static final Field<Rating> RATING = Field.of("rating", new RatingType());

	private static final ImmutableSet<Field<?>> FIELDS = 
		ImmutableSet.<Field<?>>of(CREATOR, TIMESTAMP, LOCATION, TAG, RESOURCE, DISTANCE, HEIGHT, RATING);

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
		field.getType().add(content, field.getName(), value);
	}

	public <T> Iterable<T> get(Field<T> field) {
		return field.getType().get(content, field.getName());
	}

	public <T> void set(Field<T> field, T value) {
		field.getType().set(content, field.getName(), value);
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

    public static ObjectNode getSchema() {
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
