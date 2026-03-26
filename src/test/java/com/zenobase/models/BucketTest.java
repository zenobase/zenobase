package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

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
	public void testAliases() {
		Bucket bucket = new Bucket();
		assertThat(bucket.isVirtual()).isFalse();
		bucket.addAlias(new Alias("foo"));
		assertThat(bucket.isVirtual()).isTrue();
	}

	@Test
	public void testRoles() {

		Identity owner = new Identity();
		Identity friend = new Identity();
		Identity other = new Identity();
		Bucket bucket = new Bucket();

		bucket.addRole(owner, Role.OWNER);
		bucket.addRole(friend, Role.CONTRIBUTOR);
		assertThat(bucket.hasRole(new Authorization(owner), Role.OWNER))
				.as("owner has full access to the bucket")
				.isTrue();
		assertThat(bucket.hasRole(new Authorization(owner, other, bucket.getId()), Role.OWNER))
				.as("other has full access to the bucket on behalf of the owner")
				.isTrue();
		assertThat(bucket.hasRole(new Authorization(owner, other, Generator.id()), Role.OWNER))
				.as("other does not have full access to this bucket on behalf of the owner")
				.isFalse();
		assertThat(bucket.hasRole(new Authorization(friend), Role.CONTRIBUTOR))
				.as("friend can contribute to the bucket")
				.isTrue();
		assertThat(bucket.hasRole(new Authorization(friend), Role.VIEWER))
				.as("friend can use the bucket")
				.isTrue();
		assertThat(bucket.hasRole(new Authorization(friend), Role.OWNER))
				.as("friend does not have full access to the bucket")
				.isFalse();
		assertThat(bucket.hasRole(new Authorization(other), Role.VIEWER))
				.as("other can not use the bucket")
				.isFalse();

		bucket.addRole(Identity.PUBLIC, Role.VIEWER);
		assertThat(bucket.hasRole(new Authorization(other), Role.VIEWER))
				.as("other can use the bucket after it was made public")
				.isTrue();
		assertThat(bucket.hasRole(new Authorization(owner, other, Generator.id()), Role.VIEWER))
				.as("other can not use this bucket on behalf of the owner even if it's public")
				.isFalse();

		assertThat(bucket.getPrincipals(Role.OWNER))
				.as("principals with full access to the bucket")
				.containsOnly(owner);
		assertThat(bucket.getPrincipals(Role.CONTRIBUTOR))
				.as("principals who can contribute to the bucket")
				.containsOnly(friend);
		assertThat(bucket.getPrincipals(Role.VIEWER))
				.as("principals who can use the bucket")
				.containsOnly(Identity.PUBLIC);
		assertThat(bucket.getPrincipals())
				.as("principals who can access the bucket")
				.containsOnly(owner, friend, Identity.PUBLIC);
	}

	@Test
	public void testEqualsHashCode() {
		Bucket b1 = new Bucket();
		b1.setLabel("Bucket #1");
		Bucket b2 = new Bucket();
		b2.setLabel("Bucket #2");
		new EqualsTester().addEqualityGroup(b1, b1.copy()).addEqualityGroup(b2).testEquals();
	}
}
