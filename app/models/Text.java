package models;

public class Text {

	private final String value;

	private Text(String label) {
		this.value = label;
	}

	public static Text valueOf(String value) {
		return new Text(value);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Text && equals((Text) that);
	}

	private boolean equals(Text that) {
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value;
	}
}
