package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.ObjectField;
import com.zenobase.json.DomainNode;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class UpdateBucketCommand extends Command {

	private static final Logger logger = LoggerFactory.getLogger(UpdateBucketCommand.class);

	private static final Command.Type TYPE = new Command.Type("update bucket", 5);
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateBucketCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public UpdateBucketCommand(Identity principal, Bucket from, Bucket to) {
		super(TYPE, principal);
		setParameter(FROM, from.toJson());
		setParameter(TO, to.toJson());
	}

	private Bucket getFrom() {
		return new Bucket(getParameter(FROM));
	}

	private Bucket getTo() {
		return new Bucket(getParameter(TO));
	}

	@Override
	public Command reverse(Identity principal) {
		Bucket from = getTo();
		Bucket to = getFrom();
		from.setVersion(from.getVersion() + 1);
		to.setVersion(to.getVersion() + 1);
		return new UpdateBucketCommand(principal, from, to);
	}

	@Override
	public String toString() {
		return String.format("updated bucket %s", getTo().getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			var command = new UpdateBucketCommand(node);
			switch (version) {
				case 5: return command;
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<UpdateBucketCommand> {

		private final BucketRepository repository;

		@Inject
		public Handler(BucketRepository repository) {
			super(UpdateBucketCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateBucketCommand command) {
			try {
				update(command);
			} catch (OpenSearchException e) {
				if (e.status() != 409) throw e;
				Bucket current = repository.find(command.getTo().getId());
				if (current != null && current.getVersion() < command.getTo().getVersion()) {
					logger.warn("Recovering from a bucket version conflict: {} -> {}...", command.getTo().getVersion(), current.getVersion());
					Bucket correctedFrom = command.getFrom().copy();
					correctedFrom.setVersion(current.getVersion());
					DomainNode.SEQ_NO.setValue(correctedFrom.toJson(), DomainNode.SEQ_NO.getValue(current.toJson()));
					DomainNode.PRIMARY_TERM.setValue(correctedFrom.toJson(), DomainNode.PRIMARY_TERM.getValue(current.toJson()));
					command.setParameter(FROM, correctedFrom.toJson());
					Bucket correctedTo = command.getTo().copy();
					correctedTo.setVersion(current.getVersion());
					command.setParameter(TO, correctedTo.toJson());
					update(command);
				} else {
					throw e;
				}
			}
		}

		private void update(UpdateBucketCommand command) {
			repository.update(command.getFrom(), command.getTo().copy(), command.getTimestamp()); // copy to prevent the version number from being incremented
		}
	}
}
