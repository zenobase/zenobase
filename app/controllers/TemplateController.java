package controllers;

import play.Logger;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.With;

@With(UserController.class)
public class TemplateController extends Controller {

	public static void get(String template) {
		Logger.info("Locale: %s", Lang.get());
		renderTemplate(template);
	}
}
