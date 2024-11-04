//    Multi-region Exporter - for Cubase
//    By Jakob Hougaard Andersen
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
 * Class that holds constants used in the project
 * @author Jakob Hougaard Andersen
 *
 */
public class Constants 
{
	/**
	 * The number of midi ticks per second per BPM – To convert from midi ticks to seconds (some time values are given in MIDI ticks).
	 * In earlier versions of the Exporter, it was assumed that there are always 960 midi ticks per seconds, but in reality it depends on the current BPM.
	 * So now the value is given in this format.
	 */
	public static final double midiTicksPerSecPerBPM = 8;
	
	/**
	 * String representation of this version of the program
	 */
	public static final String versionString = "2.0";
	
	/**
	 * String representation of the build date of this version
	 */
	public static final String buildDateString = "2020-09-10";
}
