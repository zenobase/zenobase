package secure;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import schema.BooleanField;
import schema.DateTimeField;
import schema.IdentityField;
import schema.Schema;
import schema.SchemaBuilder;
import schema.TokenField;

import common.Nodes;

public class User {

	public static final String TYPE_NAME = "user";
	public static final TokenField NAME = new TokenField("name");
	public static final IdentityField IDENTITY = new IdentityField("identity");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField PASSWORD = new TokenField("password");
	public static final TokenField EMAIL = new TokenField("email");
	public static final BooleanField VERIFIED = new BooleanField("verified");
	public static final BooleanField SUSPENDED = new BooleanField("suspended");
	public static final BooleanField SUPERUSER = new BooleanField("superuser");

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

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(NAME).add(IDENTITY)
			.add(CREATED).add(PASSWORD).add(EMAIL)
			.add(VERIFIED).add(SUSPENDED).add(SUPERUSER).build();
	}

	public ObjectNode toJson(boolean includeProfile) {
		ObjectNode object = Nodes.newObject();
		NAME.setValue(object, name);
		IDENTITY.setValue(object, identity);
		if (includeProfile && email != null) {
			CREATED.setValue(object, created);
			if (email != null) {
				object.put(EMAIL.getName(), email);
				object.put(VERIFIED.getName(), verified);
			}
		}
		return object;
	}

	public ObjectNode toJson() {
		ObjectNode object = toJson(true);
		PASSWORD.setValue(object, password);
		SUSPENDED.setValue(object, suspended);
		SUPERUSER.setValue(object, superuser);
		return object;
	}

	public static User parse(ObjectNode object) {
		Identity identity = Iterables.getOnlyElement(IDENTITY.getValues(object));
		String name = Iterables.getOnlyElement(NAME.getValues(object));
		DateTime created = Iterables.getOnlyElement(CREATED.getValues(object));
		User user = new User(identity, name, created);
		user.setPassword(Iterables.getOnlyElement(PASSWORD.getValues(object)));
		user.setSuspended(Iterables.getOnlyElement(SUSPENDED.getValues(object), Boolean.FALSE));
		user.setSuperuser(Iterables.getOnlyElement(SUPERUSER.getValues(object), Boolean.FALSE));
		String email = Iterables.getOnlyElement(EMAIL.getValues(object), null);
		if (email != null) {
			user.setEmail(email);
			user.setVerified(Iterables.getOnlyElement(VERIFIED.getValues(object), Boolean.FALSE));
		}
		return user;
	}
}
