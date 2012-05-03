package com.zenobase.testing;

import org.fest.assertions.Assertions;
import org.fest.assertions.GenericAssert;

import com.zenobase.json.Field;
import com.zenobase.models.Event;

public class EventAssert extends GenericAssert<EventAssert, Event> {

	private EventAssert(Event actual) {
		super(EventAssert.class, actual);
	}

	public static EventAssert assertThat(Event actual) {
		return new EventAssert(actual);
	}

	public EventAssert hasField(Field<?> field) {
		Assertions.assertThat(actual.contains(field)).as("contains field " + field.getName()).isTrue();
		return this;
	}

	public <T> EventAssert hasValue(Field<T> field, T expected) {
		Assertions.assertThat(actual.getValue(field)).as("value of field " + field.getName()).isEqualTo(expected);
		return this;
	}
}
