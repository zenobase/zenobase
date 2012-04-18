package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.collect.ImmutableList;

import com.zenobase.common.Generator;
import com.zenobase.common.Nodes;
import com.zenobase.models.DomainNode;
import com.zenobase.models.Identity;
import com.zenobase.schema.DateTimeField;
import com.zenobase.schema.Field;
import com.zenobase.schema.IdentityField;
import com.zenobase.schema.ObjectField;
import com.zenobase.schema.Schema;
import com.zenobase.schema.SchemaBuilder;
import com.zenobase.schema.TokenField;

public abstract class CommandSupport extends DomainNode implements Command {

	public static final String TYPE_NAME = "command";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("@type", false);
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final ObjectField PARAMETERS = new ObjectField("parameters");

	public CommandSupport(ObjectNode node) {
		super(node);
	}

	public CommandSupport(String type, Identity principal) {
		setValue(ID, Generator.id());
		setValue(TYPE, type);
		setValue(PRINCIPAL, principal);
		setValue(TIMESTAMP, new DateTime(DateTimeZone.UTC));
		setValue(PARAMETERS, Nodes.newObject());
	}

	@Override
	public String getType() {
		return getValue(TYPE);
	}

	@Override
	public String getId() {
		return getValue(ID);
	}

	@Override
	public Identity getPrincipal() {
		return getValue(PRINCIPAL);
	}

	@Override
	public DateTime getTimestamp() {
		return getValue(TIMESTAMP);
	}

	protected <T> T getParameter(Field<T> field) {
		return field.getValue(getValue(PARAMETERS));
	}

	protected <T> void setParameter(Field<T> field, T value) {
		field.setValue(getValue(PARAMETERS), value);
	}

	protected <T> ImmutableList<T> getParameters(Field<T> field) {
		return field.getValues(getValue(PARAMETERS));
	}

	protected <T> void addParameter(Field<T> field, T value) {
		field.addValue(getValue(PARAMETERS), value);
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(ID).add(TYPE).add(PRINCIPAL)
			.add(TIMESTAMP).add(PARAMETERS).build();
	}
}
