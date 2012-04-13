package commands;

import services.BucketManager;

import com.google.inject.Inject;

public class DeleteEventCommandHandler extends CommandHandlerSupport<DeleteEventCommand> {

	private final BucketManager bucketManager;

	@Inject
	public DeleteEventCommandHandler(BucketManager bucketManager) {
		super(DeleteEventCommand.class);
		this.bucketManager = bucketManager;
	}

	@Override
	public void execute(DeleteEventCommand command) {
		bucketManager.delete(command.getBucketId(), command.getEvent().getId());
	}
}
