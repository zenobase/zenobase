package com.zenobase.models;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import com.zenobase.common.Generator;
import com.zenobase.json.BitsField;
import com.zenobase.json.ConcentrationField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.DurationField;
import com.zenobase.json.EnergyField;
import com.zenobase.json.Field;
import com.zenobase.json.FrequencyField;
import com.zenobase.json.IdentityField;
import com.zenobase.json.IntegerField;
import com.zenobase.json.LengthField;
import com.zenobase.json.LocationField;
import com.zenobase.json.Nodes;
import com.zenobase.json.PressureField;
import com.zenobase.json.RatingField;
import com.zenobase.json.ResourceField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TemperatureField;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;
import com.zenobase.json.VelocityField;
import com.zenobase.json.VolumeField;
import com.zenobase.json.WeightField;

public class Event extends DomainNode {

	public static final String TYPE_NAME = "event";

	public static final TokenField ID = new TokenField("@id", false);
	public static final IdentityField AUTHOR = new IdentityField("author");
	public static final ResourceField SOURCE = new ResourceField("source");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final DurationField DURATION = new DurationField("duration");
	public static final FrequencyField FREQUENCY = new FrequencyField("frequency");
	public static final VelocityField VELOCITY = new VelocityField("velocity");
	public static final BitsField BITS = new BitsField("bits");
	public static final IntegerField COUNT = new IntegerField("count");
	public static final LocationField LOCATION = new LocationField("location");
	public static final TokenField TAG = new TokenField("tag");
	public static final ResourceField RESOURCE = new ResourceField("resource");
	public static final LengthField DISTANCE = new LengthField("distance");
	public static final LengthField HEIGHT = new LengthField("height");
	public static final WeightField WEIGHT = new WeightField("weight");
	public static final VolumeField VOLUME = new VolumeField("volume");
	public static final ConcentrationField CONCENTRATION = new ConcentrationField("concentration");
	public static final PressureField PRESSURE = new PressureField("pressure");
	public static final EnergyField ENERGY = new EnergyField("energy");
	public static final TemperatureField TEMPERATURE = new TemperatureField("temperature");
	public static final RatingField RATING = new RatingField("rating");
	public static final TextField NOTE = new TextField("note");

	private static final ImmutableSet<Field<?>> FIELDS =
		ImmutableSet.<Field<?>>of(ID, VERSION, AUTHOR, SOURCE, TIMESTAMP, DURATION, FREQUENCY, VELOCITY, BITS, COUNT, LOCATION, TAG, RESOURCE, DISTANCE, HEIGHT, WEIGHT, VOLUME, CONCENTRATION, PRESSURE, ENERGY, TEMPERATURE, RATING, NOTE);

	public Event() {
		this(Generator.id());
	}

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
	public <T> T getValue(Field<T> field) {
		return super.getValue(field);
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

	public Event copy() {
		return new Event(Nodes.copy(toJson()));
	}

	public static Schema getSchema() {
		SchemaBuilder schema = new SchemaBuilder(TYPE_NAME);
		for (Field<?> field : FIELDS) {
			schema.add(field);
		}
		return schema.build();
    }

	// TODO remove after migration
	public static Event migrate(ObjectNode node) {
		Event event = new Event(node);
		List<DateTime> timestamps = event.getValues(Event.TIMESTAMP);
		if (timestamps.size() == 1) {
			event.setValue(Event.TIMESTAMP, Iterables.getOnlyElement(timestamps));
		} else {
			event.setValue(Event.TIMESTAMP, null);
			for (DateTime timestamp : timestamps) {
				event.addValue(Event.TIMESTAMP, timestamp);
			}
		}
		return event;
	}
}
