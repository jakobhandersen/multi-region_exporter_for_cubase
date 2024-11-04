//    Multi-region Exporter - for Cubase
//    Copyright (C) 2017 Jakob Hougaard Andsersen
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
	 * To calculate time i seconds. 
	 * Apparently, some time values are given in MIDI ticks. There are 960 ticks per second.
	 */
	public static final double midiTicksPerSec = 960;
	
	/**
	 * String representation of this version of the program
	 */
	public static final String versionString = "1.0.0.2";
	
	/**
	 * String representation of the build date of this version
	 */
	public static final String buildDateString = "2017-10-08";
}
