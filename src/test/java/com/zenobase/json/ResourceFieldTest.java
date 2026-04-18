package com.zenobase.json;

import com.zenobase.models.Resource;
import org.junit.jupiter.api.Test;

public class ResourceFieldTest extends FieldTestSupport<Resource> {

	@Override
	protected Field<Resource> newField(String name) {
		return new ResourceField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(new Resource("Google", "http://www.google.com/"));
	}
}
