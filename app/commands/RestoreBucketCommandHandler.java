package commands;

import com.google.inject.Inject;

import services.BucketManager;

public class RestoreBucketCommandHandler extends CommandHandlerSupport<RestoreBucketCommand> {

	private final BucketManager manager;

	@Inject
	public RestoreBucketCommandHandler(BucketManager manager) {
		super(RestoreBucketCommand.class);
		this.manager = manager;
	}

	@Override
	public void execute(RestoreBucketCommand command) {
		manager.store(command.getBucket(), false);
	}
}
