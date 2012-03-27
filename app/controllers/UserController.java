package controllers;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.IdentityHelper;
import secure.User;
import secure.UserManager;
import services.CommandQueue;

import common.Nodes;

@With(Timed.class)
public class UserController extends ControllerSupport {

	@Inject
	static UserManager users;

	@Inject
	static CommandQueue queue;

	public static Result who() {
		Identity identity = IdentityHelper.in(ctx()).get();
		if (identity != null) {
			User user = users.find(identity);
			return ok(user != null ? user.toJson() : toJson(identity));
		}
    	return noContent();
    }

	public static Result get() {
    	Identity identity = IdentityHelper.in(ctx()).get();
    	if (identity == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	ArrayNode array = Nodes.newArray();
    	for (User user : users.find()) {
    		ObjectNode object = user.toJson();
    		array.add(object);
    	}
        return ok(array);
	}

	public static Result get(String name) {
		Identity identity = IdentityHelper.in(ctx()).get();
		return identity != null ? get(name, identity) : unauthorized();
	}

	private static Result get(String name, Identity identity) {
		User user = users.find(name);
		return user != null ? get(user, identity) : notFound();
	}

	private static Result get(User user, Identity identity) {
		return user.getIdentity().equals(identity) ? ok(user.toJson(true)) : forbidden();
	}

	public static Result find(String identity) {
    	return "*".equals(identity) ? get() : find(new Identity(identity)); 
    }

	private static Result find(Identity identity) {
		User user = users.find(identity);
    	return ok(user != null ? user.toJson(false) : toJson(identity)); 
    }

	private static JsonNode toJson(Identity identity) {
		ObjectNode object = Nodes.newObject();
		object.put(User.IDENTITY.getName(), identity.getId());
		return object;
	}
}
