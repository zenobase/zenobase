package commands;

import models.Bucket;
import secure.Identity;
import services.BucketManager;

public class UpdateBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket from, to;

	public UpdateBucketCommand(BucketManager manager, Identity identity, Bucket from, Bucket to) {
		super(identity);
		this.manager = manager;
		this.from = from;
		this.to = to;
	}

	@Override
	public void execute() {
		manager.update(to);
	}

	@Override
	public UpdateBucketCommand reverse(Identity identity) {
		return new UpdateBucketCommand(manager, identity, to, from);
	}

	@Override
	public String toString() {
		return String.format("updated '%s'", to);
	}
}
