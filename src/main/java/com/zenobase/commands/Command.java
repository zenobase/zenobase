package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.zenobase.common.Generator;
import com.zenobase.json.CommandTypeField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.Field;
import com.zenobase.json.IdentityField;
import com.zenobase.json.IntegerField;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class Command extends DomainNode {

	public static final String TYPE_NAME = "command";

	public static final TokenField ID = new TokenField("@id");
	public static final CommandTypeField TYPE = new CommandTypeField("@type");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final ObjectField PARAMETERS = new ObjectField("parameters");
	public static final IntegerField COST = new IntegerField("cost");

	public Command(ObjectNode node) {
		super(node);
	}

	public Command(Command.Type type, Identity principal) {
		this(type, principal, DateTime.now(DateTimeZone.UTC));
	}

	public Command(Command.Type type, Identity principal, DateTime timestamp) {
		setValue(ID, Generator.id());
		setValue(TYPE, type);
		setValue(PRINCIPAL, principal);
		setValue(TIMESTAMP, timestamp);
		setValue(PARAMETERS, Nodes.newObject());
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public Command.Type getType() {
		return Objects.requireNonNull(getValue(TYPE));
	}

	public void setType(Command.Type type) {
		setValue(TYPE, type);
	}

	protected void checkType(Command.Type expected) {
		Preconditions.checkArgument(expected.equals(getType()), "Expected %s but was %s", expected, getType());
	}

	public Identity getPrincipal() {
		return Objects.requireNonNull(getValue(PRINCIPAL));
	}

	public DateTime getTimestamp() {
		return Objects.requireNonNull(getValue(TIMESTAMP));
	}

	protected <T> @Nullable T getParameter(Field<T> field) {
		return field.getValue(Objects.requireNonNull(getValue(PARAMETERS)));
	}

	protected <T> void setParameter(Field<T> field, @Nullable T value) {
		field.setValue(Objects.requireNonNull(getValue(PARAMETERS)), value);
	}

	protected <T> ImmutableList<T> getParameters(Field<T> field) {
		return field.getValues(Objects.requireNonNull(getValue(PARAMETERS)));
	}

	protected <T> void addParameter(Field<T> field, T value) {
		field.addValue(Objects.requireNonNull(getValue(PARAMETERS)), value);
	}

	public int getCost() {
		return getValue(COST, 0);
	}

	protected void setCost(int cost) {
		setValue(COST, cost);
	}

	protected void addCost(int cost) {
		setCost(getCost() + cost);
	}

	public Command reverse(Identity principal) {
		throw new UnsupportedOperationException();
	}

	public boolean isPermitted(Authorization auth) {
		return auth.getScope() == null && getPrincipal().equals(auth.getPrincipal());
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Command c && getId().equals(c.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(ID)
			.add(TYPE)
			.add(PRINCIPAL)
			.add(TIMESTAMP)
			.add(PARAMETERS)
			.add(COST)
			.build();
	}

	public record Type(String name, int version) {
		@Override
		public String toString() {
			return String.format("%s (%s)", name, version);
		}
	}
}
