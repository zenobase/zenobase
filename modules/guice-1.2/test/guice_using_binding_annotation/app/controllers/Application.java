package controllers;

import javax.inject.Inject;

import play.Logger;
import play.modules.guice.InjectSupport;
import play.mvc.Controller;
import au.com.louth.Car;
import au.com.louth.annotations.Loud;
import au.com.louth.annotations.Quiet;

import com.google.inject.name.Named;

@InjectSupport
public class Application extends Controller {

	@Inject @Quiet
	static Car car1;
	
	@Inject @Loud
	static Car car2;
	
	@Inject @Named("Foo")
	static Car car3;
	
	public static void car1(){
		renderJSON(car1.drive());
	}
	
	public static void car2(){
		renderJSON(car2.drive());
	}
	
	public static void car3(){
		renderJSON(car3.drive());
	}
	
	
    public static void index() {
        render();
    }

}