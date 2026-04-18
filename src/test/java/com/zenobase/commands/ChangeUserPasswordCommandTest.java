package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.models.Identity;
import org.junit.jupiter.api.Test;

public class ChangeUserPasswordCommandTest {

	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(
		new ChangeUserPasswordCommand.Handler()
	);

	@Test
	public void test() {
		Identity principal = new Identity();
		Command command = new ChangeUserPasswordCommand(principal, "tester", "old-password", "new-password");
		registry.execute(command);
		assertThat(command.toString()).isEqualTo("changed password for user tester");

		Command undo = command.reverse(principal);
		registry.execute(undo);
		assertThat(undo.toJson().get("parameters")).isNotEqualTo(command.toJson().get("parameters"));

		Command redo = undo.reverse(principal);
		assertThat(redo.toJson().get("parameters")).isEqualTo(command.toJson().get("parameters"));
	}
}
