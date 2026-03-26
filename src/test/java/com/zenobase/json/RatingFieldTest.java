package com.zenobase.json;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Rating;

public class RatingFieldTest extends FieldTestSupport<Rating> {

	@Override
	protected Field<Rating> newField(String name) {
		return new RatingField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(Rating.valueOf(0));
		roundtrip(Rating.valueOf(50));
		roundtrip(Rating.valueOf(100));
	}
}
