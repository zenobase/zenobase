package commands;

import models.Bucket;
import secure.Identity;
import services.BucketManager;

public class RestoreBucketCommand extends CommandSupport {

	private final BucketManager manager;
	private final Bucket bucket;

	public RestoreBucketCommand(BucketManager manager, Identity identity, Bucket bucket) {
		super(identity);
		this.manager = manager;
		this.bucket = bucket;
	}

	@Override
	public void execute() {
		manager.store(bucket, false);
	}

	@Override
	public DeleteBucketCommand reverse(Identity identity) {
		return new DeleteBucketCommand(manager, identity, bucket);
	}

	@Override
	public String toString() {
		return String.format("restored bucket '%s'", bucket);
	}
}
