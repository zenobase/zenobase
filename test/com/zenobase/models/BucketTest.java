package com.zenobase.models;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class BucketTest {

	@Test
	public void testLabelAndDescription() {
		String label = "Test Bucket";
		String description = "this is a test";
		Bucket bucket = new Bucket();
		bucket.setLabel(label);
		bucket.setDescription(description);
		assertThat(bucket.getLabel()).isEqualTo(label);
		assertThat(bucket.getDescription()).isEqualTo(description);
	}

	@Test
	public void testPermissions() {
		Identity owner = new Identity();
		Identity friend = new Identity();
		Identity other = new Identity();
		Bucket bucket = new Bucket();
		bucket.addPermission(owner, Permission.ALL);
		bucket.addPermission(friend, Permission.CONTRIBUTE);
		assertThat(bucket.getPermission(owner)).isEqualTo(Permission.ALL);
		assertThat(bucket.getPermission(friend)).isEqualTo(Permission.CONTRIBUTE);
		assertThat(bucket.getPermission(other)).isEqualTo(Permission.NONE);
		assertThat(bucket.getPrincipals(Permission.ALL)).containsOnly(owner);
		assertThat(bucket.getPrincipals(Permission.CONTRIBUTE)).containsOnly(friend);
	}

	@Test
	public void testEqualsHashCode() {
		Bucket b1 = new Bucket();
		b1.setLabel("Bucket #1");
		Bucket b2 = new Bucket();
		b2.setLabel("Bucket #2");
		new EqualsTester()
			.addEqualityGroup(b1, b1.copy())
			.addEqualityGroup(b2).testEquals();
	}
}
