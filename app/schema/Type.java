package schema;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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

	public ImmutableList<T> get(ObjectNode object, String fieldName) {
		List<T> values = Lists.newArrayList();
		JsonNode node = object.get(fieldName);
		if (node != null && node.isArray()) {
			for (JsonNode element : ((ArrayNode) node)) {
				values.add(get(element));
			}
		}
		else if (node != null && !node.isMissingNode() && !node.isNull()) {
			values.add(get(node));
		}
		return ImmutableList.copyOf(values);
	}

	protected abstract T get(JsonNode node);

	public void add(ObjectNode object, String fieldName, T value) {
		JsonNode node = object.get(fieldName);
		if (node == null) {
			object.put(fieldName, get(value));
		}
		else if (node.isArray()) {
			((ArrayNode) node).add(get(value));
		}
		else {
			object.putArray(fieldName).addAll(Lists.newArrayList(node, get(value)));
		}
	}

	public void add(ObjectNode object, String fieldName, Iterable<T> values) {
		JsonNode node = object.get(fieldName);
		if (node == null) {
			object.putArray(fieldName).addAll(get(values));
		}
		else if (node.isArray()) {
			((ArrayNode) node).addAll(get(values));
		}
		else {
			List<JsonNode> nodes = Lists.newArrayList(node);
			nodes.addAll(get(values));
			object.putArray(fieldName).addAll(nodes);
		}
	}

	public void set(ObjectNode object, String fieldName, T value) {
		object.put(fieldName, get(value));
	}

	private List<JsonNode> get(Iterable<T> values) {
		List<JsonNode> nodes = Lists.newArrayList();
		for (T value : values) {
			nodes.add(get(value));
		}
		return nodes;
	}

	protected abstract JsonNode get(T value);

	public void configureSchema(ObjectNode schema) {
		schema.put("type", schemaType);
	}

	public void prePersist(ObjectNode object, String fieldName) {
		
	}

	public void postLoad(ObjectNode object, String fieldName) {
		
	}
}
