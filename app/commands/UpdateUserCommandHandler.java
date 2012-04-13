package commands;

import com.google.inject.Inject;

import services.UserManager;

public class UpdateUserCommandHandler extends CommandHandlerSupport<UpdateUserCommand> {

	private final UserManager manager;

	@Inject
	public UpdateUserCommandHandler(UserManager manager) {
		super(UpdateUserCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(UpdateUserCommand command) {
		manager.update(command.getTo());
	}
}
