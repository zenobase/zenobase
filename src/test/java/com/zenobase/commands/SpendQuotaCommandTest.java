package com.zenobase.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.models.Identity;
import org.junit.jupiter.api.Test;

public class SpendQuotaCommandTest {

	private final CommandHandlerRegistry registry = CommandHandlerRegistry.containing(new SpendQuotaCommand.Handler());

	@Test
	public void test() {
		Identity principal = new Identity();
		Command command = new SpendQuotaCommand(principal, 5);
		assertThat(command.getCost()).isEqualTo(5);
		assertThat(command.toString()).isEqualTo("spent quota");
		registry.execute(command);

		Command undo = command.reverse(principal);
		assertThat(undo.getCost()).isEqualTo(-5);
		registry.execute(undo);

		Command redo = undo.reverse(principal);
		assertThat(redo.getCost()).isEqualTo(5);
	}
}
