package com.zenobase.models;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

import com.zenobase.common.Generator;
import com.zenobase.oauth.Authorization;

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
		assertThat(bucket.isPermitted(new Authorization(owner), Permission.ALL))
			.as("owner has full access to the bucket").isTrue();
		assertThat(bucket.isPermitted(new Authorization(owner, other, bucket.getId()), Permission.ALL))
			.as("other has full access to the bucket on behalf of the owner").isTrue();
		assertThat(bucket.isPermitted(new Authorization(owner, other, Generator.id()), Permission.ALL))
			.as("other does not have full access to this bucket on behalf of the owner").isFalse();
		assertThat(bucket.isPermitted(new Authorization(friend), Permission.CONTRIBUTE))
			.as("friend can contribute to the bucket").isTrue();
		assertThat(bucket.isPermitted(new Authorization(friend), Permission.USE))
			.as("friend can use the bucket").isTrue();
		assertThat(bucket.isPermitted(new Authorization(friend), Permission.ALL))
			.as("friend does not have full access to the bucket").isFalse();
		assertThat(bucket.isPermitted(new Authorization(other), Permission.USE))
			.as("other can not use the bucket").isFalse();

		bucket.addPermission(Identity.PUBLIC, Permission.USE);
		assertThat(bucket.isPermitted(new Authorization(other), Permission.USE))
			.as("other can use the bucket after it was made public").isTrue();

		assertThat(bucket.getPrincipals(Permission.ALL))
			.as("principals with full access to the bucket").containsOnly(owner);
		assertThat(bucket.getPrincipals(Permission.CONTRIBUTE))
			.as("principals who can contribute to the bucket").containsOnly(friend);
		assertThat(bucket.getPrincipals(Permission.USE))
			.as("principals who can use the bucket").containsOnly(Identity.PUBLIC);
		assertThat(bucket.getPrincipals())
			.as("principals who can access the bucket").containsOnly(owner, friend, Identity.PUBLIC);
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
