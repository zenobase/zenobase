package com.zenobase.json;

import org.junit.Test;

import com.zenobase.models.Resource;

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
