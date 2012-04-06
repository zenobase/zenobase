package secure;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import schema.BooleanType;
import schema.DateTimeType;
import schema.Field;
import schema.IdentityType;
import schema.SchemaBuilder;
import schema.TokenType;

import common.Nodes;

public class User {

	public static final String TYPE_NAME = "user";
	public static final Field<String> NAME = Field.of("name", new TokenType());
	public static final Field<Identity> IDENTITY = Field.of("identity", new IdentityType());
	public static final Field<DateTime> CREATED = Field.of("created", new DateTimeType());
	public static final Field<String> PASSWORD = Field.of("password", new TokenType());
	public static final Field<String> EMAIL = Field.of("email", new TokenType());
	public static final Field<Boolean> VERIFIED = Field.of("verified", new BooleanType());
	public static final Field<Boolean> SUSPENDED = Field.of("suspended", new BooleanType());
	public static final Field<Boolean> SUPERUSER = Field.of("superuser", new BooleanType());

	private final Identity identity;
	private final String name;
	private DateTime created;
	private String password;
	private String email;
	private boolean verified;
	private boolean suspended;
	private boolean superuser;

	public User(Identity identity, String name) {
		this.identity = identity;
		this.name = name;
		this.created = new DateTime(DateTimeZone.UTC);
	}

	public User(Identity identity, String name, DateTime created) {
		this.identity = identity;
		this.name = name;
		this.created = created;
	}

	public Identity getIdentity() {
		return identity;
	}

	public String getName() {
		return name;
	}

	public DateTime getCreated() {
		return created;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean passwordEquals(String password) {
		return BCrypt.checkpw(password, this.password);
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

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public boolean isSuperuser() {
		return superuser;
	}

	public void setSuperuser(boolean superuser) {
		this.superuser = superuser;
	}

	@Override
	public String toString() {
		return name;
	}

	public User copy() {
		User copy = new User(identity, name);
		copy.created = created;
		copy.email = email;
		copy.password = password;
		copy.verified = verified;
		copy.superuser = superuser;
		copy.suspended = suspended;
		return copy;
	}

	public static ObjectNode getSchema() {
		SchemaBuilder schema = new SchemaBuilder(TYPE_NAME);
		schema.add(NAME);
		schema.add(IDENTITY);
		schema.add(CREATED);
		schema.add(PASSWORD);
		schema.add(EMAIL);
		schema.add(VERIFIED);
		schema.add(SUSPENDED);
		schema.add(SUPERUSER);
		return schema.build();
	}

	public ObjectNode toJson(boolean includeProfile) {
		ObjectNode object = Nodes.newObject();
		NAME.getType().set(object, NAME.getName(), name);
		IDENTITY.getType().set(object, IDENTITY.getName(), identity);
		if (includeProfile && email != null) {
			CREATED.getType().set(object, CREATED.getName(), created);
			if (email != null) {
				object.put(EMAIL.getName(), email);
				object.put(VERIFIED.getName(), verified);
			}
		}
		return object;
	}

	public ObjectNode toJson() {
		ObjectNode object = toJson(true);
		PASSWORD.getType().set(object, PASSWORD.getName(), password);
		SUSPENDED.getType().set(object, SUSPENDED.getName(), suspended);
		SUPERUSER.getType().set(object, SUPERUSER.getName(), superuser);
		return object;
	}

	public static User parse(ObjectNode object) {
		Identity identity = Iterables.getOnlyElement(IDENTITY.getType().get(object, IDENTITY.getName()));
		String name = Iterables.getOnlyElement(NAME.getType().get(object, NAME.getName()));
		DateTime created = Iterables.getOnlyElement(CREATED.getType().get(object, CREATED.getName()));
		User user = new User(identity, name, created);
		user.setPassword(Iterables.getOnlyElement(PASSWORD.getType().get(object, PASSWORD.getName())));
		user.setSuspended(Iterables.getOnlyElement(SUSPENDED.getType().get(object, SUSPENDED.getName()), Boolean.FALSE));
		user.setSuperuser(Iterables.getOnlyElement(SUPERUSER.getType().get(object, SUPERUSER.getName()), Boolean.FALSE));
		String email = Iterables.getOnlyElement(EMAIL.getType().get(object, EMAIL.getName()), null);
		if (email != null) {
			user.setEmail(email);
			user.setVerified(Iterables.getOnlyElement(VERIFIED.getType().get(object, VERIFIED.getName()), Boolean.FALSE));
		}
		return user;
	}
}
