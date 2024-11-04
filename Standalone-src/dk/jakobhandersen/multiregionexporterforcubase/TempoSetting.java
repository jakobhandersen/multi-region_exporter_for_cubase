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

import java.util.*; 


/**
 * Represents the tempo settings found in a track XML file and contains functionality to convert midi ticks to seconds based on this information.
 * Added for version 1.0.0.4 as previously it was (wrongly) assumed that midi ticks always flow at a constant rate regardless of tempo.
 * @author Jakob Hougaard Andersen
 */
public class TempoSetting 
{
    
	/**
	 * The list of tempo events found in the XML track file
	 */
	private List<TempoEvent> tempoEvents;
	
	/**
	 * Rehearsal tempo found in the track XML file. Note that rehearsal tempo is the tempo used if tempo track functionality is switched off
	 */
	private double rehearsalTempo = 120;
	
	/**
	 * Has rehearsal tempo been set?
	 */
	private boolean rehearsalTempoSet = false;
	
	/**
	 * If 1, then tempo track functionality is switched off and we use rehearsal tempo
	 */
	private int rehearsalMode = 0;
	
	/**
	 * What TempoEvent is currently being set up (if any)
	 */
	private TempoEvent currentlySettingUpTempoEvent;
	
	/**
	 * Constructor
	 */
	public TempoSetting()
	{
		tempoEvents = new ArrayList<TempoEvent>();
	}
	
	/**
	 * @param tempo the 'rehearsal tempo' found in track XML file
	 */
	public void setRehearsalTempo(double tempo)
	{
		rehearsalTempo = tempo;
		rehearsalTempoSet = true;
		Debug.log("Rehearsal tempo set to "+ rehearsalTempo);
	}
	
	/**
	 * Sets 'reheasal mode'. If 1, then tempo track functionality is switched off and we use rehearsal tempo. If 0 then we use tempo track functionality (tempo events).
	 * @param mode 
	 */
	public void setRehearsalMode(int mode)
	{
		rehearsalMode = mode;
		Debug.log("Rehearsal mode set to "+ rehearsalMode);
	}
	
	/**
	 * Called when the parser starts parsing a new 'MTempoEvent'
	 */
	public void startSettingUpTempoEvent()
	{
		if (currentlySettingUpTempoEvent != null)
		{
			Debug.log("Error: startSettingUpTempoEvent was called while currentlySettingUpTempoEvent != null");
		}
		currentlySettingUpTempoEvent = new TempoEvent();
		Debug.log("Starting set up of new tempo event");
	}
	
	/**
	 * Set the BPM of the tempo event currently being set up
	 * @param bpm
	 */
	public void currentEventSetBPM(double bpm)
	{
	    if (currentlySettingUpTempoEvent == null)
        {
            Debug.log("Error: currentEventSetBPM was called while currentlySettingUpTempoEvent == null");
        } 
	    else
	    {
	        currentlySettingUpTempoEvent.setBpm(bpm);
	    }
	}
	
	/**
	 * Set the position (time) in midi ticks of the tempo event currently being set up
	 * @param midiTickPosition
	 */
	public void currentEventSetMidiTickPosition(double midiTickPosition)
    {
        if (currentlySettingUpTempoEvent == null)
        {
            Debug.log("Error: currentEventSetMidiTickPosition was called while currentlySettingUpTempoEvent == null");
        } 
        else
        {
            currentlySettingUpTempoEvent.setMidiTickPosition(midiTickPosition);
        }
    }
	
	/**
	 * Set whether the tempo is ramping (<int name="Func" value="1"/>') to the tempo event currently being set up. 
	 * @param ramp 
	 */
	public void currentEventSetRamp(int ramp)
    {
        if (currentlySettingUpTempoEvent == null)
        {
            Debug.log("Error: currentEventSetRamp was called while currentlySettingUpTempoEvent == null");
        } 
        else
        {
            currentlySettingUpTempoEvent.setRamp(ramp);
        }
    }
	
