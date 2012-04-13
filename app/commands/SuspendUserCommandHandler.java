package commands;

import com.google.inject.Inject;

import models.User;
import services.UserManager;

public class SuspendUserCommandHandler extends CommandHandlerSupport<SuspendUserCommand> {

	private final UserManager manager;

	@Inject
	public SuspendUserCommandHandler(UserManager manager) {
		super(SuspendUserCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(SuspendUserCommand command) {
		User user = command.getUser().copy();
		user.setSuspended(command.isSuspend());
		manager.update(user);
	}
}
