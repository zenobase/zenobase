package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.EqualsTester;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.common.Generator;
import com.zenobase.oauth.Authorization;
import org.junit.jupiter.api.Test;

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
		assertThat(bucket.hasRole(new Authorization(friend), Role.VIEWER)).as("friend can use the bucket").isTrue();
		assertThat(bucket.hasRole(new Authorization(friend), Role.OWNER))
			.as("friend does not have full access to the bucket")
			.isFalse();
		assertThat(bucket.hasRole(new Authorization(other), Role.VIEWER)).as("other can not use the bucket").isFalse();

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
	public void testExternalScope() {
		// Bucket.hasRole pattern-matches the external scope as a literal "external" to avoid a dependency from
		// models/ onto auth/. This test pins the lockstep so a future rename of EXTERNAL_SCOPE doesn't silently
		// break the role check.
		assertThat(Auth0TokenAuthorizer.EXTERNAL_SCOPE).isEqualTo("external");

		Identity owner = new Identity();
		Identity client = new Identity();
		Identity stranger = new Identity();
		String external = Auth0TokenAuthorizer.EXTERNAL_SCOPE;

		// Owner's own bucket: external token grants principal-role access.
		Bucket owned = new Bucket();
		owned.addRole(owner, Role.OWNER);
		assertThat(owned.hasRole(new Authorization(owner, client, external), Role.VIEWER))
			.as("external token reaches the principal's own bucket")
			.isTrue();
		assertThat(owned.hasRole(new Authorization(owner, client, external), Role.OWNER))
			.as("external token gets the principal's full role on their own bucket")
			.isTrue();

		// Another user's public bucket: external token must NOT reach via the PUBLIC branch. This is the security
		// boundary that distinguishes external clients from first-party tokens (which would pass).
		Bucket publicOther = new Bucket();
		publicOther.addRole(stranger, Role.OWNER);
		publicOther.addRole(Identity.PUBLIC, Role.VIEWER);
		assertThat(publicOther.hasRole(new Authorization(owner, client, external), Role.VIEWER))
			.as("external token does NOT reach another user's public bucket via the PUBLIC role")
			.isFalse();
		// Sanity: a first-party token from the same user CAN reach the same public bucket.
		assertThat(publicOther.hasRole(new Authorization(owner), Role.VIEWER))
			.as("first-party token still reaches another user's public bucket")
			.isTrue();
		// Sanity: an anonymous request still reaches it.
		assertThat(publicOther.hasRole(null, Role.VIEWER)).as("anonymous still reaches public bucket").isTrue();

		// Private bucket of another user: external token does not reach it.
		Bucket privateOther = new Bucket();
		privateOther.addRole(stranger, Role.OWNER);
		assertThat(privateOther.hasRole(new Authorization(owner, client, external), Role.VIEWER))
			.as("external token does not reach another user's private bucket")
			.isFalse();
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
