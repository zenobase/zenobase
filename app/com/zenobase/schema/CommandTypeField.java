package com.zenobase.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.common.Nodes;
import com.zenobase.models.Resource;

public class CommandTypeField extends Field<Command.Type> {

	private static final TokenField NAME = new TokenField("name");
	private static final IntegerField VERSION = new IntegerField("version");

	public CommandTypeField(String name) {
		super(name, Resource.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, NAME);
		configureSchema(properties, VERSION);
	}

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected Command.Type getValue(JsonNode node) {
		return new Command.Type(NAME.getValue((ObjectNode) node), VERSION.getValue((ObjectNode) node));
	}

	@Override
	protected JsonNode toJson(Command.Type value) {
		ObjectNode node = Nodes.newObject();
		NAME.setValue(node, value.getName());
		VERSION.setValue(node, value.getVersion());
		return node;
	}
}
