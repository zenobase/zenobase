package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;

public class TaskList extends LazyList<Task> {

	public TaskList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected Task toObject(ObjectNode node) {
		return new Task(node);
	}

	public static ObjectNode toJson(PartialList<Task> tasks) {
		ObjectNode resultNode = Nodes.newObject();
		TOTAL.setValue(resultNode, Ints.checkedCast(tasks.getTotal()));
		ArrayNode tasksNode = resultNode.putArray("tasks");
		for (Task task : tasks) {
			tasksNode.add(task.toJson());
		}
		return resultNode;
	}
}
