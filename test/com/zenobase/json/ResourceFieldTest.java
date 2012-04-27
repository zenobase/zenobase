package com.zenobase.json;

import org.junit.Test;

import com.zenobase.json.ResourceField;
import com.zenobase.models.Resource;

public class ResourceFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new ResourceField(FIELD_NAME), new Resource("Google", "http://www.google.com/"));
	}
}
