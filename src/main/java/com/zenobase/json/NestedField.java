package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.search.constraints.ConstraintBuilder;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class NestedField<T> extends Field<T> {

	private final Field<?> parent;
	private final Field<T> field;

	public NestedField(Field<?> parent, Field<T> field) {
		super(field.getName(), field.getType(), field.getSchemaType());
		this.parent = parent;
		this.field = field;
	}

	@Override
	public String getPath() {
		return concat(parent.getPath(), field.getName());
	}

	public String getPath(String parent) {
		return concat(parent, field.getName());
	}

	@Override
	public @Nullable T getValue(JsonNode node) {
		return field.getValue(node);
	}

	@Override
	public JsonNode toJson(@Nullable T value) {
		return field.toJson(value);
	}

	@Override
	public void createSchema(ObjectNode schema) {
		field.createSchema(schema);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		field.configureSchema(schema);
	}

	// TODO verify that this results in queries with the correct exact path
	public void addConstraintBuilders(String path, Field<?> target) {
		for (Map.Entry<String, ConstraintBuilder> entry : field.getConstraintBuilders().entries()) {
			target.addConstraintBuilder(concat(path, entry.getKey()), entry.getValue());
		}
	}

	@Override
	public void prePersist(ObjectNode node) {
		field.prePersist(node);
	}

	@Override
	public void postPersist(ObjectNode node) {
		field.postPersist(node);
	}

	@Override
	public String toString() {
		return getPath();
	}
}
