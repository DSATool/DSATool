/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dsatool.util;

import java.util.Collection;
import java.util.function.Function;

import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class StringUtil {

	public static <T> String mkString(final Collection<T> collection, final String separator) {
		final StringBuilder result = new StringBuilder();
		for (final T item : collection) {
			final String toAppend = item.toString();
			if (!toAppend.isEmpty()) {
				result.append(toAppend);
				result.append(separator);
			}
		}
		if (!result.isEmpty())
			return result.substring(0, result.length() - separator.length());
		else
			return "";
	}

	public static <T> String mkString(final Collection<T> collection, final String separator, final Function<T, T> map) {
		return mkString(collection.stream().map(map).toList(), separator);
	}

	public static <T> String mkString(final Collection<T> collection, final String start, final String separator, final String end) {
		if (collection.isEmpty())
			return "";
		else
			return start + mkString(collection, separator) + end;
	}

	public static <T> String mkString(final Collection<T> collection, final String start, final String separator, final String end, final Function<T, T> map) {
		return mkString(collection.stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkString(final JSONObject obj, final String separator, final Function<String, T> map) {
		return mkString(obj.keySet().stream().map(map).toList(), separator);
	}

	public static <T> String mkString(final JSONObject obj, final String start, final String separator, final String end, final Function<String, T> map) {
		return mkString(obj.keySet().stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkStringArr(final JSONArray arr, final String separator, final Function<JSONArray, T> map) {
		return mkString(arr.getArrs().stream().map(map).toList(), separator);
	}

	public static <T> String mkStringArr(final JSONArray arr, final String start, final String separator, final String end, final Function<JSONArray, T> map) {
		return mkString(arr.getArrs().stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkStringBool(final JSONArray arr, final String separator) {
		return mkString(arr.getBools(), separator);
	}

	public static <T> String mkStringBool(final JSONArray arr, final String separator, final Function<Boolean, T> map) {
		return mkString(arr.getBools().stream().map(map).toList(), separator);
	}

	public static <T> String mkStringBool(final JSONArray arr, final String start, final String separator, final String end) {
		return mkString(arr.getBools(), start, separator, end);
	}

	public static <T> String mkStringBool(final JSONArray arr, final String start, final String separator, final String end, final Function<Boolean, T> map) {
		return mkString(arr.getBools().stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkStringDouble(final JSONArray arr, final String separator) {
		return mkString(arr.getDoubles(), separator);
	}

	public static <T> String mkStringDouble(final JSONArray arr, final String separator, final Function<Double, T> map) {
		return mkString(arr.getDoubles().stream().map(map).toList(), separator);
	}

	public static <T> String mkStringDouble(final JSONArray arr, final String start, final String separator, final String end) {
		return mkString(arr.getDoubles(), start, separator, end);
	}

	public static <T> String mkStringDouble(final JSONArray arr, final String start, final String separator, final String end, final Function<Double, T> map) {
		return mkString(arr.getDoubles().stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkStringInt(final JSONArray arr, final String separator) {
		return mkString(arr.getInts(), separator);
	}

	public static <T> String mkStringInt(final JSONArray arr, final String separator, final Function<Long, T> map) {
		return mkString(arr.getInts().stream().map(map).toList(), separator);
	}

	public static <T> String mkStringInt(final JSONArray arr, final String start, final String separator, final String end) {
		return mkString(arr.getInts(), start, separator, end);
	}

	public static <T> String mkStringInt(final JSONArray arr, final String start, final String separator, final String end, final Function<Long, T> map) {
		return mkString(arr.getInts().stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkStringObj(final JSONArray arr, final String separator, final Function<JSONObject, T> map) {
		return mkString(arr.getObjs().stream().map(map).toList(), separator);
	}

	public static <T> String mkStringObj(final JSONArray arr, final String start, final String separator, final String end, final Function<JSONObject, T> map) {
		return mkString(arr.getObjs().stream().map(map).toList(), start, separator, end);
	}

	public static <T> String mkStringString(final JSONArray arr, final String separator) {
		return mkString(arr.getStrings(), separator);
	}

	public static <T> String mkStringString(final JSONArray arr, final String separator, final Function<String, T> map) {
		return mkString(arr.getStrings().stream().map(map).toList(), separator);
	}

	public static <T> String mkStringString(final JSONArray arr, final String start, final String separator, final String end) {
		return mkString(arr.getStrings(), start, separator, end);
	}

	public static <T> String mkStringString(final JSONArray arr, final String start, final String separator, final String end, final Function<String, T> map) {
		return mkString(arr.getStrings().stream().map(map).toList(), start, separator, end);
	}

	private StringUtil() {}
}
