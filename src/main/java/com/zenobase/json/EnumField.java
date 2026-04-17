package com.zenobase.json;

import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.search.constraints.TermConstraintBuilder;

public class EnumField<E extends Enum<E>> extends Field<E> {

	public static <T extends Enum<T>> EnumField<T> newInstance(String name, Class<T> type) {
		return new EnumField<>(name, type);
	}

	private EnumField(String name, Class<E> type) {
		super(name, type, "keyword");
		addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
	}

	@Override
	@SuppressWarnings("unchecked")
	protected @Nullable E getValue(JsonNode node) {
		String value = node.textValue();
		return value != null ? Enum.valueOf((Class<E>) getType(), value.toUpperCase(Locale.ROOT)) : null;
	}

	@Override
	public JsonNode toJson(@Nullable E value) {
		return value != null ? new TextNode(value.toString().toLowerCase(Locale.ROOT)) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
	}
}
