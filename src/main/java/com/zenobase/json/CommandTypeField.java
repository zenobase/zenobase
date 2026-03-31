package com.zenobase.json;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

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
		return new Command.Type(
				Objects.requireNonNull(NAME.getValue((ObjectNode) node)),
				Objects.requireNonNull(VERSION.getValue((ObjectNode) node)));
	}

	@Override
	public JsonNode toJson(Command.@Nullable Type value) {
		Preconditions.checkNotNull(value);
		ObjectNode node = Nodes.newObject();
		NAME.setValue(node, value.name());
		VERSION.setValue(node, value.version());
		return node;
	}
}
