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
import java.util.*;

/**
 * Comparator to be used when sorting a list of AudioBites based on their start time
 * @author Jakob Hougaard Andersen
 *
 */
public class AudioBiteStartComparator implements Comparator<AudioBite>
{

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(AudioBite arg0, AudioBite arg1) 
	{
		return Double.compare(arg0.getStart(),arg1.getStart());
	}

}
