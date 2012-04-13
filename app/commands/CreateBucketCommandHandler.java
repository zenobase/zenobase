package commands;

import com.google.inject.Inject;

import services.BucketManager;

public class CreateBucketCommandHandler extends CommandHandlerSupport<CreateBucketCommand> {

	private final BucketManager manager;

	@Inject
	public CreateBucketCommandHandler(BucketManager manager) {
		super(CreateBucketCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(CreateBucketCommand command) {
		manager.store(command.getBucket(), true);
	}
}
