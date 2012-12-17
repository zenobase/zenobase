package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import com.google.inject.Inject;

import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class UpdateBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("update bucket", 3);
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateBucketCommand(ObjectNode node) {
		super(node);
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
			switch (version) {
				case 2:
					UpdateBucketCommand command = new UpdateBucketCommand(node);
					migrate(command.getFrom());
					migrate(command.getTo());
					return command;
				case 3:
					return new UpdateBucketCommand(node);
			}
			return null;
		}
	}

	public static void migrate(Bucket bucket) {
		for (ObjectNode widget : bucket.getWidgets()) {
			ObjectNode original = Nodes.copy(widget);
			widget.remove("description");
			widget.remove("singleton");
			String type = widget.get("type").getTextValue();
			if ("histogram".equals(type)) {
				widget.put("type", "ratings");
				widget.remove("field");
				widget.remove("from");
				widget.remove("to");
				widget.remove("step");
			} else if ("intervals".equals(type)) {
				widget.put("type", "histogram");
			} else if ("calendar-count".equals(type)) {
				widget.put("type", "time_histogram");
			} else if ("map".equals(type)) {
				rename(widget, "markerColor", "marker_color");
			} else if ("gantt".equals(type)) {
				rename(widget, "termField", "field");
				widget.remove("valueField");
			} else if ("plot".equals(type) || "timeline".equals(type)) {
				widget.remove("keyField");
				rename(widget, "valueField", "field");
			} else if ("scoreboard".equals(type)) {
				rename(widget, "termField", "key_field");
				rename(widget, "valueField", "value_field");
			}
			Logger.info("migrated widget:\n>" + original + "\n<" + widget);
		}
	}

	private static void rename(ObjectNode node, String from, String to) {
		node.put(to, node.get(from));
		node.remove(from);
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
			repository.update(command.getTo().copy()); // copy to prevent the version number from being incremented
		}
	}
}
