package controllers;

import javax.inject.Inject;

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
			if (user != null) {
				return ok(user.toJson());
			}
			ObjectNode object = Nodes.newObject();
			object.put(User.IDENTITY.getName(), identity.getId());
			return ok(object);
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
}
