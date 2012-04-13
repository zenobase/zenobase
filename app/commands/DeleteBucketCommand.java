package commands;

import models.Bucket;
import models.Identity;

public class DeleteBucketCommand extends CommandSupport {

	public static final String TYPE = "delete bucket";

	private final Bucket bucket;

	public DeleteBucketCommand(Identity identity, Bucket bucket) {
		super(TYPE, identity);
		this.bucket = bucket;
	}

	public Bucket getBucket() {
		return bucket;
	}

	@Override
	public Command reverse(Identity identity) {
		return new RestoreBucketCommand(identity, bucket);
	}

	@Override
	public String toString() {
		return String.format("deleted bucket '%s'", bucket);
	}
}
