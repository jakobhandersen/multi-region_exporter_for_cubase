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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;



/**
 * The class that coordinates the actual functionality of the application.
 * Handles connectivity with the UserInterface.
 * @author Jakob Hougaard Andersen
 *
 */
public class ExporterEngine
{
	/**
	 * The currently read audio file (represented as an InputAudioFile) if any
	 */
	InputAudioFile currentInputAudioFile = null;
	
	/**
	 * AudioBites that are properly set up and lie within the length of the loaded audio file
	 */
	private List<AudioBite> audioBites;
	
	/**
	 * The amount of seconds the AudioBites are allowed to be outside audio file range.
	 * To compensate for small rounding errors and alike.
	 */
	private double biteOutsideAudioTolerance = 0.01;
	
	/**
	 * The time in seconds that is added to the length of the individual AudioBite.
	 * This can be useful if the mix has reverb or other time influencing effects.
	 */
	private double trailingTime = 0;
	
	/**
	 * The folder in which the output files will be created
	 */
	private String outputFolder = "noPath";
	
	/**
	 * Has the output folder been set?
	 */
	private boolean outputFolderSet = false;
	
	/**
	 * Should the names/descriptions of the individual events in Cubase be used for naming the output audio files.
	 * Alternatively, a fixed name (fixedName) specified by the user will be used as names (followed by _0001, _0002, 000n)
	 */
	private boolean useCubaseNames = true;
	
	/**
	 * The number of audio bites that were renamed, due to name equality, in the last read XML
	 * This is used to give feedback to the user and is only relevant if useCubaseNames == true
	 */
	private int renamedAudioBitesInLastXML = 0;
	
	/**
	 * If useCubaseNames == false, this string will be used for naming the output audio files
	 */
	private String fixedName = "region";
	
	/**
	 * Path to sox program on the user's computer
	 */
	private String soxPath = "not set";
	
	/**
	 * Path to soxi shortcut on the user's computer
	 */
	private String soxiPath = "not set";
	
	/**
	 * Path to FFMPEG program on the user's computer
	 */
	private String ffmpegPath = "not set";
	
	
	/**
	 * Reference to the currently running AudioSplitter, if any
	 */
	private AudioOutputter currentlyRunningSplitter = null;
	
	/**
	 * Reference to the currently running WaveformGenerator, if any
	 */
	private WaveformGenerator currentlyRunningWaveformGenerator = null;
	
	/**
	 * Reference to the currently running InputAudioFileBuilder, if any
	 */
	private InputAudioFileBuilder currentlyRunningInputAudioFileBuilder = null;
	
	/**
	 * Number of horizontal pixels
	 */
	private int waveformWidth;
	
	/**
	 * Number of vertical pixels
	 */
	private int waveformHeight;
	
	/**
	 * Should the output files be converted to mp3 after splitting?
	 */
	private boolean convertToMp3 = false;
	
	/**
	 * If converting to mp3, this is the FFMPEG argument controlling bitrate
	 */
	private String convertToMp3FFMPEGBitrateArgument;
	
	
	/**
	 * List of currently started Processes that need to be destroyed if the application is closed.
	 */
	private List<Process> startedProcesses;
	
	/**
	 * reference to the UserInterface, set in constructor
	 */
	private UserInterface userInterface;
	
	
	/**
	 * path to temporary folder
	 */
	private String temporaryFolderPath; 
	
	/**
	 * Constructor
	 */
	public ExporterEngine(UserInterface userInterface)
	{
		audioBites = new ArrayList<AudioBite>();
		this.userInterface = userInterface;
		startedProcesses = new ArrayList<Process>();
		setPaths();
	}
	
	
	/**
	 * Called from the AudioOutputter when this is done
	 * @param successes the number of successfully created files
	 * @param total the number of the files that were supposed to be created
	 * @param caller the AudioSplitter calling this function
	 */
	public void audioOutputterDoneCallback(int successes, int total, AudioOutputter caller)
	{
		//Is this correct use of 'synchronized'? I am not so experienced with muti-threading..
		synchronized (this)
		{
			if (successes == total)
			{
				sendMessageToUser(UserMessageType.SUCCESS, successes + " audio file(s) successfully created in folder: "+outputFolder);
			}
			else
			{
				sendMessageToUser(UserMessageType.ERROR, "Error(s) accured while creating " + (total - successes) + " audio file(s).");
				sendMessageToUser(UserMessageType.STATE, successes + " audio files successfully created in folder: "+outputFolder);
			}
			if (caller != currentlyRunningSplitter)
			{
				Debug.log("Error in AudioSplitterCallback(). caller != currentlyRunningSplitter");
			}
			currentlyRunningSplitter = null;
			sendEventToInterface(EngineEvent.DONE_OUTPUTTING_FILES);
		}
	}
	
