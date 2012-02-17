package models;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import schema.DateTimeType;
import schema.Field;
import schema.LengthType;
import schema.LocationType;
import schema.RatingType;
import schema.ResourceType;
import schema.SchemaBuilder;
import schema.TokenType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import common.Generator;
import common.Nodes;

public class Event {
	
	public static final String TYPE_NAME = "event";
	public static final Field<Token> TAG = Field.of("tag", new TokenType());
	public static final Field<DateTime> DATE_TIME = Field.of("dateTime", new DateTimeType());
	public static final Field<Location> LOCATION = Field.of("location", new LocationType());
	public static final Field<Length> DISTANCE = Field.of("distance", new LengthType());
	public static final Field<Length> HEIGHT = Field.of("height", new LengthType());
	public static final Field<Resource> RESOURCE = Field.of("resource", new ResourceType());
	public static final Field<Rating> RATING = Field.of("rating", new RatingType());

	private static final ImmutableSet<Field<?>> FIELDS = 
		ImmutableSet.<Field<?>>of(TAG, RESOURCE, DISTANCE, HEIGHT, LOCATION, DATE_TIME, RATING);

	private final String id;
	private final String bucket;
	private final ObjectNode object;

	public Event(String id, String bucket) {
		this.id = id;
		this.bucket = bucket;
		object = Nodes.newObject();
	}

	public Event(String id, String bucket, ObjectNode object) {
		this.id = id;
		this.bucket = bucket;
		this.object = Nodes.copy(object); // TODO validate
	}

	public String getId() {
		return id;
	}

	public String getBucket() {
		return bucket;
	}

	public <T> void add(Field<T> field, T value) {
		field.getType().add(object, field.getName(), value);
	}

	public <T> Iterable<T> get(Field<T> field) {
		return field.getType().get(object, field.getName());
	}

	public <T> boolean contains(Field<T> field) {
		return object.has(field.getName());
	}

	public ObjectNode toJson() {
		return object;
	}

	// TODO memoize
    public static ObjectNode getSchema() {
		SchemaBuilder schema = new SchemaBuilder(TYPE_NAME).index(true);
		for (Field<?> field : FIELDS) {
			schema.add(field);
		}
		return schema.build();
    }

	public Multimap<Field<?>, Object> toMap() {
		Multimap<Field<?>, Object> map = ArrayListMultimap.create();
		for (Field<?> field : FIELDS) {
			map.putAll(field, get(field));
		}
		return map;
	}

	public static Event newEvent(String bucket, ObjectNode content) {
    	return new Event(Generator.id(), bucket, content);
	}

	@Override
	public String toString() {
		return object.toString();
	}
}
