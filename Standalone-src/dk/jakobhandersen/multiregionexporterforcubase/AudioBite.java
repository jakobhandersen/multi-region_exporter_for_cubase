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
 * Class that represents one audio region that is to be copied from the full file to its own file.
 * @author Jakob Hougaard Andersen.
 */
public class AudioBite
{
	/**
	 * Start time in seconds
	 */
	private double start;
	
	/**
	 * Length of the AudioBite.
	 * Note that the length can be described in different time formats (TimeFormat) defined in lengthFormat.
	 */
	private double length;
	
	/**
	 * The time format of the length variable
	 */
	private TimeFormat lengthFormat = TimeFormat.NONE;
	
	/**
	 * End time in seconds. Calculated from start and length.
	 */
	private double end;
	
	/**
	 * The actually used value for end when creating audio files and range markers.
	 * This value includes trailing time and is clamped to audio file length.
	 */
	private double functionalEnd;
	
	/**
	 * The name of the AudioBite. Will also be the filename (without file
	 * ending).
	 */
	private String name = "untitled";
	
	/**
	 * Indicates whether the name has been set normally (from element
	 * description/name). This is used to determine whether it is allowed to
	 * override name with audio file name (fall-back if no name/description).
	 */
	private boolean nameSetNormally = false;
	
	/**
	 * Has the start time been set?
	 */
	private boolean startSet = false;
	
	/**
	 * Has the end time been calculated (from start and length) ?
	 */
	private boolean endCalculated = false;
	
	/**
	 * Has functionalEnd been set?
	 */
	private boolean functionalEndSet = false;
	
	
	
	
	/**
	 * @param start start time in seconds
	 */
	public void setStart(double start)
	{
		this.start = start;
		startSet = true;
	}
	
	/**
	 * @param length length value
	 * @param format the TimeFormat of the specified length value
	 */
	public void setLength(double length, TimeFormat format)
	{
		this.length = length;
		lengthFormat = format;
	}
	
	
	/**
	 * Calculates the end time of this AudioBite on the basis of start, length and lengthFormat
	 * @param sampleRate the sample rate of the Cubase project (defined in track XML)
	 */
	public void calculateEnd(double sampleRate)
	{
		switch(lengthFormat)
		{
		case SECONDS:
			end = start + length;
			endCalculated = true;
			break;
			
		case MIDI_TICKS:
			end = start + (length/Constants.midiTicksPerSec);
			endCalculated = true;
			break;
			
		case SAMPLES:
			end = start + (length/sampleRate);
			endCalculated = true;
			break;
			
		default:
			//Error. Somehow unknown format or NONE
			break;
		}
	}
	
	/**
	 * @param endTime the functional end time (including trailing time and clamped to length of audio file).
	 */
	public void setFunctionalEnd(double endTime)
	{
		functionalEndSet = true;
		functionalEnd = endTime;
	}
	
	
	public double getFunctionalEnd()
	{
		if (! functionalEndSet)
		{
			Debug.log("Error! GetFunctionalEnd() called but functionalEnd is not set");
		}
		return functionalEnd;
	}
	
	/**
	 * Sets the name of this AudioBite.
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = Utils.getValidFileNameString(name);
		nameSetNormally = true;
		//Debug.Log("Name set: "+ name);
	}
	
	/**
	 * Sets the name of this AudioBite to the name of an audio file, but only if the name has not already been set by SetName()
	 * @param name the name of relevant audio file
	 */
	public void setNameFromAudioFileName(String name)
	{
		if (! nameSetNormally)
		{
			this.name = Utils.getValidFileNameString(name);
		}
	}
	
	/**
	 * @return true if this AudioBite is properly set up
	 */
	public boolean isSetup()
	{
		return (startSet && endCalculated);
	}
	
	/**
	 * @return start time in seconds
	 */
	public double getStart()
	{
		return start;
	}
	
	/**
	 * @return end time in seconds
	 */
	public double getEnd()
	{
		return end;
	}
	
	/**
	 * @return name of this AudioBite
	 */
	public String getName()
	{
		return name;
	}
	

	
	@Override
	public String toString()
	{
		return "AudioBite: name = " + name + ", start = "+start +", end = "+end+", functionalEnd = "+functionalEnd+", length = "+(functionalEnd-start);
	}
	
}
