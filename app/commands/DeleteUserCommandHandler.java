package commands;

import com.google.inject.Inject;

import services.UserManager;

public class DeleteUserCommandHandler extends CommandHandlerSupport<DeleteUserCommand> {

	private final UserManager manager;

	@Inject
	public DeleteUserCommandHandler(UserManager manager) {
		super(DeleteUserCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(DeleteUserCommand command) {
		manager.delete(command.getUser());
	}
}
