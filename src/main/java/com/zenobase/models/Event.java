package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.zenobase.common.Generator;
import com.zenobase.json.BitsField;
import com.zenobase.json.ConcentrationField;
import com.zenobase.json.DateTimeRangeField;
import com.zenobase.json.DecimalField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.DurationField;
import com.zenobase.json.EnergyField;
import com.zenobase.json.Field;
import com.zenobase.json.FrequencyField;
import com.zenobase.json.IdentityField;
import com.zenobase.json.IntegerField;
import com.zenobase.json.LengthField;
import com.zenobase.json.LengthPerVolumeField;
import com.zenobase.json.LightField;
import com.zenobase.json.LocationField;
import com.zenobase.json.PaceField;
import com.zenobase.json.PercentageField;
import com.zenobase.json.PressureField;
import com.zenobase.json.RatingField;
import com.zenobase.json.ResourceField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.SoundField;
import com.zenobase.json.TemperatureField;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;
import com.zenobase.json.VelocityField;
import com.zenobase.json.VolumeField;
import com.zenobase.json.WeightField;

public class Event extends DomainNode {

	public static final String TYPE_NAME = "event";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField BUCKET = new TokenField("_bucket");
	public static final IdentityField AUTHOR = new IdentityField("author");
	public static final ResourceField SOURCE = new ResourceField("source");
	public static final DateTimeRangeField TIMESTAMP = new DateTimeRangeField("timestamp");
	public static final DurationField DURATION = new DurationField("duration");
	public static final FrequencyField FREQUENCY = new FrequencyField("frequency");
	public static final VelocityField VELOCITY = new VelocityField("velocity");
	public static final PaceField PACE = new PaceField("pace");
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
	public static final LengthPerVolumeField DISTANCE_PER_VOLUME = new LengthPerVolumeField("distance/volume");
	public static final IntegerField HUMIDITY = new IntegerField("humidity");
	public static final PressureField PRESSURE = new PressureField("pressure");
	public static final SoundField SOUND = new SoundField("sound");
	public static final EnergyField ENERGY = new EnergyField("energy");
	public static final LightField LIGHT = new LightField("light");
	public static final TemperatureField TEMPERATURE = new TemperatureField("temperature");
	public static final RatingField RATING = new RatingField("rating");
	public static final PercentageField PERCENTAGE = new PercentageField("percentage");
	public static final PercentageField MOON = new PercentageField("moon");
	public static final DecimalField CURRENCY = new DecimalField("currency");
	public static final TextField NOTE = new TextField("note");

	public static final ImmutableSet<Field<?>> FIELDS =
		ImmutableSet.of(
			ID, BUCKET, VERSION, AUTHOR, SOURCE, TIMESTAMP, DURATION, FREQUENCY, VELOCITY, PACE, BITS, COUNT,
			LOCATION, TAG, RESOURCE, DISTANCE, HEIGHT, WEIGHT, VOLUME, CONCENTRATION, DISTANCE_PER_VOLUME,
			HUMIDITY, PRESSURE, SOUND, ENERGY, LIGHT, TEMPERATURE, RATING, PERCENTAGE, MOON, CURRENCY, NOTE);

	public static final Schema SCHEMA = buildSchema();

	public Event() {
		this(Generator.id());
	}

	public Event(String id) {
		setValue(ID, id);
	}

	public Event(ObjectNode node) {
		super(node);
	}

	@Override
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
	public <T> ImmutableList<T> getValues(Field<T> field) {
		return super.getValues(field);
	}

	@Override
	public <T> void setValue(Field<T> field, T value) {
		super.setValue(field, value);
	}

	@Override
	public <T> void setValues(Field<T> field, Iterable<T> values) {
		super.setValues(field, values);
	}

	@Override
	public <T> boolean contains(Field<T> field) {
		return super.contains(field);
	}

	public void prePersist(String bucketId) {
		setValue(Event.BUCKET, bucketId);
		for (Field<?> field : FIELDS) {
			if (contains(field)) {
				field.prePersist(toJson());
			}
		}
	}

	public void postPersist() {
		setValue(Event.BUCKET, null);
		for (Field<?> field : FIELDS) {
			if (contains(field)) {
				field.postPersist(toJson());
			}
		}
	}

	public Event copy() {
		return new Event(toJson().deepCopy());
	}

	private static Schema buildSchema() {
		var schema = new SchemaBuilder(TYPE_NAME);
		schema.setRouting(BUCKET.getName());
		for (Field<?> field : FIELDS) {
			schema.add(field);
		}
		return schema.build();
    }
}
