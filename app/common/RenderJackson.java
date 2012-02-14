package common;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;

import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;

public class RenderJackson extends Result {

	private final JsonNode object;

	public RenderJackson(JsonNode object) {
		this.object = object;
	}

	public void apply(Request request, Response response) {
		try {
			setContentTypeIfNotSet(response, "application/json; charset=" + getEncoding());
			new JsonPrinter(response.out).print(object);
		} catch (IOException e) {
			throw new UnexpectedException(e);
		}
	}
}
