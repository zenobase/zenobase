package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * A user's grant of access to one of their buckets to an external client (e.g. an MCP client like Claude Desktop, or
 * a third-party REST integration). One row per {@code (user, client, bucket)} tuple. Mutations flow through
 * {@code CreateExternalBucketGrantCommand} / {@code DeleteExternalBucketGrantCommand} so the audit trail lives in
 * the command log — no {@code revoked_at} column; revoke == delete.
 */
public class ExternalBucketGrant extends DomainNode {

	public static final String TYPE_NAME = "external_bucket_grant";

	public static final TokenField ID = new TokenField("@id");
	public static final IdentityField USER = new IdentityField("user");
	public static final IdentityField CLIENT = new IdentityField("client");
	public static final TokenField BUCKET = new TokenField("bucket");
	public static final TokenField RIGHTS = new TokenField("rights");
	public static final DateTimeField CREATED = new DateTimeField("created");

	public static final String RIGHT_READ = "read";

	public static final Schema SCHEMA = new SchemaBuilder(TYPE_NAME)
		.add(VERSION)
		.add(ID)
		.add(USER)
		.add(CLIENT)
		.add(BUCKET)
		.add(RIGHTS)
		.add(CREATED)
		.build();

	public ExternalBucketGrant(ObjectNode node) {
		super(node);
	}

	public ExternalBucketGrant(Identity user, Identity client, String bucketId, String rights) {
		this(user, client, bucketId, rights, DateTime.now(DateTimeZone.UTC));
	}

	public ExternalBucketGrant(Identity user, Identity client, String bucketId, String rights, DateTime created) {
		setValue(ID, id(user, client, bucketId));
		setValue(USER, user);
		setValue(CLIENT, client);
		setValue(BUCKET, bucketId);
		setValue(RIGHTS, rights);
		setValue(CREATED, created);
	}

	public static String id(Identity user, Identity client, String bucketId) {
		return user.id() + "|" + client.id() + "|" + bucketId;
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public Identity getUser() {
		return Objects.requireNonNull(getValue(USER));
	}

	public Identity getClient() {
		return Objects.requireNonNull(getValue(CLIENT));
	}

	public String getBucketId() {
		return Objects.requireNonNull(getValue(BUCKET));
	}

	public String getRights() {
		return Objects.requireNonNull(getValue(RIGHTS));
	}

	public DateTime getCreated() {
		return Objects.requireNonNull(getValue(CREATED));
	}
}
