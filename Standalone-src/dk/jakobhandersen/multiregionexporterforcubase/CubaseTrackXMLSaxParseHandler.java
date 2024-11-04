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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extension of DefaultHandler to be used with SAXParser.
 * Parses the Cubase track XML file and creates AudioBite objects.
 * 
 * Note: I have no previous experience in working with SAX parsing, so this class might not be very elegant.
 * A future task will be to either review and refine this parser or to write another non-SAX parser.
 * 
 * Also, there is currently quite a lot of duplicate code for the different event types that could be encapsulated.
 * And furthermore an idea could be to change from first checking the qName of the element into first checking the relevant attribute.
 * 
 * Inspiration: http://www.journaldev.com/1198/java-sax-parser-example-tutorial-to-parse-xml-to-list-of-objects
 * Inspiration: http://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html
 * 
 * @author Jakob Hougaard Andersen
 *
 */
public class CubaseTrackXMLSaxParseHandler extends DefaultHandler
{
	/**
	 * The resulting list of AudioBites.
	 */
	private List<AudioBite> audioBites = null;
	
	/**
	 * Describes which track type (ElementType) we are currently within.
	 * Used for filtering out strange MAudioEvents that exist outside track elements (when using audio parts).
	 */
	private ElementType currentlyWithinTrack = ElementType.None;
	
	/**
	 * Describes which 'descendant level' in relation to the current track element we are on.
	 * Is used to determine when we are exiting the element. 
	 */
	private int subTrackNodeLevel = 0;
	
	/**
	 * Describes which (content-relevant) XML element type (ElementType) we are currently in.
	 */
	private ElementType currentlyParsingElement = ElementType.None;
	
	/**
	 * Describes which 'descendant level' in relation to the current relevant element we are on.
	 * Is used to determine when we are exiting the element.
	 */
	private int subElementNodeLevel = 0;
	
	/**
	 * The AudioBite we are currently creating.
	 */
	private AudioBite currentlySettingUpBite = null;
	
	/**
	 * Project sample rate (read from XML).
	 * To be used for calculating end times.
	 */
	private double sampleRate;
	
	/**
	 * Has the sample rate been read?
	 */
	private boolean sampleRateSet = false;
	
	/**
	 * The ID of the PAudioClip element we are currently parsing.
	 * Is only relevant if we are in fact currently parsing a PAudioClip element.
	 * Is used for handling naming fall-back to audio clip name.
	 * Note that only the first occurrence of a given audio clip obj node (@class = PAudioClip) will have other info than its ID - e.g. its name.
	 */
	private String currentPAudioClipID = "";
	
	/**
	 * Maps from audio clip ID to name.
	 * Note that only the first occurrence of a given audio clip obj node (@class = PAudioClip) will have other info than its ID - e.g. its name.
	 * The next ones (no class attribute) will just have the ID.
	 */
	private Map<String,String> audioClipIdNameMap = null;
	
	/**
	 * The number of audio bites that have been renamed during FinalizeAudioBites()
	 */
	private int numRenamedAudioBites = 0;
	
	
	/**
	 * The current domain type (time settings on track).
	 * 0 = musical and 1 = linear
	 */
	private int currentDomainType = 0;
	
	/**
	 * Are we currently inside element of type member with name "Domain"?
	 * This is used for determining track time settings (musical or linear)
	 */
	private int domainMemberSubMemberLevel = 0;
	
	
	private TempoSetting tempoSetting;
	
	
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException 
	{
		
	}
	
