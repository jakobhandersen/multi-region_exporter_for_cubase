//    Multi-region Exporter - for Cubase
//    Copyright (C) 2016 Jakob Hougaard Andsersen
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package dk.jakobhandersen.multiregionexporterforcubase;

/**
 * Class that handles print calls for debugging
 * @author Jakob Hougaard Andersen
 *
 */
public class Debug 
{
	/**
	 * Is the debug printing enabled
	 */
	private static boolean enabled = true;
	
	/**
	 * Prints by System.out.println() if enabled
	 * @param text the text to print
	 */
	public static void log(String text)
	{
		if (enabled)
		{
			System.out.println(text);
		}
	}
}
