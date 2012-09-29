package com.zenobase.controllers;

import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import play.Logger;
import play.libs.F;
import play.libs.WS;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.json.Nodes;

@With(Timed.class)
public class OpenGraphController extends ControllerSupport {

	public static Result get(final String url) {
		return async(WS.url(url).get().map(new F.Function<WS.Response, Result>() {
			@Override
			public Result apply(WS.Response response) {
				Logger.info(response.getUri() + ": " + response.getStatus());
				ObjectNode node = toJson(url, extractTitle(response));
				return ok(node);
			}
		}));
	}

	private static String extractTitle(WS.Response response) {
		try {
			Document doc = Jsoup.parse(response.getBodyAsStream(), null, response.getUri().toString());
			Element titleElement = doc.select("meta[property=og:title]").first();
			if (titleElement != null) {
				Logger.info("og:title " + titleElement);
				return titleElement.attr("content");
			}
			titleElement = doc.select("title").first();
			if (titleElement != null) {
				Logger.info("title " + titleElement);
				return titleElement.text();
			}
		} catch (IOException e) {
			Logger.warn("Couldn't parse document at " + response.getUri());
		}
		return null;
	}

	private static ObjectNode toJson(final String url, String title) {
		ObjectNode node = Nodes.newObject();
		node.put("url", url);
		if (title != null) {
			node.put("title", title);
		}
		return node;
	}
}
