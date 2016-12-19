package edu.kit.joana.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements a watch dog which takes care that a given object is initialized properly.
 * This means that:
 * <ul>
 * <li> every declared field of the object is set</li>
 * <li> no field is set twice</li>
 * </ul>
 * The use cases which were thought of here are simple POJOs with public non-final fields and
 * (consequently) no getters/setters.
 * I assume that it is considered a programming error that not all fields have been set and
 * every field has been set once.
 * Usage examples:<br/>
 * <verbatim>
 * T o = new T(); // (you know what T is in your context) <br/>
 * ReflectiveWatchdog<T> rwd = new ReflectiveWatchdog<T>(o); <br/>
 * rwd.set("f", value); // no type safety here but there are runtime errors if this fails <br/>
 * ...<br/>
 * rwd.set("g", value); <br/>
 * ...<br/>
 * T checkedO = rwd.getResult(); // returns o, but before that, it is checked that all declared fields of o have been set. If there are unset fields, a runtime exception is thrown.<p/>
 * </verbatim>
 * @author  Martin Mohr &lt;martin.mohr@kit.edu&gt;
 *
 * @param <T> type of watched object
 */
public class ReflectiveWatchdog<T> {
	private T obj;
	private Set<String> initializedFields;
	public ReflectiveWatchdog(T obj) {
		this.obj = obj;
		this.initializedFields = new HashSet<String>();
	}
	/**
	 * Sets the given field to the given value.
	 * @param field field to set
	 * @param value value to set field to
	 * @throws RuntimeException if field has been set before or if setting this field fails for other reasons
	 */
	public void set(String field, Object value) {
		if (this.initializedFields.contains(field)) {
			throw new RuntimeException(String.format("Field %s already set!", field));
		} else {
			try {
				obj.getClass().getField(field).set(obj, value);
				this.initializedFields.add(field);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
	}
	/**
	 * Returns the object which was passed in the constructor. Before that, checks that every field has been set.
	 * @return the object which was passed in the constructor
	 * @throws RuntimeException if one field was not set
	 */
	public T getResult() {
		for (Field f : obj.getClass().getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers()) && !initializedFields.contains(f.getName())) {
				throw new RuntimeException(String.format("Field %s not initialized!", f.getName()));
			}
		}
		return obj;
	}
}
