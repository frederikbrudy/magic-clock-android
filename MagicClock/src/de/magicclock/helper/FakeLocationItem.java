/**
 * MagicClock, a clock that displays people's locations
 * Copyright (C) 2012 www.magicclock.de
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * You can contact the authors via authors@magicclock.de or www.magicclock.de
 */
package de.magicclock.helper;

public class FakeLocationItem{
	public final String text;
	public final String value;
	public final int icon;
	
	public FakeLocationItem(String value, String text, Integer icon) {
		this.text = text;
		this.value = value;
		this.icon = icon;
	}
	
	public FakeLocationItem(String value, String text) {
		this(value, text, 0);
	}
	
	@Override
	public String toString() {
		return text;
	}
}