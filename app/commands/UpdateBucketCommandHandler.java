package commands;

import com.google.inject.Inject;

import services.BucketManager;

public class UpdateBucketCommandHandler extends CommandHandlerSupport<UpdateBucketCommand> {

	private final BucketManager manager;

	@Inject
	public UpdateBucketCommandHandler(BucketManager manager) {
		super(UpdateBucketCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(UpdateBucketCommand command) {
		manager.update(command.getTo());
	}
}
