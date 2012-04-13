package commands;

import com.google.inject.Inject;

import services.BucketManager;

public class CreateEventCommandHandler extends CommandHandlerSupport<CreateEventCommand> {

	private final BucketManager bucketManager;

	@Inject
	public CreateEventCommandHandler(BucketManager bucketManager) {
		super(CreateEventCommand.class);
		this.bucketManager = bucketManager;
	}

	@Override
	public void execute(CreateEventCommand command) {
		bucketManager.add(command.getBucketId(), command.getEvent());
	}
}
