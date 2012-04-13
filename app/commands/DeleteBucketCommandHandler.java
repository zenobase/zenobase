package commands;

import com.google.inject.Inject;

import services.BucketManager;

public class DeleteBucketCommandHandler extends CommandHandlerSupport<DeleteBucketCommand> {

	private final BucketManager manager;

	@Inject
	public DeleteBucketCommandHandler(BucketManager manager) {
		super(DeleteBucketCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(DeleteBucketCommand command) {
		manager.deleteBucket(command.getBucket().getId());
	}
}
