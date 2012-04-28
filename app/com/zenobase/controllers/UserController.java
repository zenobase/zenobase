package com.zenobase.controllers;

import javax.inject.Inject;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.commands.ChangeUserEmailCommand;
import com.zenobase.commands.ChangeUserPasswordCommand;
import com.zenobase.commands.ChangeUserVerifiedCommand;
import com.zenobase.common.BCrypt;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.io.UserPrinter;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.models.UserProfile;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

@With(Timed.class)
public class UserController extends ControllerSupport {

	private static final TokenField KEY = new TokenField("key");
	private static final TokenField EXPIRES = new TokenField("expires");
	private static final TokenField USERNAME = new TokenField("username");

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

	@BodyParser.Of(BodyParser.Json.class)
	public static Result update(String username) {
		ObjectNode body = body();
		User user = users.find(username);
    	if (user == null) {
    		return notFound("user not found");
    	}
    	if (body.has(User.EMAIL.getName())) {
    		return updateEmail(body, user);
    	}
    	if (body.has(User.PASSWORD.getName())) {
    		return updatePassword(body, user);
    	}
    	if (body.has(User.VERIFIED.getName())) {
    		return updateVerified(body, user);
    	}
    	return badRequest("invalid update request");
	}

	private static Result updateEmail(ObjectNode node, User user) {
		Identity principal = new SecurityContext(ctx()).getPrincipal();
    	if (principal == null) {
    		return unauthorized();
    	}
    	if (!user.equals(principal) && !users.isSuperuser(principal)) {
    		return forbidden();
    	}
		String email = User.EMAIL.getValue(node);
    	if (!SignUpForm.isValidEmail(email)) {
    		return badRequest("invalid email address");
    	}
		String commandId = queue.dispatch(new ChangeUserEmailCommand(principal, user.getName(), user.isVerified() && user.getEmail().equals(email), user.getEmail(), email));
		verificationMailer.send(user.getName(), email);
		return ok(receipt(commandId));
	}

	private static Result updatePassword(ObjectNode node, User user) {
    	String key = KEY.getValue(node);
    	if (key == null || key.length() < 50) {
    		return badRequest("missing key field");
    	}
    	String password = User.PASSWORD.getValue(node);
		if (!SignUpForm.isValidPassword(password)) {
			return badRequest("invalid password");
		}
    	String expires = EXPIRES.getValue(node);
		if (expires == null) {
			return badRequest("missing field " + EXPIRES);
		}
		if (new DateTime(Long.parseLong(expires, 36)).isBefore(new DateTime())) {
			return badRequest("request expired");
		}
		if (!BCrypt.checkpw(PasswordResetMailer.toString(user, expires), key)) {
			return badRequest("invalid key");
		}
		queue.dispatch(new ChangeUserPasswordCommand(user.asIdentity(), user.getName(), user.getHashedPassword(), User.getHashedPassword(password)));
		new SecurityContext(ctx()).setPrincipal(user.asIdentity(), true);
		return noContent();
	}

	private static Result updateVerified(ObjectNode node, User user) {
		if (user.isVerified()) {
			return badRequest("already verified");
		}
		String key = KEY.getValue(node);
		if (key == null || key.length() < 50) {
			return badRequest("missing key");
		}
		boolean verified = User.VERIFIED.getValue(node);
		if (!verified) {
			return badRequest("verified expected true");
		}
		if (!BCrypt.checkpw(VerificationMailer.toString(user), key)) {
			return badRequest("invalid key");
		}
		queue.dispatch(new ChangeUserVerifiedCommand(user.asIdentity(), user.getName(), true));
		return noContent();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result requestReset() {
		ObjectNode body = body();
		String username = USERNAME.getValue(body);
		if (username == null) {
			return badRequest("missing user name");
		}
		User user = users.find(username);
    	if (user == null) {
    		return badRequest("user not found");
    	}
		if (!user.isVerified()) {
			return badRequest("can't reset password without a verified email address");
		}
		String email = User.EMAIL.getValue(body);
		if (email == null || !email.equals(user.getEmail())) {
			return badRequest("invalid email");
		}
		resetMailer.send(user);
		return noContent();
	}
}
