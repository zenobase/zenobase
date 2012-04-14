package commands;

import models.DomainNode;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import schema.DateTimeField;
import schema.Field;
import schema.IdentityField;
import schema.ObjectField;
import schema.TokenField;

import com.google.common.collect.ImmutableList;
import common.Generator;
import common.Nodes;

public abstract class CommandSupport extends DomainNode implements Command {

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("@type", false);
	public static final IdentityField IDENTITY = new IdentityField("identity");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final ObjectField PARAMETERS = new ObjectField("parameters");

	public CommandSupport(ObjectNode object) {
		super(object);
	}

	public CommandSupport(String type, Identity identity) {
		setValue(ID, Generator.id());
		setValue(TYPE, type);
		setValue(IDENTITY, identity);
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
	public Identity getIdentity() {
		return getValue(IDENTITY);
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
}