	/**
	 * Called when the parser ends parsing an 'MTempoEvent'
	 */
	public void endSettingUpTempoEvent()
	{
		if (currentlySettingUpTempoEvent == null)
		{
			Debug.log("Error: endSettingUpTempoEvent was called while currentlySettingUpTempoEvent == null");
		}
		else if (!currentlySettingUpTempoEvent.isSetUpFromXML())
		{
			Debug.log("Error: endSettingUpTempoEvent was called but currentlySettingUpTempoEvent is not completely set up");
		}
		else
		{
			tempoEvents.add(currentlySettingUpTempoEvent);
		}
		currentlySettingUpTempoEvent = null;
		Debug.log("Ended set up of new tempo event");
	}
	
	/**
	 * Calculates the actual position in seconds for the individual tempo events
	 */
	public void finalizeSetting()
	{
	   if (this.rehearsalMode == 0)
	   {
	       double lastEventSec = 0;
	       double lastEventTicks = 0;
	       double lastEventBpm = 120;
	       for (int i = 0; i < this.tempoEvents.size(); i++)
	       {
	           TempoEvent t = this.tempoEvents.get(i);
	           
	           if (i > 0)//if this is not the first event
	           {
	               double deltaTicks = t.getMidiTickPosition() - lastEventTicks;
	               double deltaSec = 0;
	               
	               if (t.getRamp() == 1)//If we are ramping to this event
	               {
	                   //A bit more complicated here...
	                   
	                   
	                   double v0 = lastEventBpm * Constants.midiTicksPerSecPerBPM;//Velocity (midi ticks per second) at last event
	                   double v = t.getBpm() * Constants.midiTicksPerSecPerBPM;//Velocity at this event
	                   
	                   
	                   
	                   //The velocity (BPM) changes at a rate proportional to its current velocity.
	                   //It looks linear in Cubase, but that is because it itself stretches/squeezes the time axis.
	                   //So it is linear in a (natural) logarithmic sense.
	                   //We use a transformation of the formula:
	                   //v=v0*e^(b*t)
	                   //Where v is the end velocity (at this event), v0 is the velocity at last event, t is time in seconds and b is (v-v0)/deltaTicks
	                   if (v != v0)//Check if the speed is the same as last event (we can't divide by zero)
	                   {
	                       deltaSec = (Math.log(v/v0)*deltaTicks)/(v-v0);
	                   }
	                   else//Just treat this as if we don't ramp
	                   {
	                       deltaSec =  deltaTicks / (lastEventBpm * Constants.midiTicksPerSecPerBPM);
	                   }
	                   //Debug.log("v0: "+v0+", v:"+v+", deltaSec:"+deltaSec);
	               }
	               else
	               {
	                   deltaSec =  deltaTicks / (lastEventBpm * Constants.midiTicksPerSecPerBPM);
	               } 
	               
	               double sec = lastEventSec + deltaSec;
                   t.setSecPosition(sec);
	           }
	           else//If this is the first event
	           {
	               if (t.getMidiTickPosition() != 0)//Should always be 0 on first event. Just check to be sure...
	               {
	                   Debug.log("Error: midi tick position of first tempo event was not zero!");
	               }
	               t.setSecPosition(0);
	           }
	           
	           //Set last values
	           lastEventBpm = t.getBpm();
               lastEventTicks = t.getMidiTickPosition();
               lastEventSec = t.getSecPosition();
               //Debug.log(lastEventSec+"");
	       }
	   }
	}
	
