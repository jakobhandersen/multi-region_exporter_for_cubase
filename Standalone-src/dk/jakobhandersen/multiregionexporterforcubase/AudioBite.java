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
 * Class that represents one audio region that is to be copied from the full file to its own file.
 * @author Jakob Hougaard Andersen.
 */
public class AudioBite
{
	/**
	 * Start time value.
	 * Note that the value can be described in different time formats (TimeFormat) defined in startValueFormat.
	 */
	private double startValue;
	
	/**
	 * The time format of the startValue variable
	 */
	private TimeFormat startValueFormat = TimeFormat.NONE;
	
	/**
	 * Length value.
	 * Note that the value can be described in different time formats (TimeFormat) defined in lengthValueFormat.
	 */
	private double lengthValue;
	
	/**
	 * The time format of the lengthValue variable
	 */
	private TimeFormat lengthValueFormat = TimeFormat.NONE;
	
	
	/**
	 * Start time in seconds. Calculated from startValue.
	 */
	private double startSec;
	
	/**
	 * End time in seconds. Calculated from startValue and lengthValue.
	 */
	private double endSec;
	
	/**
	 * The actually used value in seconds for end when creating audio files and range markers.
	 * This value includes trailing time and is clamped to audio file length.
	 */
	private double functionalEndSec;
	
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
	 * Has the start value been set?
	 */
	private boolean startValueSet = false;
	
	/**
	 * Has the length value been set?
	 */
	private boolean lengthValueSet = false;
	
	/**
	 * Has the start and end time in seconds been calculated ?
	 */
	private boolean startAndEndSecCalculated = false;
	
	/**
	 * Has functionalEnd been set?
	 */
	private boolean functionalEndSet = false;
	
	
	
	
	/**
	 * Calculates the start and end time of this AudioBite on the basis of startValue, startValueFormat, lengthValue and lengthValueFormat
	 * @param sampleRate the sample rate of the Cubase project (defined in track XML)
	 */
	public void calculateStartAndEndSec(double sampleRate, TempoSetting tempoSetting)
	{
		if (startValueSet && lengthValueSet)
		{
		    startAndEndSecCalculated = true;
		    
			//First calculate startSec
		    switch(startValueFormat)
	        {
	        case SECONDS:
	            this.startSec = this.startValue;
	            break;
	            
	        case MIDI_TICKS:
	            this.startSec = tempoSetting.midiTickPositionToSeconds(this.startValue);
	            break;
	            
	        case SAMPLES:
	            this.startSec = this.startValue /sampleRate;
	            break;
	            
	        default:
	            //Error. Somehow unknown format or NONE
	            startAndEndSecCalculated = false;
	            break;
	        }
			
			//Then calculate endSec
		    switch(lengthValueFormat)
	        {
	        case SECONDS:
	            endSec = startSec + lengthValue;
	            break;
	            
	        case MIDI_TICKS:
	            //The current method only works if startValue is also in midi tics when lengthValue is
	            if (this.startValueFormat == TimeFormat.MIDI_TICKS)
	            {
	                endSec = startSec + tempoSetting.midiTickLengthToSeconds(lengthValue, startValue);
	            }
	            else
	            {
	                Debug.log("Error: lenghtValue of AudioBite was defined in midi ticks but startValue was not");
	                startAndEndSecCalculated = false;
	            }
	            break;
	            
	        case SAMPLES:
	            endSec = startSec + (lengthValue/sampleRate);
	            break;
	            
	        default:
	            //Error. Somehow unknown format or NONE
	            startAndEndSecCalculated = false;
	            break;
	        }
			
		}
		
	}
	
	/**
	 * @return end time in seconds
	 */
	public double getEndSec()
	{
		return endSec;
	}
	
	
	public double getFunctionalEndSec()
	{
		if (! functionalEndSet)
		{
			Debug.log("Error! GetFunctionalEnd() called but functionalEnd is not set");
		}
		return functionalEndSec;
	}
	
	/**
	 * @return name of this AudioBite
	 */
	public String getName()
	{
		return name;
	}
	
	
	/**
	 * @return start time in seconds
	 */
	public double getStartSec()
	{
		return startSec;
	}
	
	/**
	 * @return true if this AudioBite is properly set up
	 */
	public boolean isSetup()
	{
		return (startAndEndSecCalculated);
	}
	
	/**
	 * @param endTime the functional end time in seconds (including trailing time and clamped to length of audio file).
	 */
	public void setFunctionalEndSec(double endTime)
	{
		functionalEndSet = true;
		functionalEndSec = endTime;
	}
	
	/**
	 * @param length length value
	 * @param format the TimeFormat of the specified length value
	 */
	public void setLengthValue(double length, TimeFormat format)
	{
		this.lengthValue = length;
		this.lengthValueFormat = format;
		this.lengthValueSet = true;
	}
	
	/**
	 * Sets the name of this AudioBite.
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = Utils.getValidFileNameString(name);
		this.nameSetNormally = true;
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
	 * @param startValue start time value
	 * @param format the TimeFormat in which the start value is specified
	 */
	public void setStartValue(double startValue, TimeFormat format)
	{
		this.startValue = startValue;
		this.startValueFormat = format;
		this.startValueSet = true;
		//Debug.log("Start value set to "+startValue+" in the format "+format.toString());
	}
	

	
	@Override
	public String toString()
	{
		return "AudioBite: name = " + name + ", startSec = "+startSec +", endSec = "+endSec+", functionalEndSec = "+functionalEndSec+", length = "+(functionalEndSec-startSec);
	}
	
}
