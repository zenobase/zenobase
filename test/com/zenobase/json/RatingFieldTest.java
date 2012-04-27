package com.zenobase.json;

import org.junit.Test;

import com.zenobase.json.RatingField;
import com.zenobase.models.Rating;

public class RatingFieldTest extends FieldTestSupport {

	private final RatingField field = new RatingField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Rating.valueOf(0));
		roundtrip(field, Rating.valueOf(50));
		roundtrip(field, Rating.valueOf(100));
	}
}
