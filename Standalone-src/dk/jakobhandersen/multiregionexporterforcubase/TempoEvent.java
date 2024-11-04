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
 * Represents an individual 'MTempoEvent' as found in the track XML file. Used by TempoSetting class.
 * @author Jakob Hougaard Andersen
 */
public class TempoEvent 
{
	
	/**
	 * The position in time defined in midi ticks
	 */
	private double midiTickPosition;
	
	/**
	 * Has the midiTickPosition been set?
	 */
	private boolean midiTickPositionSet = false;
	
	/**
	 * The tempo (BPM) at this event
	 */
	private double bpm;
	
	/**
	 * Has bpm been set?
	 */
	private boolean bpmSet = false;
	
	/**
	 * Indicates whether the tempo is ramping (== 1) or jumping (==0) to this event.
	 * Set with <int name="Func" value="1"/> in the XML file
	 */
	private int ramp = 0;
	
	/**
	 * The position of this event in time defined in seconds.
	 * Will be calculated based on the other values and the previous events in TempoSetting 
	 */
	private double secPosition;

	

	/**
	 * @return the position in time of this event defined in midi ticks
	 */
	public double getMidiTickPosition() 
	{
		return midiTickPosition;
	}

	/**
	 * Set the position in time of this event as defined in midi ticks
	 * @param midiTickPosition
	 */
	public void setMidiTickPosition(double midiTickPosition) 
	{
		this.midiTickPosition = midiTickPosition;
		this.midiTickPositionSet = true;
	}

	/**
	 * @return tempo at this event (BPM)
	 */
	public double getBpm() 
	{
		return bpm;
	}

	
	/**
	 * Set the tempo (BPM) at this event
	 * @param bpm
	 */
	public void setBpm(double bpm) 
	{
		this.bpm = bpm;
		this.bpmSet = true;
	}

	/**
	 * @return 1 if tempo ramps to this event and 0 if it jumps
	 */
	public int getRamp() 
	{
		return ramp;
	}

	/**
	 * Set whether the tempo ramps to this event (1=ramp, 0=jump)
	 * @param ramp
	 */
	public void setRamp(int ramp) 
	{
		this.ramp = ramp;
	}

	/**
	 * @return the position in time of this event defined in seconds
	 */
	public double getSecPosition() 
	{
		return secPosition;
	}

	/**
	 * Set the position in time of this event defined in seconds
	 * @param secPosition
	 */
	public void setSecPosition(double secPosition) 
	{
		this.secPosition = secPosition;
	}
	
	/**
	 * @return true if this event has the necessary data input from the XML file.
	 * Note that this does not indicate whether secPosition has been set.
	 */
	public boolean isSetUpFromXML()
	{
		return midiTickPositionSet && bpmSet;
	}
	
}
