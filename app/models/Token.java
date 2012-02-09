package models;

public class Token implements Comparable<Token> {

	private final String value;

	private Token(String value) {
		this.value = value;
	}

	public static Token valueOf(String value) {
		return new Token(value);
	}

	@Override
	public int compareTo(Token that) {
		return value.compareTo(that.value);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Token && equals((Token) that);
	}

	private boolean equals(Token that) {
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
