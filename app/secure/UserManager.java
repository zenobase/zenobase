package secure;

import java.util.Map;

import org.elasticsearch.common.collect.Maps;

public class UserManager {

	private final Map<String, User> users = Maps.newHashMap();

	public User find(Identity identity) {
		for (User user : users.values()) {
			if (user.getIdentity().equals(identity)) {
				return user;
			}
		}
		return null;
	}

	public User find(String name) {
		return users.get(name);
	}

	public void store(User user) {
		users.put(user.getName(), user);
	}
}
