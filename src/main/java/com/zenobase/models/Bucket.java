package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.zenobase.common.Generator;
import com.zenobase.json.AliasField;
import com.zenobase.json.BooleanField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.ObjectField;
import com.zenobase.json.RolesField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;
import com.zenobase.oauth.Authorization;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class Bucket extends DomainNode {

	public static final String TYPE_NAME = "bucket";

	public static final TokenField ID = new TokenField("@id");
	public static final TokenField LABEL = new TokenField("label", true);
	public static final TextField DESCRIPTION = new TextField("description");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final RolesField ROLES = new RolesField("roles");
	public static final ObjectField WIDGETS = new ObjectField("widgets");
	public static final AliasField ALIASES = new AliasField("aliases");
	public static final BooleanField REFRESH = new BooleanField("refresh");
	public static final BooleanField ARCHIVED = new BooleanField("archived");

	public static final Schema SCHEMA = new SchemaBuilder(TYPE_NAME)
		.add(VERSION)
		.add(ID)
		.add(LABEL)
		.add(DESCRIPTION)
		.add(CREATED)
		.add(ROLES)
		.add(WIDGETS)
		.add(ALIASES)
		.add(REFRESH)
		.add(ARCHIVED)
		.build();

	public Bucket(ObjectNode node) {
		super(node);
	}

	public Bucket() {
		this(Generator.id());
	}

	public Bucket(String id) {
		setValue(ID, id);
		setValue(CREATED, DateTime.now(DateTimeZone.UTC));
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public @Nullable String getLabel() {
		return getValue(LABEL);
	}

	public void setLabel(@Nullable String label) {
		setValue(LABEL, label);
	}

	public @Nullable String getDescription() {
		return getValue(DESCRIPTION);
	}

	public void setDescription(@Nullable String description) {
		setValue(DESCRIPTION, description);
	}

	public @Nullable DateTime getCreated() {
		return getValue(CREATED);
	}

	public ImmutableSet<Identity> getPrincipals(Role role) {
		ImmutableSet.Builder<Identity> principals = ImmutableSet.builder();
		for (Map.Entry<Identity, Role> entry : getValues(ROLES)) {
			if (entry.getValue() == role) {
				principals.add(entry.getKey());
			}
		}
		return principals.build();
	}

	public ImmutableSet<Identity> getPrincipals() {
		ImmutableSet.Builder<Identity> principals = ImmutableSet.builder();
		for (Map.Entry<Identity, Role> entry : getValues(ROLES)) {
			principals.add(entry.getKey());
		}
		return principals.build();
	}

	/**
	 * Authorization.scope can be one of:
	 * <ul>
	 *   <li>{@code null} — first-party token (web UI, personal API token). Reaches the principal's own roles and any
	 *       PUBLIC role.</li>
	 *   <li>A bucket id — legacy bucket-scoped token. Constrains the principal's role check to that bucket. No PUBLIC.
	 *       (Pre-Auth0 concept; not issued anymore but commands carrying it may still replay.)</li>
	 *   <li>The sentinel {@code "external"} — a third-party / MCP token. Reaches the principal's own roles only. The
	 *       PUBLIC branch is deliberately skipped so an external client cannot reach a public bucket owned by another
	 *       user via a unilaterally-issued grant — the per-bucket grant table is intended to scope access to buckets
	 *       the user themselves owns.</li>
	 * </ul>
	 */
	public boolean hasRole(@Nullable Authorization auth, Role role) {
		ImmutableList<Entry<Identity, Role>> roles = getValues(ROLES);
		String scope = auth != null ? auth.getScope() : null;
		boolean isBucketScoped = scope != null && !scope.equals(getId()) && !isExternalScope(scope);
		if (auth != null && !isBucketScoped) {
			for (Map.Entry<Identity, Role> entry : roles) {
				if (entry.getKey().equals(auth.getPrincipal())) {
					return entry.getValue().implies(role);
				}
			}
		}
		// PUBLIC branch is only reachable for anonymous or first-party requests. Bucket-scoped legacy tokens and
		// external/MCP tokens never see PUBLIC roles via this method.
		if (auth == null || scope == null) {
			for (Map.Entry<Identity, Role> entry : roles) {
				if (entry.getKey().equals(Identity.PUBLIC)) {
					return entry.getValue().implies(role);
				}
			}
		}
		return false;
	}

	private static boolean isExternalScope(String scope) {
		// Keep in lockstep with Auth0TokenAuthorizer.EXTERNAL_SCOPE — duplicated as a literal here to avoid a dep
		// from models/ onto auth/. BucketTest.testExternalScope pins the value.
		return "external".equals(scope);
	}

	public void addRole(Identity principal, Role role) {
		addValue(ROLES, Maps.immutableEntry(principal, role));
	}

	public List<ObjectNode> getWidgets() {
		return getValues(WIDGETS);
	}

	public void setWidgets(Iterable<ObjectNode> widgets) {
		setValues(WIDGETS, widgets);
	}

	public List<Alias> getAliases() {
		return getValues(ALIASES);
	}

	public void setAliases(Iterable<Alias> alias) {
		setValues(ALIASES, alias);
	}

	public void addAlias(Alias alias) {
		addValue(ALIASES, alias);
	}

	public boolean isVirtual() {
		return !getAliases().isEmpty();
	}

	public void setRefresh(boolean refresh) {
		setValue(REFRESH, refresh ? Boolean.TRUE : null);
	}

	public void setArchived(boolean archived) {
		setValue(ARCHIVED, archived ? Boolean.TRUE : null);
	}

	public boolean isArchived() {
		return Boolean.TRUE.equals(getValue(ARCHIVED));
	}

	public Bucket copy() {
		return new Bucket(toJson().deepCopy());
	}

	public Bucket sanitize() {
		return new Bucket(SCHEMA.sanitize(toJson()));
	}

	public boolean valid() {
		return !Strings.isNullOrEmpty(getLabel()) && !getPrincipals(Role.OWNER).isEmpty();
	}

	@Override
	public String toString() {
		return MoreObjects.firstNonNull(getLabel(), getId());
	}
}
