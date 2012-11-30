package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;

public class CommandTypeField extends Field<Command.Type> {

	private static final TokenField NAME = new TokenField("name");
	private static final IntegerField VERSION = new IntegerField("version");

	public CommandTypeField(String name) {
		super(name, Command.Type.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, NAME);
		configureSchema(properties, VERSION);
	}

	@Override
	protected Command.Type getValue(JsonNode node) {
		return new Command.Type(NAME.getValue((ObjectNode) node), VERSION.getValue((ObjectNode) node));
	}

	@Override
	public JsonNode toJson(Command.Type value) {
		Preconditions.checkNotNull(value);
		ObjectNode node = Nodes.newObject();
		NAME.setValue(node, value.getName());
		VERSION.setValue(node, value.getVersion());
		return node;
	}
}
