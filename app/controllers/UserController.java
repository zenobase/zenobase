package controllers;

import io.UserPrinter;

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

import common.Callback;
import common.Nodes;
import common.PartialList;

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

	public static Result find(String identity, int offset, int limit) {
		return identity == null ? find(offset, limit) : find(new Identity(identity)); 
    }

	public static Result find(int offset, int limit) {
    	Identity identity = IdentityHelper.in(ctx()).get();
    	if (identity == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(identity)) {
    		return forbidden();
    	}
    	if (offset == 0 && limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
    	PartialList<User> result = users.find(offset, limit);
    	ObjectNode resultNode = Nodes.newObject();
    	resultNode.put("total", result.size());
    	ArrayNode usersNode = resultNode.putArray("users");
    	for (User user : result.getElements()) {
    		usersNode.add(user.toJson());
    	}
        return ok(resultNode);
	}

	private static Result findAll() {
    	Chunks<String> chunks = new StringChunks() {
			@Override
			public void onReady(final Out<String> out) {
		    	final UserPrinter printer = new UserPrinter(out);
				users.find(new Callback<User>() {
					@Override
					public void call(User user) {
						printer.print(user);
					}
				});
		    	out.close();
			}
		};
        return ok(chunks);
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
