package com.zenobase.models;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class IdentityTest {

	@Test
	public void testEqualsHashCode() {
		Identity me = new Identity();
		Identity you = new Identity();
		new EqualsTester()
			.addEqualityGroup(me, new Identity(me.getId()))
			.addEqualityGroup(you).testEquals();
	}

	@Test
	public void testToJson() {
		Identity me = new Identity();
		assertThat(me.toJson()).path(User.ID.getName()).isNotNull();
	}
}
