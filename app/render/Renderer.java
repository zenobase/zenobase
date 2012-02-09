package render;

import java.io.PrintWriter;

import schema.Field;

public abstract class Renderer<T> {

	private final Class<T> type;
	private boolean wrap;

	public Renderer(Class<T> type) {
		this.type = type;
	}

	public Class<T> getType() {
		return type;
	}

	public boolean canWrap() {
		return wrap;
	}

	protected void setWrap(boolean wrap) {
		this.wrap = wrap;
	}

	public abstract void render(Field<T> field, T value, PrintWriter out);
}
