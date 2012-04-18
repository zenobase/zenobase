package controllers;

import io.UserPrinter;

import javax.inject.Inject;

import models.User;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Result;
import play.mvc.With;
import models.Identity;
import services.CommandQueue;
import services.UserManager;

import commands.UpdateUserCommand;
import common.Callback;
import common.Identities;
import common.Nodes;
import common.PartialList;

@With(Timed.class)
public class UserController extends ControllerSupport {

	@Inject
	static UserManager users;

	@Inject
	static CommandQueue queue;

	public static Result who() {
		Identity principal = Identities.in(ctx()).get();
		if (principal != null) {
			User user = users.find(principal);
			return ok(user != null ? toJson(user) : toJson(principal));
		}
    	return noContent();
    }

	public static Result get(String name) {
		Identity principal = Identities.in(ctx()).get();
		return principal != null ? get(name, principal) : unauthorized();
	}

	private static Result get(String name, Identity principal) {
		User user = users.find(name);
		return user != null ? get(user, principal) : notFound();
	}

	private static Result get(User user, Identity principal) {
		return user.equals(principal) ? ok(toJson(user)) : forbidden();
	}

	public static Result find(String identity, int offset, int limit) {
		return identity == null ? find(offset, limit) : find(new Identity(identity)); 
    }

	public static Result find(int offset, int limit) {
    	Identity principal = Identities.in(ctx()).get();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
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
    		usersNode.add(toJson(user));
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
    	return ok(user != null ? new User(identity.getId(), user.getName()).toJson() : toJson(identity));
    }

	public static class UserUpdate {

		private final String email;
		private final String password;

		public UserUpdate(String email, String password) {
			this.email = email;
			this.password = password;
		}

		public String getEmail() {
			return email;
		}

		public String getPassword() {
			return password;
		}

		public boolean isEmpty() {
			return email == null && password == null;
		}

		public User apply(User from) {
			User to = from.copy();
			if (email != null) {
				to.setEmail(email);
				to.setVerified(false);
			}
			if (password != null) {
				to.changePassword(password);
			}
			return to;
		}

		public static UserUpdate parse(ObjectNode objectNode) {
			String email = objectNode.findPath(User.EMAIL.getName()).getTextValue();
			String password = objectNode.findPath(User.PASSWORD.getName()).getTextValue();
			return new UserUpdate(email, password);
		}
	}

	public static Result update(String name) {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest();
		}
		Identity principal = Identities.in(ctx()).get();
    	if (principal == null) {
    		return unauthorized();
    	}
		User user = users.find(name);
    	if (user == null) {
    		return notFound();
    	}
    	if (!user.equals(principal) && !users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	UserUpdate update = UserUpdate.parse(body);
    	if (!update.isEmpty()) {
    		String commandId = queue.dispatch(new UpdateUserCommand(principal, user, update.apply(user)));
            response().setHeader("Undo", String.format("/queue/%s", commandId));
    		return noContent();
    	}
		return badRequest();
	}

	private static JsonNode toJson(Identity identity) {
		ObjectNode object = Nodes.newObject();
		object.put(User.ID.getName(), identity.getId());
		return object;
	}
}
