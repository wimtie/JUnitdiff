package nl.realworks.hudson.junitdiff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Doe diff tussen 2 collecties.
 * 
 * CollectionDiff
 *
 * @version $Id: CollectionDiff.java 90460 2009-04-17 15:14:51Z ronald $
 */
public class CollectionDiff<T> {

	private Collection<T> c1;
	private Collection<T> c2;
	
	public CollectionDiff(Collection<T> c1, Collection<T> c2) {
		this.c1 = c1;
		this.c2 = c2;
	}
	
	/**
	 * Items die alleen in lijst 1 zitten.
	 */
	public List<T> getItemsLeft() {
		List<T> itemsLeft = new ArrayList<T>();
		for (T o : c1) {
			if (!c2.contains(o)) {
				itemsLeft.add(o);
			}
		}
		return itemsLeft;
	}
	
	/**
	 * Items die alleen in lijst 2 zitten.
	 */
	public List<T> getItemsRight() {
		List<T> itemsRight = new ArrayList<T>();
		for (T o : c2) {
			if (!c1.contains(o)) {
				itemsRight.add(o);
			}
		}
		return itemsRight;
	}

}
