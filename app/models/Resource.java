package models;

public class Resource {

	private final Text title;
	private final Token url;

	public Resource(Text title, Token url) {
		this.title = title;
		this.url = url;
	}

	public Text getTitle() {
		return title;
	}

	public Token getUrl() {
		return url;
	}
}
