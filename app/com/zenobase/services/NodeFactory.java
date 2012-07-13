package com.zenobase.services;

import org.elasticsearch.node.Node;

public interface NodeFactory {

	Node createNode(String clusterName);
}
