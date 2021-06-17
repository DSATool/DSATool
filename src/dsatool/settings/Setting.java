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
package dsatool.settings;

import java.util.Arrays;
import java.util.Objects;

public abstract class Setting {
	protected final String name;
	protected final String[] path;

	public Setting(final String name, final String[] path) {
		this.name = name;
		this.path = path;
	}

	public abstract void create(SettingsPage page);

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) return true;
		if ((obj == null) || !(obj instanceof Setting)) return false;
		final Setting other = (Setting) obj;
		if (!Objects.equals(name, other.name)) {
			return false;
		}
		if (!Arrays.equals(path, other.path)) return false;
		return true;
	}

	public String[] getPath() {
		return path;
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
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(path);
		return result;
	}
}
