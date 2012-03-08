package schema;

import org.codehaus.jackson.node.ObjectNode;

public class Field<T> {

	private final String name;
	private final Type<T> type;

	public static <T> Field<T> of(String name, Type<T> type) {
		return new Field<T>(name, type);
	}

	private Field(String name, Type<T> type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Type<T> getType() {
		return type;
	}

	public void prePersist(ObjectNode object) {
		if (object.has(name)) {
			type.prePersist(object, name);
		}
	}

	public void postLoad(ObjectNode object) {
		if (object.has(name)) {
			type.postLoad(object, name);
		}
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Field && equals((Field<?>) that);
	}

	private boolean equals(Field<?> that) {
		return name.equals(that.getName());
	}
}
