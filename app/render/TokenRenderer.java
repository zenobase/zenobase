package render;

import java.io.PrintWriter;

import models.Token;
import schema.Field;

public class TokenRenderer extends Renderer<Token> {

	public TokenRenderer() {
		super(Token.class);
	}

	@Override
	public void render(Field<Token> field, Token value, PrintWriter out) {
		out.printf("<i class=\"icon-tag\" title=\"%s\"></i> %s", field.getName(), value);
	}
}
