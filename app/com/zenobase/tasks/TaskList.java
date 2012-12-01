package com.zenobase.tasks;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;

public class TaskList extends PartialList<Task> {

	public TaskList(Iterable<Task> elements, long size) {
		super(elements, size);
	}

	public ObjectNode toJson() {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(size()));
    	ArrayNode tasksNode = resultNode.putArray("tasks");
    	for (Task task : getElements()) {
    		ObjectNode node = task.toJson();
    		node.remove(Task.CREDENTIALS.getName());
    		tasksNode.add(node);
    	}
		return resultNode;
	}
}
