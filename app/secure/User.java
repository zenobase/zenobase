package secure;

import org.codehaus.jackson.node.ObjectNode;

import common.Nodes;

public class User {

	private final Identity identity;
	private final String name;
	private String password;

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
		this.password = BCrypt.hashpw(password, BCrypt.gensalt());
	}

	@Override
	public String toString() {
		return name;
	}

	public ObjectNode toJson() {
		ObjectNode object = Nodes.newObject();
		object.put("identity", identity.getId());
		object.put("name", name);
		return object;
	}
}
