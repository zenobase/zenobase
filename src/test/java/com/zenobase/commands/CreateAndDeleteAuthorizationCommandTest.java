package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import org.junit.jupiter.api.Test;

public class CreateAndDeleteAuthorizationCommandTest {

	@Test
	public void test() {
		Identity principal = new Identity();
		Authorization authorization = new Authorization(principal);

		CreateAuthorizationCommand created = new CreateAuthorizationCommand(principal, authorization);
		assertThat(created.getAuthorization().toJson()).isEqualTo(authorization.toJson());
		assertThat(created.getTimestamp()).isEqualTo(authorization.getCreated());
		assertThat(created.toString()).isEqualTo("created an authorization");

		Command undo = created.reverse(principal);
		assertThat(undo).isInstanceOf(DeleteAuthorizationCommand.class);
		assertThat(undo.toString()).isEqualTo("removed an authorization");

		Command redo = undo.reverse(principal);
		assertThat(redo).isInstanceOf(CreateAuthorizationCommand.class);
		assertThat(((CreateAuthorizationCommand) redo).getAuthorization().toJson()).isEqualTo(authorization.toJson());
	}
}
