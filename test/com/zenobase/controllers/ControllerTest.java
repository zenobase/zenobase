package com.zenobase.controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.NO_CONTENT;
import static play.test.Helpers.*;

import org.junit.Ignore;
import org.junit.Test;
import play.api.libs.Crypto;
import play.mvc.Http.Cookie;
import play.mvc.Result;

import com.zenobase.models.Identity;

public class ControllerTest {

	@Test
	@Ignore
	public void test() {
		UserController.users = null;
		Identity identity = new Identity();
		Cookie cookie = new Cookie("token", Crypto.sign(identity.getId(), "secret".getBytes()) + "-" + identity.getId(), 0, "/", null, false, true);
		Result result = callAction(com.zenobase.controllers.routes.ref.UserController.who(), fakeRequest().withCookies(cookie));
		assertThat(status(result)).isEqualTo(NO_CONTENT);
	}
}
