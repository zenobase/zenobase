package secure;

import java.util.Map;

import models.Token;

import org.codehaus.jackson.node.ObjectNode;

import schema.BooleanType;
import schema.Field;
import schema.SchemaBuilder;
import schema.TokenType;

import common.Nodes;

public class User {

	public static final String TYPE_NAME = "user";
	public static final Field<Token> NAME = Field.of("name", new TokenType());
	public static final Field<Token> IDENTITY = Field.of("identity", new TokenType());
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

	public ObjectNode toPublicJson() {
		ObjectNode object = Nodes.newObject();
		object.put(NAME.getName(), name);
		object.put(IDENTITY.getName(), identity.getId());
		return object;
	}

	public ObjectNode toPrivateJson() {
		ObjectNode object = toPublicJson();
		if (email != null) {
			object.put(EMAIL.getName(), email);
			object.put(VERIFIED.getName(), verified);
		}
		return object;
	}

	public ObjectNode toJson() {
		ObjectNode object = toPrivateJson();
		object.put(PASSWORD.getName(), password);
		return object;
	}

	@Deprecated // TODO should read from an ObjectNode
	public static User fromMap(Map<String, Object> map) {
		Identity identity = new Identity((String) map.get(IDENTITY.getName()));
		String name = (String) map.get(NAME.getName());
		User user = new User(identity, name);
		user.setPassword((String) map.get(PASSWORD.getName()));
		String email = (String) map.get(EMAIL.getName());
		if (email != null) {
			user.setEmail(email);
			user.setVerified((Boolean) map.get(VERIFIED.getName()));
		}
		return user;
	}
}
