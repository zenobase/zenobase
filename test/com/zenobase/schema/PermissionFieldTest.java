package com.zenobase.schema;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import com.google.common.collect.ImmutableMap;

import com.zenobase.models.Identity;
import com.zenobase.models.Permission;

public class PermissionFieldTest extends FieldTestSupport {

	private final Map<Identity, Permission> map = ImmutableMap.of(
		new Identity(), Permission.ALL,
		new Identity(), Permission.CONTRIBUTE);

	@Test
	public void test() {
		PermissionField field = new PermissionField(FIELD_NAME);
		for (Map.Entry<Identity, Permission> entry : map.entrySet()) {
			roundtrip(field, entry);
		}
	}

	@Test
	public void testToMap() {
		assertThat(PermissionField.toMap(map.entrySet())).isEqualTo(map);
	}
}
