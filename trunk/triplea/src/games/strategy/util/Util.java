/*
 * Util.java
 *
 * Created on November 13, 2001, 1:57 PM
 */

package games.strategy.util;

import java.util.*;

/**
 * Some utility methods for dealing with collections.
 * 
 * @author  Sean Bridges
 * @version 1.0
 */
public class Util 
{
	/**
	 * return a such that a exists in c1 and a exists in c2.
	 * always returns a new collection.
	 */
	public static List intersection(Collection c1, Collection c2)
	{
		if(c1 == null || c2 == null)
			return Collections.EMPTY_LIST;
		if(c1.size() == 0 || c2.size() == 0)
			return Collections.EMPTY_LIST;

		List intersection = new ArrayList();		
		Iterator iter = c1.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if(c2.contains(current))
				intersection.add(current);
		}
		return intersection;
	}

	/**
	 * Returns a such that a exists in c1 but not in c2.
	 * Always returns a new collection.
	 */
	public static List difference(Collection c1, Collection c2)
	{
		if(c1 == null || c1.size() == 0)
			return Collections.EMPTY_LIST;
		if(c2 == null || c2.size() == 0)
			return new ArrayList(c1);
		
		List difference = new ArrayList();
		Iterator iter = c1.iterator();
		while(iter.hasNext())
		{
			Object current = iter.next();
			if(!c2.contains(current))
				difference.add(current);
		}
		return difference;
	}
	
	/**
	 * true if for each a in c1, a exists in c2, 
	 * and if for each b in c2, b exist in c1
	 * and c1 and c2 are the same size.
	 * Note that (a,a,b) (a,b,b) are equal.
	 */
	public static boolean equals(Collection c1, Collection c2)
	{
		if(c1 == null || c2 == null)
			return c1 == c2;
		
		if(c1.size() != c2.size() )
			return false;
		
		if(c1 == c2)
			return true;
		
		if(!c1.containsAll(c2))
			return false;
		
		if(!c2.containsAll(c1))
			return false;
		
		return true;
	}
	
	public static List toList(Object[] objects)
	{
		ArrayList list = new ArrayList(objects.length);
		for(int i = 0; i < objects.length; i++)
		{
			list.add(objects[i]);
		}
		return list;
	}
	
	
	
	/** Creates new Util */
    private Util() 
	{
    }

}
