package commands;

import models.Bucket;
import models.Identity;
import services.BucketManager;

public class CreateBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;

	public CreateBucketCommand(BucketManager manager, Identity identity, Bucket bucket) {
		super(identity);
		this.manager = manager;
		this.bucket = bucket;
	}

	@Override
	public void execute() {
		manager.store(bucket, true);
	}

	@Override
	public DeleteBucketCommand reverse(Identity identity) {
		return new DeleteBucketCommand(manager, identity, bucket);
	}

	@Override
	public String toString() {
		return String.format("created '%s'", bucket);
	}
}
