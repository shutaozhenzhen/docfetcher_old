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
 * List and string operation related utility methods.
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
	
	/**
	 * Converts the given raw regex string into an array of valid regular
	 * expressions. The raw regex string is expected to consist of one or more
	 * regular expressions separated by whitespace and/or a '$' character.
	 */
	public static String[] parseExclusionString(String str) {
		int first = 0;
		int last = str.length() - 1;
		for (int i = 0; i < str.length(); i++) {
			if (Character.isWhitespace(str.charAt(i)) || str.charAt(i) == '$')
				first += 1;
			else break;
		}
		for (int i = last; i >= 0; i--) {
			if (Character.isWhitespace(str.charAt(i)) || str.charAt(i) == '$')
				last -= 1;
			else break;
		}
		if (first == str.length() || last == -1)
			return new String[0];
		return str.substring(first, last + 1).split("\\s*\\$+\\s*"); //$NON-NLS-1$
	}

	/**
	 * Shortens the given string if its length exceeds a fixed limit.
	 */
	public static String truncate(String str) {
		if (str.length() > 32)
			return str.substring(0, 32) + "..."; //$NON-NLS-1$
		return str;
	}

	/**
	 * Converts the given period of time in milliseconds into something more
	 * human-friendly (e.g. "1 h 24 min 3 s").
	 */
	public static String simpleDuration(long millis) {
		int secs = (int) (millis / 1000);
		int hrs = secs / 3600;
		secs -= hrs * 3600;
		int mins = secs / 60;
		secs -= mins * 60;
		String ret = ""; //$NON-NLS-1$
		if (hrs != 0) ret += hrs + " h"; //$NON-NLS-1$
		if (mins != 0) ret += (hrs == 0 ? "" : " ") + mins + " min"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (secs != 0) ret += (hrs == 0 && mins == 0 ? "" : " ") + secs + " s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (ret.equals("")) //$NON-NLS-1$
			return "0 s"; //$NON-NLS-1$
		return ret;
	}

	/**
	 * Returns whether the given target string starts with one of the given prefixes.
	 */
	public static boolean startsWith(String target, String... prefixes) {
		for (String prefix : prefixes)
			if (target.startsWith(prefix))
				return true;
		return false;
	}

}
