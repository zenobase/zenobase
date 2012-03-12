package secure;

import java.util.List;

import models.Token;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;

import schema.BooleanType;
import schema.Field;
import schema.IdentityType;
import schema.SchemaBuilder;
import schema.TokenType;

import common.Nodes;

public class User {

	public static final String TYPE_NAME = "user";
	public static final Field<Token> NAME = Field.of("name", new TokenType());
	public static final Field<Identity> IDENTITY = Field.of("identity", new IdentityType());
	public static final Field<Token> PASSWORD = Field.of("password", new TokenType());
	public static final Field<Token> EMAIL = Field.of("email", new TokenType());
	public static final Field<Boolean> VERIFIED = Field.of("verified", new BooleanType());

	private final Identity identity;
	private final String name;
	private String password;
	private String email;
	private boolean verified;

	public User(Identity identity, String name) {
		this.identity = identity;
		this.name = name;
	}

	public Identity getIdentity() {
		return identity;
	}

	public String getName() {
		return name;
	}

	public boolean passwordEquals(String password) {
		return BCrypt.checkpw(password, this.password);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void changePassword(String password) {
		this.password = BCrypt.hashpw(password, BCrypt.gensalt());
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	@Override
	public String toString() {
		return name;
	}

	public static ObjectNode getSchema() {
		SchemaBuilder schema = new SchemaBuilder(TYPE_NAME);
		schema.add(NAME);
		schema.add(IDENTITY);
		schema.add(PASSWORD);
		schema.add(EMAIL);
		schema.add(VERIFIED);
		return schema.build();
	}

	public ObjectNode toJson(boolean includeProfile) {
		ObjectNode object = Nodes.newObject();
		NAME.getType().set(object, NAME.getName(), Token.valueOf(name));
		IDENTITY.getType().set(object, IDENTITY.getName(), identity);
		if (includeProfile && email != null) {
			object.put(EMAIL.getName(), email);
			object.put(VERIFIED.getName(), verified);
		}
		return object;
	}

	public ObjectNode toJson() {
		ObjectNode object = toJson(true);
		object.put(PASSWORD.getName(), password);
		return object;
	}

	public static User parse(ObjectNode object) {
		Identity identity = Iterables.getOnlyElement(IDENTITY.getType().get(object, IDENTITY.getName()));
		String name = Iterables.getOnlyElement(NAME.getType().get(object, NAME.getName())).toString();
		User user = new User(identity, name);
		user.setPassword(Iterables.getOnlyElement(PASSWORD.getType().get(object, PASSWORD.getName())).toString());
		List<Token> tokens = EMAIL.getType().get(object, EMAIL.getName());
		String email = !tokens.isEmpty() ? Iterables.getOnlyElement(tokens).toString() : null;
		if (email != null) {
			user.setEmail(email);
			user.setVerified(Iterables.getOnlyElement(VERIFIED.getType().get(object, VERIFIED.getName())));
		}
		return user;
	}
}
