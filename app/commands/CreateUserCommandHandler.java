package commands;

import com.google.inject.Inject;

import services.UserManager;

public class CreateUserCommandHandler extends CommandHandlerSupport<CreateUserCommand> {

	private final UserManager manager;

	@Inject
	public CreateUserCommandHandler(UserManager manager) {
		super(CreateUserCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(CreateUserCommand command) {
		manager.store(command.getUser());
	}
}
