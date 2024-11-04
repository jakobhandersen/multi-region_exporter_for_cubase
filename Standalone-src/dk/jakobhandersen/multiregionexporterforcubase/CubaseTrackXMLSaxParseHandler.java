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
	 * Describes which (relevant) XML element type (ElementType) we are currently in.
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
	
	
	@Override
	public void startDocument()
	{
		audioBites = new ArrayList<AudioBite>();
		audioClipIdNameMap = new HashMap<String,String>(0);
		currentlySettingUpBite = null;
		sampleRateSet = false;
	}
	
	@Override
	public void endDocument()
	{
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
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException 
	{
		if (currentlyParsingElement != ElementType.None)
		{
			subElementNodeLevel += 1;
			String valueAttr = attributes.getValue("value");
			String nameAttr = attributes.getValue("name");
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
								//Start position is defined in midi ticks
								currentlySettingUpBite.setStart(midiTicksToSeconds(Double.parseDouble(valueAttr)));
							}
						}
						else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								//Length is defined in samples (project sample rate) on audio events
								currentlySettingUpBite.setLength(Double.parseDouble(valueAttr), TimeFormat.SAMPLES);
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
								//Start position is defined in midi ticks
								currentlySettingUpBite.setStart(midiTicksToSeconds(Double.parseDouble(valueAttr)));
							}
						}
						else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								//Length is defined midi ticks
								currentlySettingUpBite.setLength(midiTicksToSeconds(Double.parseDouble(valueAttr)), TimeFormat.SECONDS);
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
								//Start position is defined in midi ticks
								currentlySettingUpBite.setStart(midiTicksToSeconds(Double.parseDouble(valueAttr)));
							}
						}
						else if (nameAttr != null && nameAttr.equalsIgnoreCase("Length"))
						{
							if (valueAttr != null && (! valueAttr.isEmpty()))
							{
								//Length is defined midi ticks
								currentlySettingUpBite.setLength(midiTicksToSeconds(Double.parseDouble(valueAttr)), TimeFormat.SECONDS);
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
				
			default:
				//Error, somehow unknown type or None
				break;
			}
				
		}
		
		if (currentlyParsingElement == ElementType.None)
		{
			if (qName.equalsIgnoreCase("obj"))
			{
				String classAttr = attributes.getValue("class");
				if (classAttr != null)
				{
					if (classAttr.equalsIgnoreCase("MAudioEvent"))
					{
						currentlySettingUpBite = new AudioBite();
						currentlyParsingElement = ElementType.MAudioEvent;
					}
					else if (classAttr.equalsIgnoreCase("MAudioPartEvent"))
					{
						currentlySettingUpBite = new AudioBite();
						currentlyParsingElement = ElementType.MAudioPartEvent;
					}
					else if (classAttr.equalsIgnoreCase("MRangeMarkerEvent"))
					{
						currentlySettingUpBite = new AudioBite();
						currentlyParsingElement = ElementType.MRangeMarkerEvent;
					}
					else if (classAttr.equalsIgnoreCase("PArrangeSetup"))
					{
						currentlyParsingElement = ElementType.PArrangeSetup;
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
		//PAudioClip is contained in MAudioEvent and the clip's name becomes the bite's name if it has no desciption.
		else if (currentlyParsingElement == ElementType.MAudioPartEvent || currentlyParsingElement == ElementType.MAudioEvent)
		{
			if (subElementNodeLevel == 1)
			{
				if (qName.equalsIgnoreCase("obj"))
				{
					String classAttr = attributes.getValue("class");
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
					if (currentlySettingUpBite != null)
					{
						audioBites.add(currentlySettingUpBite);
					}
					currentlySettingUpBite = null;
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
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException 
	{
		
	}
	
	/**
	 * Does the final processing of the parsed AudioBites.
	 * This includes sorting by start time and removing audio bites that are not properly set up.
	 */
	private void finalizeAudioBites()
	{
		Collections.sort(audioBites, new AudioBiteStartComparator());//Sort by start
		
		List<AudioBite> unSetupBites = new ArrayList<AudioBite>();
		
		for (AudioBite b : audioBites)
		{
			b.calculateEnd(sampleRate);
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
	}
	
	
	/**
	 * Converts from midi ticks to seconds
	 * @param ticks midi ticks
	 * @return seconds
	 */
	private double midiTicksToSeconds(double ticks)
	{
		return ticks/Constants.midiTicksPerSec;
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
	
}
