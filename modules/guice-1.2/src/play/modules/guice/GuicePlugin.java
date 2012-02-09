package play.modules.guice;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Named;

/**
 * Enable <a href="http://google-guice.googlecode.com">Guice</a> integration in
 * Playframework. This plugin first scans for a custom Guice Injector if it's
 * not found, then it tries to create an injector from all the guice modules
 * available on the classpath. The Plugin is then passed to Play injector for
 * Controller IoC.
 * 
 * @author <a href="mailto:info@lucianofiandesio.com">Luciano Fiandesio</a>
 * @author <a href="mailto:info@hausel@freemail.hu">Peter Hausel</a>
 * @author <a href="mailto:lrgalego@gmail.com">Lucas Galego</a>
 * @author <a href="mailto:a.a.vasiljev@gmail.com">Alexander Vasiljev</a>
 */
public class GuicePlugin extends PlayPlugin implements BeanSource {
	
	private Injector injector;
	private final List<Module> modules = new ArrayList<Module>();

	@Override
	public void onApplicationStart() {	
		Logger.debug("Starting Guice modules scanning");
		loadInjector();
		play.inject.Injector.inject(this);
		injectAnnotated();
	}

	private void loadInjector(){
		try {
            modules.clear();
            Logger.debug("Guice modules cleared");
			for (final Class clazz : Play.classloader.getAllClasses()) {
				if(clazz.getSuperclass() == null){
					continue;
				}
				if (isCustomInjector(clazz)) {
					loadCustomInjector(clazz);
					return;
				}
				if (isGuiceModule(clazz)) {
					modules.add((Module) clazz.newInstance());
				}
			}
			loadInjectorFromModules();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to create Guice injector", e);
		}
	}
	
	private boolean isCustomInjector(Class clazz){
		return GuiceSupport.class.isAssignableFrom(clazz);
	}
	
	private boolean isGuiceModule(Class clazz){
		return AbstractModule.class.isAssignableFrom(clazz);
	}
	
	private void loadCustomInjector(Class clazz) throws InstantiationException, IllegalAccessException {
		final GuiceSupport gs = (GuiceSupport) clazz.newInstance();
		injector = gs.configure();
		Logger.info("Guice injector created: " + clazz.getName());
	}
	
	private void loadInjectorFromModules(){
		if(modules.isEmpty()){
			throw new IllegalStateException("Could not find any custom guice injector or abstract modules. Are you sure you have at least one on the classpath?");
		}
		injector = Guice.createInjector(modules);
		Logger.info("Guice injector created with modules: " + moduleList());
	}

	private String moduleList(){
		final StringBuilder moduleList = new StringBuilder("\n");
		for(Module module : modules){
			moduleList.append(module.getClass());
			moduleList.append("\n");
		}
		return moduleList.toString();
	}

	public <T> T getBeanOfType(Class<T> clazz) {
		if (injector == null){
			return null;
		}
		try {
			return injector.getInstance(clazz);
		} catch (ConfigurationException e) {
			Logger.error(e.getMessage());
			return null;
		}
	}

	public <T> T getBeanWithKey(Key<T> key) {
		if (injector == null){
			return null;
		}
		return injector.getInstance(key);
	}

	private void injectAnnotated(){
		try{
			for (Class<?> clazz : Play.classloader.getAnnotatedClasses(play.modules.guice.InjectSupport.class)) {
				for (Field field : clazz.getDeclaredFields()) {
					if (isInjectable(field)) {
						inject(field);
					}
				}
			}
		} catch(Exception e){
			throw new RuntimeException("Error injecting dependencies", e);
		}
	}
	
	private boolean isInjectable(Field field){
		return Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class);
	}
	
	private void inject(Field field) throws IllegalAccessException {
		field.setAccessible(true);
		final Annotation fieldBinding = fieldBinding(field);
		if(fieldBinding != null){
			field.set(null, getBeanWithKey(Key.get(field.getType(), fieldBinding)));
		} else {
			field.set(null, getBeanOfType(field.getType()));
		}
	}
	
	private Annotation fieldBinding(Field field){
		for (Annotation annotation : field.getAnnotations()) {
			if (annotation.annotationType().equals(Named.class)) {
				return annotation;
			}
			for (Annotation internal : annotation.annotationType().getAnnotations()) {
				if (internal.annotationType().equals(BindingAnnotation.class)) {
					return annotation;
				}
			}
		}
		return null;
	}
	
}
