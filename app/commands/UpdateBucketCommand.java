package commands;

import models.Bucket;
import models.Identity;

public class UpdateBucketCommand extends CommandSupport {

	public static final String TYPE = "update bucket";

	private final Bucket from, to;

	public UpdateBucketCommand(Identity identity, Bucket from, Bucket to) {
		super(TYPE, identity);
		this.from = from;
		this.to = to;
	}

	public Bucket getFrom() {
		return from;
	}

	public Bucket getTo() {
		return to;
	}

	@Override
	public Command reverse(Identity identity) {
		return new UpdateBucketCommand(identity, to, from);
	}

	@Override
	public String toString() {
		return String.format("updated '%s'", to);
	}
}