	@Override
	public void endDocument()
	{
	    if (tempoSetting.isSetUpFromXML())
	    {
	        tempoSetting.finalizeSetting();
	    }
	    else
	    {
	        Debug.log("Error: tempo setting is not properly set up from XML");
	    }
	    
	    
		if (sampleRateSet)
		{
			finalizeAudioBites();
			Debug.log("Number of properly set up audio bites = " +audioBites.size());
		}
		else
		{
			//Error no sample rate set
			Debug.log("Error: no sample rate set");
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException 
	{
		if (currentlyParsingElement != ElementType.None)
		{
			if (subElementNodeLevel == 0)
			{
				//End parsing event
				Debug.log("Ending parsing "+currentlyParsingElement.toString());
				if ((currentlyParsingElement != ElementType.MAudioPart) && (currentlyParsingElement != ElementType.PAudioClip))
				{
				    //If
				    if (currentlyParsingElement == ElementType.MTempoEvent)
				    {
				        tempoSetting.endSettingUpTempoEvent();
				    }
				    
				    
				    //If we are currently setting up an AudioBite (should not be the case if we are currently parsing a MTempoEvent) finish the setup
				    if (currentlySettingUpBite != null)
					{
						audioBites.add(currentlySettingUpBite);
					}
					currentlySettingUpBite = null;
					
					//Set currentlyParsingElement to null
					currentlyParsingElement = ElementType.None;
				}
				//MAudioPart and PAudioClip are special cases
				else
				{
					if (currentlyParsingElement == ElementType.MAudioPart)
					{
						currentlyParsingElement = ElementType.MAudioPartEvent;
						subElementNodeLevel = 0;
					}
					else if (currentlyParsingElement == ElementType.PAudioClip)
					{
						currentlyParsingElement = ElementType.MAudioEvent;
						subElementNodeLevel = 0;
					}
				}
			}
			else
			{
				subElementNodeLevel -= 1;
			}
		}
		
		if (domainMemberSubMemberLevel > 0)
		{
			domainMemberSubMemberLevel -= 1;
			//Debug.log("Setting domainMemberSubMemberLevel to "+domainMemberSubMemberLevel);
		}
		
		if (currentlyWithinTrack != ElementType.None)
		{
			if (subTrackNodeLevel == 0)
			{
				//Exiting track
				Debug.log("Exiting track "+ currentlyWithinTrack.toString());
				currentlyWithinTrack = ElementType.None;
			}
			else
			{
				subTrackNodeLevel -= 1;
			}
		}
	}
	
	/**
	 * Gets the AudioBites resulting from the parsing
	 * @return
	 */
	public List<AudioBite> getAudioBites()
	{
		return audioBites;
	}
	
	/**
	 * Returns the number of AudioBites that were renamed due to name equality
	 * @return
	 */
	public int getNumRenamedAudioBites()
	{
		return numRenamedAudioBites;
	}
	
	@Override
	public void startDocument()
	{
		audioBites = new ArrayList<AudioBite>();
		audioClipIdNameMap = new HashMap<String,String>(0);
		currentlySettingUpBite = null;
		sampleRateSet = false;
		currentlyWithinTrack = ElementType.None;
		subTrackNodeLevel = 0;
		currentlyParsingElement = ElementType.None;
		subElementNodeLevel = 0;
		numRenamedAudioBites = 0;
		domainMemberSubMemberLevel = 0;
		tempoSetting = new TempoSetting();
	}
	
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException 
	{
		//Get class attribute, name attribute and value attribute of element (if present)
		//We might optimize by only getting these when absolutely necessary, but is is easier to manage code-wise by always getting them here
		String classAttr = attributes.getValue("class");
		String nameAttr = attributes.getValue("name");
		String valueAttr = attributes.getValue("value");
		
		boolean justEnteredDomainMember = false;
		
		
		//Check for track
		if (currentlyWithinTrack == ElementType.None)
		{
			if (qName.equalsIgnoreCase("obj"))
			{
				if (classAttr != null)
				{
					if (classAttr.equalsIgnoreCase("MAudioTrackEvent"))
					{
						currentlyWithinTrack = ElementType.MAudioTrackEvent;
					}
					else if (classAttr.equalsIgnoreCase("MMarkerTrackEvent"))
					{
						currentlyWithinTrack = ElementType.MMarkerTrackEvent;
						
					}
					else if (classAttr.equalsIgnoreCase("MMidiTrackEvent"))
					{
					    currentlyWithinTrack = ElementType.MMidiTrackEvent;
					}
					else if (classAttr.equalsIgnoreCase("MInstrumentTrackEvent"))
					{
					    currentlyWithinTrack = ElementType.MInstrumentTrackEvent;
					}
					
					if (currentlyWithinTrack != ElementType.None)
					{
					    Debug.log("Entering track " + currentlyWithinTrack.toString());
					}
				}
			}
		}
		else
		{
			subTrackNodeLevel += 1;
		}
		
		
		//If we are currently within a content relevant element
		if (currentlyParsingElement != ElementType.None)
		{
			subElementNodeLevel += 1;
			switch (currentlyParsingElement)
			{
			case MAudioEvent:
				if (subElementNodeLevel == 1)
				{
					if (qName.equalsIgnoreCase("string"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Description"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								currentlySettingUpBite.setName(valueAttr);
							}
						}
					}
					else if (qName.equalsIgnoreCase("float"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Start"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								if (this.currentDomainType == 0)
								{
									//Start position is defined in midi ticks
									currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr),TimeFormat.MIDI_TICKS);
								}
								else if (this.currentDomainType == 1)
								{
									//Start position is defined in seconds
									currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr),TimeFormat.SECONDS);
								}
							}
						}
						else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								//Length is defined in samples (project sample rate) on audio events
								currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.SAMPLES);
							}
						}
					}
					else if (qName.equalsIgnoreCase("obj"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("AudioClip"))
						{
							String IDAttr = attributes.getValue("ID");
							if (IDAttr != null)
							{
								if (audioClipIdNameMap.containsKey(IDAttr))
								{
									currentlySettingUpBite.setNameFromAudioFileName(audioClipIdNameMap.get(IDAttr));
								}
							}
						}
					}
				}
				break;
			
			case PAudioClip:
				if (subElementNodeLevel == 1)
				{
					if (qName.equalsIgnoreCase("string"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Name"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								if (! audioClipIdNameMap.containsKey(currentPAudioClipID))//If we have not met this audio file before. Should always be the case here.
								{
									audioClipIdNameMap.put(currentPAudioClipID, valueAttr);
									currentlySettingUpBite.setNameFromAudioFileName(valueAttr);
								}
								else
								{
									Debug.log("Error. PAudioClip ID was already in Map");
								}
							}
						}
					}
				}
				break;
				
			case MAudioPartEvent:
				if (subElementNodeLevel == 1)
				{
					if (qName.equalsIgnoreCase("float"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Start"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								if (this.currentDomainType == 0)
								{
									//Start position is defined in midi ticks
									currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr), TimeFormat.MIDI_TICKS);
								}
								else if (this.currentDomainType == 1)
								{
									//Start position is defined in seconds
									currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr), TimeFormat.SECONDS);
								}
							}
						}
						else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								if (this.currentDomainType == 0)
								{
									//Length is defined in midi ticks
									currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.MIDI_TICKS);
								}
								else if (this.currentDomainType == 1)
								{
									//Length is defined in seconds
									currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.SECONDS);
								}
							}
						}
					}
				}
				break;
				
			case MAudioPart:
				if (subElementNodeLevel == 1)
				{
					if (qName.equalsIgnoreCase("string"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Name"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								currentlySettingUpBite.setName(valueAttr);
							}
						}
					}
				}
				break;
				
			case MRangeMarkerEvent:
				if (subElementNodeLevel == 1)
				{
					if (qName.equalsIgnoreCase("float"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Start"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								if (this.currentDomainType == 0)
								{
									//Start position is defined in midi ticks
									currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr), TimeFormat.MIDI_TICKS);
								}
								else if (this.currentDomainType == 1)
								{
									//Start position is defined in seconds
									currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr), TimeFormat.SECONDS);
								}
							}
						}
						else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								if (this.currentDomainType == 0)
								{
									//Length is defined midi ticks
									currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.MIDI_TICKS);
								}
								else if (this.currentDomainType == 1)
								{
									//Length is defined in seconds
									currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.SECONDS);
								}
							}
						}
					}
					else if (qName.equalsIgnoreCase("string"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("Name"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								currentlySettingUpBite.setName(valueAttr);
							}
						}
					}
				}
				break;
				
			case MMidiPartEvent:
			    if (subElementNodeLevel == 1)
                {
			        if (qName.equalsIgnoreCase("float"))
                    {
                        if (nameAttr != null && nameAttr.equalsIgnoreCase("Start"))
                        {
                            if (valueAttr != null && (! valueAttr.isEmpty()))
                            {
                                if (this.currentDomainType == 0)
                                {
                                    //Start position is defined in midi ticks
                                    currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr), TimeFormat.MIDI_TICKS);
                                }
                                else if (this.currentDomainType == 1)
                                {
                                    //Start position is defined in seconds
                                    currentlySettingUpBite.setStartValue(Double.parseDouble(valueAttr), TimeFormat.SECONDS);
                                }
                            }
                        }
                        else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
                        {
                            if (valueAttr != null && (! valueAttr.isEmpty()))
                            {
                                if (this.currentDomainType == 0)
                                {
                                    //Length is defined midi ticks
                                    currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.MIDI_TICKS);
                                }
                                else if (this.currentDomainType == 1)
                                {
                                    //Length is defined in seconds
                                    currentlySettingUpBite.setLengthValue(Double.parseDouble(valueAttr), TimeFormat.SECONDS);
                                }
                            }
                        }
                    }
                }
			    else if (subElementNodeLevel == 2)
			    {
			        //Note that the name of the MMidiPartEvent is actually set in a sub object called MMidiPart
			        //but it seems that there is always only one MMidiPart within an MMidiPartEvent
			        //so we just treat the whole thing as an MMidiPartEvent and check for indentation (2) to get the name
			        if (qName.equalsIgnoreCase("string"))
                    {
                        if (nameAttr != null && nameAttr.equalsIgnoreCase("Name"))
                        {
                            if (valueAttr != null && (! valueAttr.isEmpty()))
                            {
                                currentlySettingUpBite.setName(valueAttr);
                            }
                        }
                    }
			    }
			    break;
				
			case PArrangeSetup:
				if (subElementNodeLevel == 1)
				{
					if (qName.equalsIgnoreCase("float"))
					{
						if (nameAttr != null && nameAttr.equalsIgnoreCase("SampleRate"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								sampleRate = Double.parseDouble(valueAttr);
								sampleRateSet = true;
								Debug.log("Sample rate = " + sampleRate);
							}
						}
					}
				}
				break;
				
			case MTempoEvent:
			    if ((nameAttr != null) && (valueAttr != null) && (! valueAttr.isEmpty()))
			    {
			        if (nameAttr.equalsIgnoreCase("BPM"))
			        {
			            tempoSetting.currentEventSetBPM(Double.parseDouble(valueAttr));
			        }
			        else if (nameAttr.equalsIgnoreCase("PPQ"))
                    {
                        tempoSetting.currentEventSetMidiTickPosition(Double.parseDouble(valueAttr));
                    }
			        else if (nameAttr.equalsIgnoreCase("Func"))
                    {
                        tempoSetting.currentEventSetRamp(Integer.parseInt(valueAttr));
                    }
			    }
			    break;
				
			default:
				//Error, somehow unknown type or None
				break;
			}
				
		}
		
		//If we are not currently parsing content relevant element – check if we are now entering one
		if (currentlyParsingElement == ElementType.None)
		{
			if (qName.equalsIgnoreCase("obj"))
			{
				if (classAttr != null)
				{
					if (classAttr.equalsIgnoreCase("MAudioEvent"))
					{
						if (currentlyWithinTrack != ElementType.None)
						{
							currentlySettingUpBite = new AudioBite();
							currentlyParsingElement = ElementType.MAudioEvent;
						}
						else
						{
							Debug.log("Discarding MAudioEvent outside track");
						}
					}
					else if (classAttr.equalsIgnoreCase("MAudioPartEvent"))
					{
						if (currentlyWithinTrack != ElementType.None)
						{
							currentlySettingUpBite = new AudioBite();
							currentlyParsingElement = ElementType.MAudioPartEvent;
						}
						else
						{
							Debug.log("Discarding MAudioPartEvent outside track");
						}
					}
					else if (classAttr.equalsIgnoreCase("MRangeMarkerEvent"))
					{
						if (currentlyWithinTrack != ElementType.None)
						{
							currentlySettingUpBite = new AudioBite();
							currentlyParsingElement = ElementType.MRangeMarkerEvent;
						}
						else
						{
							Debug.log("Discarding MRangeMarkerEvent outside track");
						}
					}
					else if (classAttr.equalsIgnoreCase("MMidiPartEvent"))
                    {
                        if (currentlyWithinTrack != ElementType.None)
                        {
                            currentlySettingUpBite = new AudioBite();
                            currentlyParsingElement = ElementType.MMidiPartEvent;
                        }
                        else
                        {
                            Debug.log("Discarding MMidiPartEvent outside track");
                        }
                    }
					else if (classAttr.equalsIgnoreCase("MTempoEvent"))
					{
				        tempoSetting.startSettingUpTempoEvent();
				        currentlyParsingElement = ElementType.MTempoEvent;
					}
					else if (classAttr.equalsIgnoreCase("PArrangeSetup"))
					{
						currentlyParsingElement = ElementType.PArrangeSetup;
					}
				}
				
			}
			
			else
			{
				if (nameAttr != null)
				{
					if (qName.equalsIgnoreCase("member") && (nameAttr.equalsIgnoreCase("Domain")) && (domainMemberSubMemberLevel <= 0) && (currentlyWithinTrack != ElementType.None))
					{
						domainMemberSubMemberLevel = 1;
						justEnteredDomainMember = true;
						//Debug.log("Setting domainMemberSubMemberLevel to "+domainMemberSubMemberLevel);
					}
					else if ((domainMemberSubMemberLevel == 1) && qName.equalsIgnoreCase("int") && nameAttr.equalsIgnoreCase("type"))
					{
						if (valueAttr != null && (!valueAttr.isEmpty()))
						{
							currentDomainType = Integer.parseInt(valueAttr);
							Debug.log("Domain type set to "+ currentDomainType);
							
						}
					}
					else if (nameAttr.equalsIgnoreCase("rehearsaltempo") && qName.equalsIgnoreCase("float"))
					{
						if (valueAttr != null && (!valueAttr.isEmpty()))
						{
							tempoSetting.setRehearsalTempo(Double.parseDouble(valueAttr));
						}
					}
					else if (nameAttr.equalsIgnoreCase("rehearsalmode") && qName.equalsIgnoreCase("int"))
					{
						if (valueAttr != null && (!valueAttr.isEmpty()))
						{
							tempoSetting.setRehearsalMode(Integer.parseInt(valueAttr));
						}
					}
				}
			}
			
			
			if (currentlyParsingElement != ElementType.None)
			{
				subElementNodeLevel = 0;
				Debug.log("Starting to parse "+currentlyParsingElement.toString());
			}
		}
		
		//Special cases.
		//MAudioPart is contained in MAudioPartEvent and contains the part's name
		//PAudioClip is contained in MAudioEvent and the clip's name becomes the bite's name if it has no description.
		else if (currentlyParsingElement == ElementType.MAudioPartEvent || currentlyParsingElement == ElementType.MAudioEvent)
		{
			if (subElementNodeLevel == 1)
			{
				if (qName.equalsIgnoreCase("obj"))
				{
					if (classAttr != null)
					{
						if (classAttr.equalsIgnoreCase("MAudioPart"))
						{
							currentlyParsingElement = ElementType.MAudioPart;
							subElementNodeLevel = 0;
							Debug.log("Starting to parse "+currentlyParsingElement.toString());
						}
						else if (classAttr.equalsIgnoreCase("PAudioClip"))
						{
							currentlyParsingElement = ElementType.PAudioClip;
							subElementNodeLevel = 0;
							String idAttr = attributes.getValue("ID");
							if (idAttr != null)
							{
								currentPAudioClipID = idAttr;
							}
							Debug.log("Starting to parse "+currentlyParsingElement.toString());
						}
					}
				}
			}
		}
		
		  //Increment domainMemberSubMemberLevel if we are inside Domain element
        if ((domainMemberSubMemberLevel > 0) && (!justEnteredDomainMember))
        {
            domainMemberSubMemberLevel += 1;
            //Debug.log("Setting domainMemberSubMemberLevel to "+domainMemberSubMemberLevel);
        }
		
		
		
	}
	
	/**
	 * Does the final processing of the parsed AudioBites.
	 * This includes sorting by start time and removing audio bites that are not properly set up.
	 */
	private void finalizeAudioBites()
	{
		
		List<AudioBite> unSetupBites = new ArrayList<AudioBite>();
		
		//Calculate start and end in seconds and check if properly set up
		for (AudioBite b : audioBites)
		{
			b.calculateStartAndEndSec(sampleRate, tempoSetting);
			if(! b.isSetup())
			{
				unSetupBites.add(b);
			}
			
		}
		
		//Remove any audio bite that is not properly set up
		if (unSetupBites.size() > 0)
		{
			Debug.log("Error: Removing " + unSetupBites.size() + " audioBite(s) from list, due to unfinished setup.");
			for (AudioBite b : unSetupBites)
			{
				audioBites.remove(b);
			}
		}
		
		//Sort by start
		Collections.sort(audioBites, new AudioBiteStartComparator());
	}
	
}
