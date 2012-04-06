package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.ImmutableList;

public abstract class Type<T> {

	private final java.lang.reflect.Type type;
	private final String schemaType;

	protected Type(java.lang.reflect.Type type, String schemaType) {
		this.type = type;
		this.schemaType = schemaType;
	}

	public java.lang.reflect.Type getType() {
		return type;
	}

	public ImmutableList<T> getValues(ObjectNode object, String fieldName) {
		ImmutableList.Builder<T> values = ImmutableList.builder();
		JsonNode node = object.get(fieldName);
		if (node != null && node.isArray()) {
			for (JsonNode element : ((ArrayNode) node)) {
				values.add(get(element));
			}
		}
		else if (node != null && !node.isMissingNode() && !node.isNull()) {
			values.add(get(node));
		}
		return values.build();
	}

	protected abstract T get(JsonNode node);

	public void addValue(ObjectNode object, String fieldName, T value) {
		JsonNode node = object.get(fieldName);
		if (node == null) {
			object.put(fieldName, toJson(value));
		}
		else if (node.isArray()) {
			((ArrayNode) node).add(toJson(value));
		}
		else {
			ArrayNode arrayNode = object.putArray(fieldName);
			arrayNode.add(node);
			arrayNode.add(toJson(value));
		}
	}

	public void addValues(ObjectNode object, String fieldName, Iterable<T> values) {
		JsonNode node = object.get(fieldName);
		if (node == null) {
			ArrayNode arrayNode = object.putArray(fieldName);
			addValues(arrayNode, values);
		}
		else if (node.isArray()) {
			addValues((ArrayNode) node, values);
		}
		else {
			ArrayNode arrayNode = object.putArray(fieldName);
			arrayNode.add(node);
			addValues(arrayNode, values);
		}
	}

	public void setValue(ObjectNode object, String fieldName, T value) {
		object.put(fieldName, toJson(value));
	}

	public void setValues(ObjectNode object, String fieldName, Iterable<T> values) {
		ArrayNode arrayNode = object.putArray(fieldName);
		addValues(arrayNode, values);
	}

	private void addValues(ArrayNode node, Iterable<T> values) {
		for (T value : values) {
			node.add(toJson(value));
		}
	}

	protected abstract JsonNode toJson(T value);

	public void configureSchema(ObjectNode schema) {
		schema.put("type", schemaType);
	}

	public void prePersist(ObjectNode object, String fieldName) {
		
	}

	public void postLoad(ObjectNode object, String fieldName) {
		
	}
}
