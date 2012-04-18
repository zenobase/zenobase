package com.zenobase.models;

import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.ImmutableSet;
import com.zenobase.schema.DateTimeField;
import com.zenobase.schema.DurationField;
import com.zenobase.schema.Field;
import com.zenobase.schema.IdentityField;
import com.zenobase.schema.LengthField;
import com.zenobase.schema.LocationField;
import com.zenobase.schema.RatingField;
import com.zenobase.schema.ResourceField;
import com.zenobase.schema.Schema;
import com.zenobase.schema.SchemaBuilder;
import com.zenobase.schema.TokenField;

public class Event extends DomainNode {

	public static final String TYPE_NAME = "event";

	public static final TokenField ID = new TokenField("@id", false);
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
		ImmutableSet.<Field<?>>of(ID, VERSION, AUTHOR, TIMESTAMP, DURATION, LOCATION, TAG, RESOURCE, DISTANCE, HEIGHT, RATING);

	public Event(String id) {
		setValue(ID, id);
	}

	public Event(ObjectNode node) {
		super(node);
	}

	public String getId() {
		return getValue(ID);
	}

	@Override
	public <T> void addValue(Field<T> field, T value) {
		super.addValue(field, value);
	}

	@Override
	public <T> void setValue(Field<T> field, T value) {
		super.setValue(field, value);
	}

	@Override
	public <T> boolean contains(Field<T> field) {
		return super.contains(field);
	}

	public void prePersist() {
		for (Field<?> field : FIELDS) {
			if (contains(field)) {
				field.prePersist(toJson());
			}
		}
	}

	public void postLoad() {
		for (Field<?> field : FIELDS) {
			if (contains(field)) {
				field.postLoad(toJson());
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
}
