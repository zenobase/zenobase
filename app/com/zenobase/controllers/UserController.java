package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.base.Strings;

import com.zenobase.commands.ChangePasswordCommand;
import com.zenobase.commands.UpdateUserCommand;
import com.zenobase.common.BCrypt;
import com.zenobase.common.Callback;
import com.zenobase.common.Nodes;
import com.zenobase.common.PartialList;
import com.zenobase.common.SecurityContext;
import com.zenobase.io.UserPrinter;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserProfile;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

@With(Timed.class)
public class UserController extends ControllerSupport {

	@Inject
	static UserManager users;

	@Inject
	static CommandQueue queue;

	@Inject
	static VerificationMailer verificationMailer;

	@Inject
	static PasswordResetMailer resetMailer;

	public static Result who() {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal != null) {
			User user = users.find(principal);
			return ok(user != null ? new UserInfo(user).toJson() : principal.toJson());
		}
    	return noContent();
    }

	public static Result get(String name) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
		if (principal == null) {
			return unauthorized();
		}
		User user = users.find(name);
		if (user == null) {
			return notFound();
		}
		return user.equals(principal) ? ok(new UserProfile(user).toJson()) : forbidden();
	}

	public static Result find(String identity, int offset, int limit) {
		return identity == null ? find(offset, limit) : find(new Identity(identity));
    }

	public static Result find(int offset, int limit) {
    	Identity principal = new SecurityContext(ctx()).getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!users.isSuperuser(principal)) {
    		return forbidden();
    	}
    	if (offset == 0 && limit == Integer.MAX_VALUE) {
    		return findAll();
    	}
        return ok(toJson(users.find(offset, limit)));
	}

	private static ObjectNode toJson(PartialList<User> result) {
    	ObjectNode resultNode = Nodes.newObject();
    	resultNode.put("total", result.size());
    	ArrayNode usersNode = resultNode.putArray("users");
    	for (User user : result.getElements()) {
    		usersNode.add(new UserProfile(user).toJson());
    	}
		return resultNode;
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
    	return ok(user != null ? new UserInfo(user).toJson() : identity.toJson());
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
			if (email != null && !email.equals(from.getEmail())) {
				to.setEmail(email);
				to.setVerified(false);
			}
			if (password != null) {
				to.changePassword(password);
			}
			return to;
		}

		public static UserUpdate parse(ObjectNode node) {
			String email = node.findPath(User.EMAIL.getName()).getTextValue();
			String password = node.findPath(User.PASSWORD.getName()).getTextValue();
			return new UserUpdate(email, password);
		}
	}

	public static Result update(String name) {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest();
		}
		Identity principal = new SecurityContext(ctx()).getPrincipal();
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
    	if (update.isEmpty()) {
    		return badRequest();
    	}
		User updated = update.apply(user);
		String commandId = queue.dispatch(new UpdateUserCommand(principal, user, updated));
		if (!updated.isVerified()) {
			verificationMailer.send(updated);
		}
		response().setHeader("Undo", String.format("/queue/%s", commandId));
		return noContent();
	}

	public static Result requestReset() {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest("missing request body");
		}
		String username = body.path("username").getTextValue();
		if (username == null) {
			return badRequest("missing user name");
		}
		User user = users.find(username);
    	if (user == null) {
    		return notFound("user not found");
    	}
		if (!user.isVerified()) {
			// return badRequest("can't perform reset because email is not verified");
		}
		String email = body.path("email").getTextValue();
		if (email == null || !email.equals(user.getEmail())) {
			return badRequest("invalid email");
		}
		resetMailer.send(user);
		return noContent();
	}

	public static Result performReset(String name) {
		ObjectNode body = (ObjectNode) request().body().asJson();
		if (body == null) {
			return badRequest();
		}
		User user = users.find(name);
    	if (user == null) {
    		return notFound();
    	}
		String pass = body.path("pass").getTextValue();
		String hash = body.path("hash").getTextValue();
		String time = body.path("time").getTextValue();
		if (System.currentTimeMillis() - Long.parseLong(time, 36) > 1000 * 60 * 60 * 24) {
			return badRequest("too long");
		}
		if (Strings.isNullOrEmpty(pass) || Strings.isNullOrEmpty(hash) || Strings.isNullOrEmpty(time)) {
			return badRequest("missing data");
		}
		if (!BCrypt.checkpw(PasswordResetMailer.toString(user, time), hash)) {
			return badRequest("invalid data");
		}
		queue.dispatch(new ChangePasswordCommand(user.asIdentity(), user.getName(), user.getPassword(), BCrypt.hashpw(pass, BCrypt.gensalt())));
		return noContent();
	}
}
