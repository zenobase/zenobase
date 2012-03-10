package controllers;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import secure.Identity;
import secure.User;
import secure.UserManager;

import common.Nodes;

@With(Timed.class)
public class UserController extends ControllerSupport {

	@Inject
	static UserManager users;

	public static Result who() {
		Identity identity = SecurityController.identity(false);
		if (identity != null) {
			User user = users.find(identity);
			return ok(user != null ? user.toJson() : toJson(identity));
		}
    	return noContent();
    }

	public static Result get(String name) {
		User user = users.find(name);
    	if (user == null) {
    		return notFound();
    	}
		return ok(SecurityController.checkIdentity(user.getIdentity()) ? user.toPrivateJson() : user.toPublicJson());
    }

	public static Result find(String identity) {
    	return find(new Identity(identity)); 
    }

	private static Result find(Identity identity) {
		User user = users.find(identity);
    	return ok(user != null ? user.toPublicJson() : toJson(identity)); 
    }

	private static JsonNode toJson(Identity identity) {
		ObjectNode object = Nodes.newObject();
		object.put(User.IDENTITY.getName(), identity.getId());
		return object;
	}
}