	/**
	 * Transforms a position in time defined in midi ticks to a position in time defined in seconds
	 * @param midiTicks
	 * @return the position in time defined in seconds
	 */
	public double midiTickPositionToSeconds(double midiTicks)
	{
	    if (this.rehearsalMode == 1)//If we don't use tempo track (= constant tempo)
	    {
	        return midiTicks/(this.rehearsalTempo * Constants.midiTicksPerSecPerBPM);
	    }
	    else if (this.tempoEvents.size() > 0)
	    {
	        //Find the index of the first event later than midiTicks
	        boolean foundLater = false;
	        int laterIndex = 0;
	        for (int i = 0; i < this.tempoEvents.size(); i++)
	        {
	            if (this.tempoEvents.get(i).getMidiTickPosition() > midiTicks)
	            {
	                laterIndex = i;
	                foundLater = true;
	                break;
	            }
	        }
	        if (!foundLater)//If midiTicks is later than any event
	        {
	            TempoEvent lastEvent = this.tempoEvents.get(this.tempoEvents.size()-1);
	            double deltaTicks = midiTicks - lastEvent.getMidiTickPosition();
	            double deltaSec = deltaTicks/(lastEvent.getBpm() * Constants.midiTicksPerSecPerBPM);//The tempo will stay at tempo of last event
	            return lastEvent.getSecPosition()+deltaSec;
	        }
	        else if (laterIndex > 0)
	        {
	            //The position is somewhere between fromEvent and toEvent (can be straight at fromEvent)
	            TempoEvent fromEvent = this.tempoEvents.get(laterIndex-1);
	            TempoEvent toEvent = this.tempoEvents.get(laterIndex);
	            double deltaTicksFromFromEvent = midiTicks - fromEvent.getMidiTickPosition();
	            if (deltaTicksFromFromEvent > 0)
	            {
	                if (toEvent.getRamp() == 0)//If the tempo jumps to toEvent (no ramping)
	                {
	                    return fromEvent.getSecPosition() + (deltaTicksFromFromEvent/(fromEvent.getBpm()*Constants.midiTicksPerSecPerBPM));
	                }
	                else//If the tempo ramps to toEvent
	                {
	                    //Ok, so here it gets a bit more complicated again...
	                    //We have the formula v=v0*e^(b*t) that we used when calculating the time in seconds of the tempo events
	                    //If we integrate that we get: 
	                    //s = (v0*e^(b*t))/b + constant 
	                    //where s is the distance traveled in ticks.
	                    //Now since we would want the distance traveled at time=0 to be 0, we set the constant to be -v0/b
	                    //So with that put in (and s replaced by ticks) we get:
	                    //ticks = (v0*e^(b*t))/b -v0/b
	                    //Isolating t we get:
	                    //t=log((b*ticks+v0)/v0)/b
	                    //And remember that b is (v-v0)/deltaTicks, where deltaTicks is the full amount of ticks between the events
	                    double ticksBetweenEvents = toEvent.getMidiTickPosition()-fromEvent.getMidiTickPosition();//deltaTicks in above formula
	                    double v0 = fromEvent.getBpm() * Constants.midiTicksPerSecPerBPM;//Ticks per second at fromEvent
	                    double v = toEvent.getBpm() * Constants.midiTicksPerSecPerBPM;//Ticks per second at toEvent
	                    double deltaSec;
	                    if (v != v0)//Check if the speed is the same at fromEvent and toEvent (we can't divide by zero)
	                    {
	                        double b = (v-v0)/ticksBetweenEvents;
	                        deltaSec = Math.log((b*deltaTicksFromFromEvent+v0)/v0)/b;
	                    }
	                    else//Just treat this as if we don't ramp
	                    {
	                        deltaSec = deltaTicksFromFromEvent/(fromEvent.getBpm()*Constants.midiTicksPerSecPerBPM);
	                    }
	                    
	                    return fromEvent.getSecPosition() + deltaSec;
	                }
	            }
	            else//If the position is straight at fromEvent
	            {
	                return fromEvent.getSecPosition();
	            }
	        }
	        else//Should never be the case since first event will be at zero
	        {
	            return 0;
	        }
	    }
	    else//Error
	    {
	        return 0;
	    }
	}
	
	
	/**
	 * Transforms a length in time defined by midi ticks to a length in time defined in seconds.
	 * Note that the resulting length in seconds depends on from where we start (because tempo may vary) - hence the midiTickStartPos parameter.
	 * @param midiTickLength
	 * @param midiTickStartPos
	 * @return The length in time defined in seconds
	 */
	public double midiTickLengthToSeconds(double midiTickLength, double midiTickStartPos)
	{
	    return (midiTickPositionToSeconds(midiTickStartPos+midiTickLength)-midiTickPositionToSeconds(midiTickStartPos));
	}
	
	/**
	 * Has this TempoEvent been populated with the necessary information from the track XML file?
	 * @return true if this TempoSetting has all necessary information from the track XML file
	 */
	public boolean isSetUpFromXML()
	{
	    if (this.rehearsalMode == 0)
	    {
	         return this.tempoEvents.size() > 0;  
	    }
	    else
	    {
	        return this.rehearsalTempoSet;
	    }
	}
}