	/**
	 * Called from AudioOutputter to inform user about progress of output process
	 * @param percentage percentage done (0 to 100)
	 */
	public void audioOutputterPercentageCallback(int percentage)
	{
		userInterface.setOuputPercentage(percentage);
	}
	
	/**
	 * Called from AudioOutputter to inform user about progress of output process
	 * @param text what is the outputter doing now?
	 */
	public void audioOutputterProcessTextCallback(String text)
	{
		userInterface.setOutputProcessText(text);
	}
	
	/**
	 * Method to be called when closing the application.
	 * Interrupts running threads an destroys registered running processes and deletes temporary files
	 */
	public void cleanUp()
	{
		try
		{
			Debug.log("Deleting temporary files folder");
			if (Utils.deleteDirectory(new File(temporaryFolderPath)))
			{
				Debug.log("Successfully deleted all temporary files");
			}
			else
			{
				Debug.log("Error deleting one or more temporary files");
			}
			
			if (currentlyRunningSplitter != null)
			{
				Debug.log("Trying to interrupt currentlyRunningSplitter");
				currentlyRunningSplitter.interrupt();
			}
			
			if (currentlyRunningWaveformGenerator != null)
			{
				Debug.log("Trying to interrupt currentlyRunningWaveformGenerator");
				currentlyRunningWaveformGenerator.interrupt();
			}
			
			if (currentlyRunningInputAudioFileBuilder != null)
			{
				Debug.log("Trying to interrupt currentlyRunningInputAudioFileBuilder");
				currentlyRunningInputAudioFileBuilder.interrupt();
			}
			if (startedProcesses != null)
			{
				if (startedProcesses.size() > 0)
				{
					Debug.log("Stopping "+ startedProcesses.size() + " process(es)");
					for (Process p : startedProcesses)
					{
						if (p != null)
						{
							p.destroyForcibly();
						}
					}
				}
				else
				{
					Debug.log("No started processes to stop");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Called from the UserInterface when the user accepts overwriting existing files
	 */
	public void createFilesOverwriteAccepted()
	{
		createFiles();
	}
	
	/**
	 * Function called by InputAudioFileBuilder when it is done building the InputAudioFile
	 * @param caller the InputAudioFileBuilder calling this function
	 * @param builtFile the built InputAudioFile
	 */
	public void inputAudioFileBuilderCallback(InputAudioFileBuilder caller,InputAudioFile builtFile)
	{
		//Is this correct use of 'synchronized'? I am not so experienced with muti-threading..
		synchronized (this)
		{
			if (caller != currentlyRunningInputAudioFileBuilder)
			{
				Debug.log("Error. Somehow, InputAudioFileBuilderCallback() was called from other caller than currentlyRunningInputAudioFileBuilder");
				return;
			}
			Debug.log("Done building InputAudioFile");
			currentlyRunningInputAudioFileBuilder = null;
			if (builtFile == null)
			{
				sendEventToInterface(EngineEvent.ERROR_READING_AUDIO_FILE);
				sendMessageToUser(UserMessageType.ERROR, "Could not read audio file");
			}
			else if (builtFile.getIsValid())
			{
				currentInputAudioFile = builtFile;
				userInterface.audioFileRead(currentInputAudioFile);
				sendEventToInterface(EngineEvent.READY_FOR_XML);
				sendMessageToUser(UserMessageType.STATE, "Audio file loaded: " + currentInputAudioFile.getFilename());
				createWaveform();
			}
			else
			{
				sendEventToInterface(EngineEvent.ERROR_READING_AUDIO_FILE);
				sendMessageToUser(UserMessageType.ERROR, "Could not read audio file: "+ builtFile.getFilename());
			}
		}
	}
	
	/**
	 * Creates a new InputAudioFileBuilder and makes it create a new InputAudioFile as a representation of the specified audio file.
	 * The InputAudioFileBuilder later calls InputAudioFileBuilderCallback() when it is done building the InputAudioFile
	 * @param fileName full path to an audio file
	 */
	public void readInputAudioFile(String fileName)
	{
		clearCurrentAudioFile();
		clearAudioBites();
		currentlyRunningInputAudioFileBuilder = new InputAudioFileBuilder(fileName, soxiPath, this);
		currentlyRunningInputAudioFileBuilder.start();
		sendEventToInterface(EngineEvent.READING_AUDIO_FILE);
		//sendMessageToUser(UserMessageType.STATE, "Loading audio file...");
	}

	/**
	 * Reads a Cubase track file (XML) and parses the content.
	 * Calls processAudioBitesFromParser() if the file was successfully parsed 
	 * @param file full path to the XML file to be read
	 */
	public void readXML(String file)
	{
		clearAudioBites();
		clearOutputFolder();
		if (currentInputAudioFile != null)
		{
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		    try 
		    {
		        SAXParser saxParser = saxParserFactory.newSAXParser();
		        CubaseTrackXMLSaxParseHandler handler = new CubaseTrackXMLSaxParseHandler();
		        File f = new File(file);
		        saxParser.parse(f, handler);
		        processAudioBitesFromParser(handler.getAudioBites(), file);
		    } 
		    catch (FileNotFoundException e)
		    {
		    	sendMessageToUser(UserMessageType.ERROR, "File not found: " + file);
		    }
		    catch (Exception e) 
		    {
		    	sendMessageToUser(UserMessageType.ERROR, "An error occurred while trying to parse XML file: " + file);
		    	Debug.log("Exception caught while trying to parse XML file:");
		        e.printStackTrace();
		    }
		}
		else
		{
			sendMessageToUser(UserMessageType.ERROR, "An audio file must be loaded before track XML can be loaded");
		}
	}

	/**
	 * Method for registering a running Process that needs to be stopped if program is closed before it is done.
	 * @param p Process to register
	 */
	public void registerStartedProcess(Process p)
	{
		//Is this correct use of 'synchronized'? I am not so experienced with muti-threading..
		synchronized(startedProcesses)
		{
			if (startedProcesses == null)
			{
				startedProcesses = new ArrayList<Process>();
			}
			startedProcesses.add(p);
		}
	}

	/**
	 * Sends a message to the user. Printed in the Log window.
	 * @param type type of message - determines how it is shown.
	 * @param message the actual message
	 */
	public void sendMessageToUser(UserMessageType type, String message)
	{
		userInterface.sendMessageToUser(type, message);
	}

	/**
	 * Called from UserInterface when convert to mp3 is changed/set
	 * @param convert
	 */
	public void setConvertToMp3(boolean convert)
	{
		convertToMp3 = convert;
		Debug.log("Convert to mp3 set to "+convertToMp3);
	}
	
	/**
	 * Called from UserInterface when mp3 conversion settings are changed/set
	 * @param argument bitrate argument used in FFMPEG for mp3 conversion
	 */
	public void setMp3ConversionBitrateArgument(String argument)
	{
		convertToMp3FFMPEGBitrateArgument = argument;
		Debug.log("Mp3 conversion bitrate argument set to: "+convertToMp3FFMPEGBitrateArgument);
	}
	
	/**
	 * Sets the output folder and at the same time starts the output process.
	 * @param path path to the folder in which the output file should be written
	 */
	public void setOutputFolder(String path)
	{
		if (currentlyRunningSplitter != null)
		{
			sendMessageToUser(UserMessageType.ERROR, "Can't output files now since the exporter is already in the process of outputting");
			return;
		}
		try
		{
			File f = new File(path);
			if((f != null) && f.exists() && f.isDirectory())
			{
				outputFolder = path;
				outputFolderSet = true;
				boolean[] folderContainsFilesResult = outputFolderContainsFilesAlready();
				if (folderContainsFilesResult[1])//If it is the special case where input audio file will be overwritten
				{
					sendEventToInterface(EngineEvent.INPUT_FILE_TO_BE_OVERWRITTEN);
					return;//Not allowed, do no further processing
				}
				if (folderContainsFilesResult[0])//If other 'normal' files will be overwritten
				{
					sendEventToInterface(EngineEvent.FILES_TO_BE_OVERWRITTEN);
				}
				else
				{
					createFiles();
				}
			}
			else
			{
				sendMessageToUser(UserMessageType.ERROR, "Could not find destination folder: "+ path);
			}
		}
		catch (Exception e)
		{
			sendMessageToUser(UserMessageType.ERROR, "An error occurred while trying to output files");
			Debug.log("Exception caught while trying to output files:");
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the amount of time (seconds) that each AudioBite is extended in length compared to the actual event/region in Cubase
	 * @param seconds
	 */
	public void setTrailingTime(double seconds)
	{
		if (trailingTime != seconds)
		{
			trailingTime = seconds;
			if (audioBites.size() >0)
			{
				setFunctionalEndAndOutputRangeMarkers();
			}
		}
		Debug.log("Trailing time set to "+seconds+" seconds");
	}
	
	/**
	 * Sets the height in pixels of waveform view
	 * @param height
	 */
	public void setWaveformHeight(int height)
	{
		waveformHeight = height;
	}
	
	
	/**
	 * Sets the width in pixels of waveform view
	 * @param width
	 */
	public void setWaveformWidth(int width)
	{
		waveformWidth = width;
	}
	
	/**
	 * Method for unregistering a Process previously registered as running.
	 * @param p Process to unregister
	 */
	public void unregisterStartedProcess(Process p)
	{
		//Is this correct use of 'synchronized'? I am not so experienced with muti-threading..
		synchronized(startedProcesses)
		{ 
			if (startedProcesses != null)
			{
				startedProcesses.remove(p);
			}
		}
	}
	
	
	/**
	 * Called from UserInterface if the user wants to use output audio file naming based on event names/descriptions in Cubase (in track XML file)
	 */
	public void useCubaseNames()
	{
		useCubaseNames = true;
		Debug.log("Using cubase names/descriptions");
	}
	
	
	/**
	 * Called from UserInterface if the user wants to use a fixed string for naming output audio files
	 * @param n the name to be used (will be followed by "_0001", "_0002" etc)
	 */
	public void useFixedName(String n)
	{
		fixedName = Utils.getValidFileNameString(n);
		if (fixedName.isEmpty() || fixedName == "")
		{
			fixedName = "invalid_file_name";
		}
		Debug.log("Using fixed name: "+fixedName);
		useCubaseNames = false;
	}
	
	/**
	 * Called by WaveformGenerator when it is done generating
	 * @param caller the calling WaveformGenerator
	 * @param success was it successful?
	 * @param inputAudioFile the InputAudioFile the waveform was created on the basis of (might not be the same as currentInputAudioFile).
	 */
	public void waveformGeneratorCallback(WaveformGenerator caller, boolean success,InputAudioFile inputAudioFile, String waveformPng)
	{
		//Is this correct use of 'synchronized'? I am not so experienced with muti-threading..
		synchronized (this)
		{
			if (caller != currentlyRunningWaveformGenerator)
			{
				Debug.log("Error in WaveformGeneratorCallback(). caller != currentlyRunningWaveformGenerator");
			}
			currentlyRunningWaveformGenerator = null;
			if (inputAudioFile != currentInputAudioFile)//If we have read a new audio file since caller was started
			{
				Utils.deleteFile(waveformPng);
				createWaveform();
				return;
			}
			else
			{
				Debug.log("Done generating waveform");
				sendEventToInterface(EngineEvent.DONE_GENERATING_WAVEFORM);
				if(success)
				{
					userInterface.waveformCreated(waveformPng);
				}
				else
				{
					sendMessageToUser(UserMessageType.WARNING, "Could somehow not create waveform preview");
				}
			}
		}
		
	}
	
	/**
	 * Clears audioBites, removes range markers in the UserInterface and tells it that we are not ready for splitting
	 */
	private void clearAudioBites()
	{
		deleteRangeMarkers();
		audioBites.clear();
		sendEventToInterface(EngineEvent.NOT_READY_FOR_SPLIT);
		//ResetTrailingTime();
	}
	
	/**
	 * Sets currentInputAudioFile to null and sends relevant messages to the UserInterface
	 */
	private void clearCurrentAudioFile()
	{
		currentInputAudioFile = null;
		sendEventToInterface(EngineEvent.CLEAR_AUDIO_FILE);
		sendEventToInterface(EngineEvent.NOT_READY_FOR_XML);
		sendEventToInterface(EngineEvent.NOT_READY_FOR_SPLIT);
	}
	
	
	/**
	 * Clears the path to output folder.
	 * To avoid writing to previously specified folder.
	 */
	private void clearOutputFolder()
	{
		outputFolder = "noPath";
		outputFolderSet = false;
	}
	
	/**
	 * Creates the files by starting a new AudioSplitter
	 */
	private void createFiles()
	{
		if (! outputFolderSet)
		{
			sendMessageToUser(UserMessageType.ERROR, "Trying to create files without output folder being set. Cancelling.");
			return;
		}
		if (audioBites == null || (audioBites.size() == 0))
		{
			sendMessageToUser(UserMessageType.ERROR, "Can't create output files since no regions are currently in memory");
			return;
		}
		
		if (convertToMp3)
		{
			sendMessageToUser(UserMessageType.STATE, "Extracting and converting "+ audioBites.size() +" file(s) to destination folder: " + outputFolder + " ...");
		}
		else
		{
			sendMessageToUser(UserMessageType.STATE, "Extracting "+ audioBites.size() +" file(s) to destination folder: " + outputFolder + " ...");
		}
		
		if (renamedAudioBitesInLastXML > 0 && useCubaseNames)
        {
        	sendMessageToUser(UserMessageType.WARNING,renamedAudioBitesInLastXML +" files were renamed since their Cubase names are not unique");
        }
		currentlyRunningSplitter = new AudioOutputter(currentInputAudioFile,audioBites,outputFolder,soxPath, ffmpegPath,temporaryFolderPath, convertToMp3, convertToMp3FFMPEGBitrateArgument, useCubaseNames, fixedName, this);
		
		currentlyRunningSplitter.start();
		sendEventToInterface(EngineEvent.OUTPUTTING_FILES);
	
	}
	
	/**
	 * Creates a downsampled version of the current input audio file to be used for extracting waveform data.
	 */
	private void createWaveform()
	{
		if (currentInputAudioFile == null)
		{
			sendMessageToUser(UserMessageType.ERROR,"Can't create waveform since there is no currently no reference to an audio file !(?)");
			return;
		}
		if (currentlyRunningWaveformGenerator == null)
		{
			currentlyRunningWaveformGenerator = new WaveformGenerator(currentInputAudioFile, ffmpegPath, temporaryFolderPath, waveformWidth, waveformHeight, this);
			currentlyRunningWaveformGenerator.start();
			sendEventToInterface(EngineEvent.GENERATING_WAVEFORM);
		}
		else//else, this function will be called when currentlyRunningWaveformGenerator calls back
		{
			Debug.log("Already creating waveform. Waiting until current process is done");
		}
		
	}
	
	/**
	 * Deletes range markers in the UserInterface
	 */
	private void deleteRangeMarkers()
	{
		userInterface.deleteRangeMarkers();
	}
	
	/**
	 * Checks if the chosen output folder contains any files with the same name as any of the audio files to be written.
	 * @return boolean[2]. First boolean indicates whether output folder contains file(s) to be overwritten. The second boolean indicates whether it is the special case where input audio file will be overwritten.
	 */
	private boolean[] outputFolderContainsFilesAlready()
	{
		boolean[] result = new boolean[]{false,false};
		if (outputFolderSet)
		{
			try
			{
				if (useCubaseNames)
				{
					for (AudioBite b : audioBites)
					{
						String fileName = outputFolder+"/"+b.getName()+".";
						if (convertToMp3)
						{
							fileName += "mp3";
						}
						else
						{
							fileName += currentInputAudioFile.getFileExtension();
						}
						
						File f = new File(fileName);
						if (f != null && f.exists() && f.isFile())
						{
							result[0] = true;
						}
						if (currentInputAudioFile != null && (currentInputAudioFile.getFilename().compareTo(fileName) == 0))
						{
							result[1] = true;
						}
					}
				}
				else
				{
					for (int i = 0; i < audioBites.size(); i++)
					{
						String fileName = outputFolder+"/"+fixedName+"_"+String.format("%04d", i+1)+".";
						if (convertToMp3)
						{
							fileName += "mp3";
						}
						else
						{
							fileName += currentInputAudioFile.getFileExtension();
						}
						File f = new File(fileName);
						if (f != null && f.exists() && f.isFile())
						{
							result[0] = true;
						}
						if (currentInputAudioFile != null && (currentInputAudioFile.getFilename().compareTo(fileName) == 0))
						{
							result[1] = true;
						}
					}
				}
			}
			catch(Exception e)
			{
				Debug.log("Exception caught while trying to check for existing files:");
				e.printStackTrace();
			}
		}
		return result;
	}
	
	
	/**
	 * Takes over from readXML() and does further work with the AudioBites found in the XML file.
	 * @param bites List of the AudioBites found in Cubase track XML file
	 * @param xmlFileName file name of the Cubase track XML file. Used for user messages.
	 */
	private void processAudioBitesFromParser(List<AudioBite> bites, String xmlFileName)
	{
		if (currentInputAudioFile == null)
		{
			sendMessageToUser(UserMessageType.ERROR, "Java external somehow lost reference to input audio file");
			return;
		}
		sendMessageToUser(UserMessageType.STATE,"Track file loaded: " + xmlFileName);
		if (bites != null && bites.size() > 0)
		{
			for (AudioBite b : bites)
			{
				if (b.getEnd() <= (currentInputAudioFile.getLength() + biteOutsideAudioTolerance))
				{
					audioBites.add(b);
				}
			}
			
			if (audioBites.size() > 0)
			{
				renamedAudioBitesInLastXML = ValidateAudioBiteNames();
				if (audioBites.size() != bites.size())
				{
					int outside = bites.size() - audioBites.size();
					sendMessageToUser(UserMessageType.WARNING,outside + " region(s) in track file are outside the range of audio file.");
				}
				sendMessageToUser(UserMessageType.STATE,audioBites.size() + " valid region(s) were found in file.");
				sendEventToInterface(EngineEvent.READY_FOR_SPLIT);
				setFunctionalEndAndOutputRangeMarkers();
			}
			else
			{
				sendMessageToUser(UserMessageType.ERROR, "No regions within audio file range were found in the track file.");
				sendEventToInterface(EngineEvent.NOT_READY_FOR_SPLIT);
			}
		}
		else
		{
			sendMessageToUser(UserMessageType.ERROR, "No regions were found in track file.");
			sendEventToInterface(EngineEvent.NOT_READY_FOR_SPLIT);
		}
		//PrintAudioBites();
	}
	
	private void sendEventToInterface(EngineEvent e)
	{
		userInterface.receiveEvent(e);
	}
	
	/**
	 * Sets the functional ends (taking the trailingTime into account) of the AudioBites and also updates the range markers in the UserInterface
	 */
	private void setFunctionalEndAndOutputRangeMarkers()
	{
		if (currentInputAudioFile == null)
		{
			sendMessageToUser(UserMessageType.ERROR, "Java external somehow lost reference to input audio file");
			return;
		}
		if (audioBites.size() > 0)
		{
			for (int i = 0; i < audioBites.size(); i++)
			{
				//Set functionalEnd
				AudioBite b = audioBites.get(i);
				double functionalEnd = b.getEnd();
				functionalEnd += trailingTime;
				if (functionalEnd > currentInputAudioFile.getLength())//Clamp to length of audio file
				{
					functionalEnd = currentInputAudioFile.getLength();
				}
				b.setFunctionalEnd(functionalEnd);
			}
			userInterface.setRangeMarkers(audioBites);
		}
		else
		{
			Debug.log("Can't UpdateAndOutputRangeMarkers since there are no AudioBites");
		}
	}
	
	/**
	 * Set up the different paths. Calculated from paths relative to jar file.
	 */
	private void setPaths()
	{
		String fileSeparator = Utils.getFileSeparator();
		//Calculate paths
		if (userInterface.computerIsMac())
		{
			String pathToContentsFolder = Utils.getJarPath(2);
			soxPath = pathToContentsFolder + fileSeparator + "sox" + fileSeparator + "sox";
			soxiPath = pathToContentsFolder + fileSeparator + "sox" + fileSeparator + "soxi";
			ffmpegPath = pathToContentsFolder + fileSeparator + "ffmpeg" + fileSeparator + "ffmpeg";
		}
		else
		{
			String pathToMainFolder = Utils.getJarPath(1);
			String pathToResourcesFolder = pathToMainFolder + fileSeparator + "resources";
			
			soxPath = pathToResourcesFolder +fileSeparator + "sox" + fileSeparator + "sox";
			soxiPath = pathToResourcesFolder + fileSeparator + "sox" + fileSeparator + "soxi";
			ffmpegPath = pathToResourcesFolder + fileSeparator + "ffmpeg" + fileSeparator + "ffmpeg";
		}
		
		try
		{
			Path p = Files.createTempDirectory("MultiRegionExporterForCubase");
			temporaryFolderPath = p.toString();
		}
		catch(Exception e)
		{
			Debug.log("Exception caught while trying to create temoprary folder:");
			e.printStackTrace();
		}
		
		Debug.log("Path to temporary folder: "+temporaryFolderPath);
		Debug.log("Sox path set to: "+soxPath);
		Debug.log("Soxi path set to: "+soxiPath);
		Debug.log("FFMPEG path set to: "+ffmpegPath);
	}
	
	/**
	 * Validates naming of AudioBites (two files can't share same name)
	 * @return number of bites that were renamed
	 */
	private int ValidateAudioBiteNames()
	{
		Map<String, AudioBite> nameToBiteMap = new HashMap<String,AudioBite>();
		List<AudioBite> bitesToBeRenamed = new ArrayList<AudioBite>();
		HashSet<String> reacurringNames = new HashSet<String>();
		int numRenamedAudioBites = 0;
		for (AudioBite b : audioBites)
		{
			if (nameToBiteMap.containsKey(b.getName()))
			{
				bitesToBeRenamed.add(b);
				reacurringNames.add(b.getName());
			}
			else
			{
				nameToBiteMap.put(b.getName(), b);
			}
		}
		//Rename audio bites that have the same name as a previous one in the list
		if (bitesToBeRenamed.size() > 0)
		{
			//first, remove the bites from nameToBiteMap that have the same name as bite(s) in bitesToBeRenamed (reacurringNames).
			//and put them into the start of bitesToBeRenamed (so we will get similar_name_0001, similar_name_0002 etc. instead of similar_name, similar_name_0002 etc.)
			for (String s : reacurringNames)
			{
				AudioBite bite = nameToBiteMap.remove(s);
				if (bite == null)
				{
					Debug.log("Error: Could not find AudioBite in nameToBiteMap though its name was in reacurringNames");
				}
				else
				{
					bitesToBeRenamed.add(0, bite);
				}
			}
			numRenamedAudioBites = bitesToBeRenamed.size();
			Debug.log("Renaming " + numRenamedAudioBites +" audioBite(s), due to name equality.");
			for (AudioBite b : bitesToBeRenamed)
			{
				
				
				int nameAddition = 1;
				String initialName = b.getName();
				String currentName = initialName +"_"+String.format("%04d", nameAddition);
				while (nameToBiteMap.containsKey(currentName))
				{
					nameAddition += 1;
					currentName = initialName + "_"+String.format("%04d", nameAddition);
				}
				b.setName(currentName);
				nameToBiteMap.put(currentName, b);
			}
		}
		return numRenamedAudioBites;
	}
	
}
