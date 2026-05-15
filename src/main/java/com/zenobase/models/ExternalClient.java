package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;
import java.util.Objects;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

/**
 * A third-party client (MCP app, REST integration) that a user has connected and may have granted access to one or
 * more of their buckets. One row per {@code (user, client_id)} pair.
 *
 * <p>All state is reflected in {@code CreateExternalClientCommand} (first observation) and
 * {@code UpdateExternalClientGrantsCommand} (grant changes), so the index can be rebuilt from the command log.
 * {@code created} comes from the creation command's timestamp; there is no {@code last_used} because tracking
 * it would either flood the command journal or get lost on rebuild.
 *
 * <p>{@link #READABLE_BUCKETS} is a multi-valued keyword field carrying the bucket ids the user has granted this
 * client read access to. A future {@code writable_buckets} field can be added without a data migration.
 */
public class ExternalClient extends DomainNode {

	public static final String TYPE_NAME = "external_client";

	public static final TokenField ID = new TokenField("@id");
	public static final IdentityField USER = new IdentityField("user");
	public static final IdentityField CLIENT = new IdentityField("client");
	public static final TextField NAME = new TextField("name");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField READABLE_BUCKETS = new TokenField("readable_buckets");

	public static final Schema SCHEMA = new SchemaBuilder(TYPE_NAME)
		.add(VERSION)
		.add(ID)
		.add(USER)
		.add(CLIENT)
		.add(NAME)
		.add(CREATED)
		.add(READABLE_BUCKETS)
		.build();

	public ExternalClient(ObjectNode node) {
		super(node);
	}

	public ExternalClient(Identity user, Identity client, @Nullable String name, DateTime created) {
		setValue(ID, id(user, client));
		setValue(USER, user);
		setValue(CLIENT, client);
		setValue(NAME, name);
		setValue(CREATED, created);
	}

	public static String id(Identity user, Identity client) {
		return user.id() + "|" + client.id();
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

	public @Nullable String getName() {
		return getValue(NAME);
	}

	public void setName(@Nullable String name) {
		setValue(NAME, name);
	}

	public DateTime getCreated() {
		return Objects.requireNonNull(getValue(CREATED));
	}

	public ImmutableList<String> getReadableBuckets() {
		return getValues(READABLE_BUCKETS);
	}

	public void setReadableBuckets(Iterable<String> bucketIds) {
		setValues(READABLE_BUCKETS, bucketIds);
	}

	public boolean canRead(String bucketId) {
		for (String granted : getReadableBuckets()) {
			if (granted.equals(bucketId)) {
				return true;
			}
		}
		return false;
	}
}
