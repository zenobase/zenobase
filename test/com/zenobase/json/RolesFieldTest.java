package com.zenobase.json;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import com.google.common.collect.ImmutableMap;

import com.zenobase.json.RolesField;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class RolesFieldTest extends FieldTestSupport {

	private final Map<Identity, Role> map = ImmutableMap.of(
		new Identity(), Role.OWNER,
		new Identity(), Role.CONTRIBUTOR);

	@Test
	public void test() {
		RolesField field = new RolesField(FIELD_NAME);
		for (Map.Entry<Identity, Role> entry : map.entrySet()) {
			roundtrip(field, entry);
		}
	}

	@Test
	public void testToMap() {
		assertThat(RolesField.toMap(map.entrySet())).isEqualTo(map);
	}
}
