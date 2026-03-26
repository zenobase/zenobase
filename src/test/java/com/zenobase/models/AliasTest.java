package com.zenobase.models;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Generator;

public class AliasTest {

	@Test
	public void testEqualsHashCode() {
		String b1 = Generator.id();
		String b2 = Generator.id();
		new EqualsTester()
				.addEqualityGroup(new Alias(b1), new Alias(b1))
				.addEqualityGroup(new Alias(b2), new Alias(b2))
				.addEqualityGroup(new Alias(b2, "tag:foo"), new Alias(b2, "tag:foo"))
				.addEqualityGroup(new Alias(b2, "tag:bar"))
				.testEquals();
	}
}
