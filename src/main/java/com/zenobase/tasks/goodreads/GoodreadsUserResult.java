package com.zenobase.tasks.goodreads;

import com.google.common.base.Preconditions;
import com.zenobase.tasks.XmlResultSupport;
import org.w3c.dom.Document;

class GoodreadsUserResult extends XmlResultSupport {

	GoodreadsUserResult(Document document) {
		super(document);
	}

	public String getId() {
		String userId = selectText("/GoodreadsResponse/user/@id");
		return Preconditions.checkNotNull(userId, "missing user ID");
	}
}
