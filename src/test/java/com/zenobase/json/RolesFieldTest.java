package com.zenobase.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;

import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class RolesFieldTest extends FieldTestSupport<Map.Entry<Identity, Role>> {

	private final Map<Identity, Role> map = Map.of(
			new Identity(), Role.OWNER,
			new Identity(), Role.CONTRIBUTOR);

	@Override
	protected Field<Map.Entry<Identity, Role>> newField(String name) {
		return new RolesField(name);
	}

	@Test
	public void test() {
		assertThat(MapField.toMap(map.entrySet())).isEqualTo(map);
		for (Map.Entry<Identity, Role> entry : map.entrySet()) {
			roundtrip(entry);
		}
		roundtrip(null);
	}
}
