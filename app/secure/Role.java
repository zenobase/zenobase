package secure;

public class Role {

	public static final String OWNER = "owner";

	private final Identity identity;
	private final String role;

	public Role(Identity identity, String role) {
		this.identity = identity;
		this.role = role;
	}

	public Identity getIdentity() {
		return identity;
	}

	public String getRole() {
		return role;
	}

	@Override
	public String toString() {
		return identity + " (" + role + ")";
	}
}
