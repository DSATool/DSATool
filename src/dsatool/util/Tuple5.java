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

import java.util.Objects;

/**
 * A handy class for two typed values
 *
 * @author Dominik Helm
 * @param <T1>
 *            The type of the first value
 * @param <T2>
 *            The type of the second value
 * @param <T3>
 *            The type of the third value
 * @param <T4>
 *            The type of the fourth value
 * @param <T5>
 *            The type of the fifth value
 */
public class Tuple5<T1, T2, T3, T4, T5> {
	/**
	 * The first value
	 */
	public T1 _1;

	/**
	 * The second value
	 */
	public T2 _2;

	/**
	 * The third value
	 */
	public T3 _3;

	/**
	 * The fourth value
	 */
	public T4 _4;

	/**
	 * The fifth value
	 */
	public T5 _5;

	/**
	 * Constructor taking two initial values
	 *
	 * @param _1
	 *            First initial value
	 * @param _2
	 *            Second initial value
	 * @param _3
	 *            Third initial value
	 * @param _4
	 *            Fourth initial value
	 * @param _4
	 *            Fifth initial value
	 */
	public Tuple5(final T1 _1, final T2 _2, final T3 _3, final T4 _4, final T5 _5) {
		this._1 = _1;
		this._2 = _2;
		this._3 = _3;
		this._4 = _4;
		this._5 = _5;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof final Tuple5<?, ?, ?, ?, ?> other)) return false;
		if (!Objects.equals(_1, other._1)) return false;
		if (!Objects.equals(_2, other._2)) return false;
		if (!Objects.equals(_3, other._3)) return false;
		if (!Objects.equals(_4, other._4)) return false;
		if (!Objects.equals(_5, other._5)) return false;
		return true;

	}

	/**
	 * Access the values by index (not typesafe)
	 *
	 * @param index
	 *            The index to access
	 * @return The value at the index or null if the index is outside of 0 < index <= 5
	 */
	public Object get(final int index) {
		return switch (index) {
			case 1 -> _1;
			case 2 -> _2;
			case 3 -> _3;
			case 4 -> _4;
			case 5 -> _5;
			default -> null;
		};
	}

	/**
	 * Access the first value in a typesafe way
	 *
	 * @return The first value
	 */
	public T1 get_1() {
		return _1;
	}

	/**
	 * Access the second value in a typesafe way
	 *
	 * @return The second value
	 */
	public T2 get_2() {
		return _2;
	}

	/**
	 * Access the third value in a typesafe way
	 *
	 * @return The third value
	 */
	public T3 get_3() {
		return _3;
	}

	/**
	 * Access the fourth value in a typesafe way
	 *
	 * @return The fourth value
	 */
	public T4 get_4() {
		return _4;
	}

	/**
	 * Access the fifth value in a typesafe way
	 *
	 * @return The fifth value
	 */
	public T5 get_5() {
		return _5;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (_1 == null ? 0 : _1.hashCode());
		result = prime * result + (_2 == null ? 0 : _2.hashCode());
		result = prime * result + (_3 == null ? 0 : _3.hashCode());
		result = prime * result + (_4 == null ? 0 : _4.hashCode());
		result = prime * result + (_5 == null ? 0 : _5.hashCode());
		return result;
	}
}
