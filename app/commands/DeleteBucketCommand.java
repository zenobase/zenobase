package commands;

import models.Bucket;
import models.Identity;
import services.BucketManager;

public class DeleteBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;

	public DeleteBucketCommand(BucketManager manager, Identity identity, Bucket bucket) {
		super(identity);
		this.manager = manager;
		this.bucket = bucket;
	}

	@Override
	public void execute() {
		manager.deleteBucket(bucket.getId());
	}

	@Override
	public RestoreBucketCommand reverse(Identity identity) {
		return new RestoreBucketCommand(manager, identity, bucket);
	}

	@Override
	public String toString() {
		return String.format("deleted bucket '%s'", bucket);
	}
}
