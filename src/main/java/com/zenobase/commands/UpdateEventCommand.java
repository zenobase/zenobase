package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.json.DomainNode;
import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class UpdateEventCommand extends Command {

	private static final Logger logger = LoggerFactory.getLogger(UpdateEventCommand.class);

	private static final Command.Type TYPE = new Command.Type("update event", 3);
	private static final TokenField BUCKET = new TokenField("bucket");
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateEventCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public UpdateEventCommand(Identity principal, String bucketId, Event from, Event to) {
		super(TYPE, principal);
		setParameter(BUCKET, bucketId);
		setParameter(FROM, from.toJson());
		setParameter(TO, to.toJson());
		addCost(1);
	}

	private String getBucketId() {
		return getParameter(BUCKET);
	}

	private Event getFrom() {
		return new Event(getParameter(FROM));
	}

	private Event getTo() {
		return new Event(getParameter(TO));
	}

	@Override
	public Command reverse(Identity principal) {
		Event from = getTo();
		Event to = getFrom();
		from.setVersion(from.getVersion() + 1);
		to.setVersion(to.getVersion() + 1);
		return new UpdateEventCommand(principal, getBucketId(), from, to);
	}

	@Override
	public String toString() {
		return String.format("updated event %s in %s", getTo().getId(), getBucketId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			var command = new UpdateEventCommand(node);
			return switch (version) {
				case 3 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<UpdateEventCommand> {

		private final EventRepository repository;

		@Inject
		public Handler(EventRepository repository) {
			super(UpdateEventCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateEventCommand command) {
			try {
				update(command);
			} catch (OpenSearchException e) {
				if (e.status() != 409) throw e;
				Event current =
						repository.find(command.getBucketId(), command.getTo().getId());
				if (current == null) {
					logger.warn("Recovering a missing event...");
					create(command);
				} else if (current.getVersion() < command.getTo().getVersion()) {
					logger.warn(
							"Recovering from an event version conflict: {} -> {}...",
							command.getTo().getVersion(),
							current.getVersion());
					Event correctedFrom = command.getFrom().copy();
					correctedFrom.setVersion(current.getVersion());
					DomainNode.SEQ_NO.setValue(correctedFrom.toJson(), DomainNode.SEQ_NO.getValue(current.toJson()));
					DomainNode.PRIMARY_TERM.setValue(
							correctedFrom.toJson(), DomainNode.PRIMARY_TERM.getValue(current.toJson()));
					command.setParameter(FROM, correctedFrom.toJson());
					Event correctedTo = command.getTo().copy();
					correctedTo.setVersion(current.getVersion());
					command.setParameter(TO, correctedTo.toJson());
					update(command);
				} else {
					throw e;
				}
			}
		}

		private void update(UpdateEventCommand command) {
			repository.update(
					command.getBucketId(),
					command.getFrom(),
					command.getTo().copy(),
					command.getTimestamp()); // copy to prevent the version number from being incremented
		}

		private void create(UpdateEventCommand command) {
			repository.add(command.getBucketId(), command.getTo().copy(), command.getTimestamp());
		}
	}
}
