/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * List operation related utility methods.
 * 
 * @author Tran Nam Quang
 */
public class UtilList {
	
	public interface Equality <S, T> {
		/**
		 * Returns true if <tt>obj1</tt> corresponds to <tt>obj2</tt>.
		 */
		public boolean equals(S obj1, T obj2);
	}
	
	public interface Selector<T> {
		/**
		 * Returns whether the given object should be selected.
		 */
		public boolean select(T obj);
	}

	/**
	 * Static use only.
	 */
	private UtilList() {
	}

	/**
	 * Returns true if each element in the first set has a corresponding element
	 * in the second set. The correspondence is based on the output of the given
	 * <tt>equality</tt> parameter: If <tt>equality.equals(obj1, obj2)</tt>
	 * returns true, <tt>obj1</tt> and <tt>obj2</tt> correspond to one another.
	 */
	public static <S, T> boolean isMap(S[] set1, T[] set2, UtilList.Equality<S, T> equality) {
		if (set1.length == 0 && set2.length == 0) return true;
		if (set1.length != set2.length) return false;
		List<S> list1 = new ArrayList<S> (set1.length);
		for (S obj1 : set1)
			list1.add(obj1);
		for (T obj2 : set2) {
			S removeFromList = null;
			for (S obj1 : list1)
				if (equality.equals(obj1, obj2)) {
					removeFromList = obj1;
					break;
				}
			if (removeFromList != null)
				list1.remove(removeFromList);
		}
		return list1.isEmpty();
	}

	/**
	 * Creates a single string from the integer array by converting the integers
	 * to strings and concatenating these strings using <code>separator</code>
	 * as the separator character, which usually is a ", " or a line separator.
	 */
	public static String toString(String separator, int... intArray) {
		if (intArray.length == 0) return ""; //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		for (int i : intArray)
			sb.append(i).append(separator);
		return sb.substring(0, sb.length() - separator.length());
	}

	/**
	 * Creates a single string from the given array of objects by first
	 * converting them using the toString() method, then by concatenating the
	 * strings, with <code>separator</code> as the separator character, which
	 * usually is a ", " or a line separator.
	 */
	public static <T> String toString(String separator, T... objects) {
		if (objects.length == 0) return ""; //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		for (Object object : objects)
			sb.append(object.toString()).append(separator);
		return sb.substring(0, sb.length() - separator.length());
	}

	/**
	 * Creates a single string from the given list of objects by first
	 * converting them using the toString() method, then by concatenating the
	 * strings, with <code>separator</code> as the separator character, which
	 * usually is a ", " or a line separator.
	 */
	public static String toString(String separator, Collection<? extends Object> list) {
		if (list.size() == 0) return ""; //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		for (Object object : list)
			sb.append(object.toString()).append(separator);
		return sb.substring(0, sb.length() - separator.length());
	}
	
	/**
	 * Creates a list for the given array.
	 */
	public static <T> List<T> toList(T... objects) {
		List<T> list = new ArrayList<T> (objects.length);
		for (T t : objects)
			list.add(t);
		return list;
	}
	
	/**
	 * Removes all entries in <tt>objects</tt> that are selected by the given
	 * <tt>Selector</tt>. The provided collection will not change during calls
	 * to the <tt>Selector</tt>, so it's safe to access it inside
	 * <tt>Selector.select</tt>.
	 */
	public static <T> void remove(Collection<T> objects, Selector<T> selector) {
		List<T> toRemove = new ArrayList<T> (objects.size());
		for (T candidate : objects)
			if (selector.select(candidate))
				toRemove.add(candidate);
		objects.removeAll(toRemove);
	}
	
	/**
	 * Returns true if <tt>objects</tt> contains <tt>object</tt>.
	 */
	public static boolean containsIdentity(Object[] objects, Object object) {
		for (Object candidate : objects)
			if (candidate == object)
				return true;
		return false;
	}
	
	/**
	 * Returns true if <tt>objects</tt> contains <tt>object</tt>.
	 */
	public static boolean containsIdentity(Collection<?> objects, Object object) {
		for (Object candidate : objects)
			if (candidate == object)
				return true;
		return false;
	}

	/**
	 * Returns true if <tt>objects</tt> contains an object that is equal to
	 * <tt>object</tt>.
	 */
	public static boolean containsEquality(Object[] objects, Object object) {
		for (Object candidate : objects)
			if (candidate.equals(object))
				return true;
		return false;
	}
	
	/**
	 * Returns true if <tt>objects</tt> contains an object that is equal to
	 * <tt>object</tt>.
	 */
	public static boolean containsEquality(Collection<?> objects, Object object) {
		for (Object candidate : objects)
			if (candidate.equals(object))
				return true;
		return false;
	}
	
	/**
	 * Returns whether <tt>candidates</tt> contains an entry that is equal to
	 * <tt>target</tt>. Whether two objects are equal is defined by the
	 * <tt>Equality</tt> parameter.
	 */
	public static <S, T> boolean containsEquality(S[] candidates, T target, Equality<S, T> equality) {
		for (S candidate : candidates)
			if (equality.equals(candidate, target))
				return true;
		return false;
	}
	
	/**
	 * Returns whether <tt>candidates</tt> contains an entry that is equal to
	 * <tt>target</tt>. Whether two objects are equal is defined by the
	 * <tt>Equality</tt> parameter.
	 */
	public static <S, T> boolean containsEquality(Collection<S> candidates, T target, Equality<S, T> equality) {
		for (S candidate : candidates)
			if (equality.equals(candidate, target))
				return true;
		return false;
	}
	
	/**
	 * Returns all elements that are in <tt>l1</tt> but not in <tt>l2</tt> as
	 * a list.
	 */
	public static <T> List<T> subtract(Collection<T> l1, Collection<T> l2) {
		List<T> newList = new ArrayList<T> (l1);
		newList.removeAll(l2);
		return newList;
	}

	/**
	 * Splits the given string into an array of substrings using <tt>sep</tt>
	 * as the separator sequence. This differs from the standard
	 * String.split(String) in that no regular expressions are used. If the
	 * <tt>noEmptyEnds</tt> parameter is true, empty strings at the start and
	 * end of the returned string array will be omitted.
	 */
	public static String[] split(String string, String sep, boolean noEmptyEnds) {
		List<String> parts = new ArrayList<String> ();
		int startIndex = 0;
		while (true) {
			int index = string.indexOf(sep, startIndex);
			if (index == -1) {
				parts.add(string.substring(startIndex));
				break;
			}
			parts.add(string.substring(startIndex, index));
			startIndex = index + 1;
		}
		if (noEmptyEnds) {
			while (! parts.isEmpty() && parts.get(0).equals("")) //$NON-NLS-1$
				parts.remove(0);
			while (! parts.isEmpty() && parts.get(parts.size() - 1).equals("")) //$NON-NLS-1$
				parts.remove(parts.size() - 1);
		}
		return parts.toArray(new String[parts.size()]);
	}
	
	/**
	 * Reverses the elements in the given array.
	 */
	public static <T> void reverse(T... list) {
		int j = list.length - 1;
		for (int i = 0; i < list.length / 2; i++) {
			T temp = list[i];
			list[i] = list[j];
			list[j] = temp;
			j--;
		}
	}

}
