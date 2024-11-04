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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * (Thread) Class that does the actual outputting of file extracted from the input file.
 * Uses SoX for splitting audio and FFMPEG for optional conversion.
 * @author Jakob Hougaard Andersen
 *
 */
public class AudioOutputter extends Thread
{
	/**
	 * The list of AudioBites that the inputFile should be split according to
	 */
	private List<AudioBite> audioBites;
	
	/**
	 * The audio input file (AudioInputFile) that should be split.
	 */
	private InputAudioFile inputFile;
	
	/**
	 * Path to the folder to output the files into
	 */
	private String outputFolder;
	
	/**
	 * The MultiRegionExporterForCubase to call when done splitting
	 */
	private ExporterEngine caller;
	
	/**
	 * Path to SoX
	 */
	private String soxPath;
	
	/**
	 * Path to FFMPEG program on the user's computer
	 */
	private String ffmpegPath = "not set";
	
	/**
	 * Path to the temporary files folder
	 */
	private String temporaryFolderPath;
	
	/**
	 * Should the output files be converted using ffmpeg after splitting?
	 */
	private boolean convertWithFfmpeg = false;
	
	/**
	 * If converting with ffmpeg, these are the arguments for conversion (between input file and output file)
	 */
	private ArrayList<String> convertWithFfmpegArguments;
	
	/**
	 * If converting with ffmpeg, this is the file ending of the output files
	 */
	private String convertWithFfmpegFileEnding;
	
	/**
	 * Should Cubase names/descriptions be used for naming output files?
	 */
	private boolean useCubaseNames;
	
	/**
	 * If useCubaseNames == false, this string will be used for naming the output audio files
	 */
	private String fixedName;
	
	/*
	 * If errors occur with conversion with ffmpeg, this will be set to true
	 */
	private boolean ffmpegError = false;
	
	/**
	 * Constructor
	 * @param inputFile the audio input file (AudioInputFile) that should be split.
	 * @param audioBites list of AudioBites that the inputFile should be split into.
	 * @param outputFolder where to output the files
	 * @param soxPath path to SoX program
	 * @param ffmpegPath path to FFMPEG program
	 * @param convertWithFfmpeg should the files be converted using ffmpeg after splitting
	 * @param convertWithFfmpegArguments if converting with ffmpeg, this string is the supplied arguments
	 * @param convertWithFfmpegFileEnding if converting with ffmpeg, this is the file ending (suffix) of the output files
	 * @param useCubaseNames should Cubase names/descriptions be used for naming output files?
	 * @param fixedName If useCubaseNames == false, this string will be used for naming the output audio files
	 * @param caller the MultiRegionExporterForCubase to call when done splitting
	 */
	public AudioOutputter(InputAudioFile inputFile, List<AudioBite> audioBites, String outputFolder, String soxPath, String ffmpegPath, String temporaryFolderPath, boolean convertWithFfmpeg, String convertWithFfmpegArguments, String convertWithFfmpegFileEnding, boolean useCubaseNames, String fixedName, ExporterEngine caller)
	{
		this.audioBites = audioBites;
		this.inputFile = inputFile;
		this.outputFolder = outputFolder;
		this.caller = caller;
		this.soxPath = soxPath;
		this.ffmpegPath = ffmpegPath;
		this.temporaryFolderPath = temporaryFolderPath;
		this.convertWithFfmpeg = convertWithFfmpeg;
		if (convertWithFfmpeg)
		{
			if (convertWithFfmpegArguments != null)
			{
				String[] splitStr = convertWithFfmpegArguments.trim().split("\\s+");
				this.convertWithFfmpegArguments = new ArrayList<String>();
				for (int i = 0; i < splitStr.length; i++)
				{
				    if (!splitStr[i].isBlank())
				    {
				        this.convertWithFfmpegArguments.add(splitStr[i]); 
				    }
				}
			}
		}
		this.convertWithFfmpegFileEnding = convertWithFfmpegFileEnding;
		this.useCubaseNames = useCubaseNames;
		this.fixedName = fixedName;
	}
	
	public void run() 
	{
		Debug.log("Running AudioOutputter thread");
		int successes = 0;
		if (convertWithFfmpeg)
		{
			
			for (int i = 0; i < audioBites.size(); i++)
			{
				caller.audioOutputterProcessTextCallback("Extracting and converting file "+(i+1)+" out of "+audioBites.size());
				AudioBite b = audioBites.get(i);
				try
				{
					if (this.isInterrupted())
					{
						Debug.log("Thread interrupted. Exiting.");
						return;
					}
					
					ArrayList<String> soxCmdAndArgs = getSoxCommand(b,i);
					
					String tempFileName = soxCmdAndArgs.get(2);//The split file to be converted
					
					ProcessBuilder soxPB = new ProcessBuilder(soxCmdAndArgs);
					
					Process soxP = soxPB.start();
					
					caller.registerStartedProcess(soxP);
					
					int soxResult = soxP.waitFor();
					
					caller.unregisterStartedProcess(soxP);
					
					if (this.isInterrupted())
					{
						//Try to delete temp file
						Utils.deleteFile(tempFileName);
						Debug.log("Thread interrupted. Exiting.");
						return;
					}
					
					if (soxResult == 0)//Success running sox command
					{
						//Then convert with ffmpeg
						ArrayList<String> ffmpegCmdAndArgs = getFFMPEGCommand(tempFileName,b,i);
						
						ProcessBuilder ffmpegPB = new ProcessBuilder(ffmpegCmdAndArgs);
						
						Process ffmpegP = ffmpegPB.start();
						
						caller.registerStartedProcess(ffmpegP);
						
						int ffmpegResult = ffmpegP.waitFor();
						
						caller.unregisterStartedProcess(ffmpegP);
						
						if (ffmpegResult == 0)//Success running FFMPEG command
						{
							successes += 1;
						}
						else
						{
						    ffmpegError = true;
						}
					}
					
					//Try to delete temp file
					Utils.deleteFile(tempFileName);
					
				}
				catch (InterruptedException e)
				{
					Debug.log("Thread interrupted. Exiting.");
					return;
				}
				catch (Exception e)
				{
					Debug.log("Exception caught while trying write audio file for AudioBite with name "+ b.getName() +":");
					e.printStackTrace();
				}
				caller.audioOutputterPercentageCallback((int)(((float)(i+1)/(float)audioBites.size())*100));
			}
			
		}
		else //Don't convert, just split
		{
			for (int i = 0; i < audioBites.size(); i++)
			{
				caller.audioOutputterProcessTextCallback("Extracting file "+(i+1)+" out of "+audioBites.size());
				AudioBite b = audioBites.get(i);
				try
				{
					if (this.isInterrupted())
					{
						Debug.log("Thread interrupted. Exiting.");
						return;
					}
					
					ArrayList<String> cmdAndArgs = getSoxCommand(b,i);
					ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
					
					Process p = pb.start();
					
					caller.registerStartedProcess(p);
					
					int result = p.waitFor();
					
					caller.unregisterStartedProcess(p);
					
					
					if (result == 0)
					{
						successes += 1;
					}
				}
				catch (InterruptedException e)
				{
					Debug.log("Thread interrupted. Exiting.");
					return;
				}
				catch (Exception e)
				{
					Debug.log("Exception caught while trying write audio file for AudioBite with name "+ b.getName() +":");
					e.printStackTrace();
				}
				caller.audioOutputterPercentageCallback((int)(((float)(i+1)/(float)audioBites.size())*100));
			}
		}
		if (successes == audioBites.size())
		{
			caller.audioOutputterProcessTextCallback("Finished successfully");
		}
		else
		{
			caller.audioOutputterProcessTextCallback("Finished with error(s)");
		}
		
		if (ffmpegError)
		{
		    caller.sendMessageToUser(UserMessageType.ERROR, "Error(s) occurred while converting with FFmpeg. Check arguments and filename extension");
		}
		
		caller.audioOutputterDoneCallback(successes, audioBites.size(),this);
	}
	
	/**
	 * Get the command to be sent to FFMPEG for conversion of each AudioBite
	 * @param inputFileName the temp file generated by SoX splitting
	 * @param b current AudioBite to be extracted
	 * @param index used for naming when ! useCubaseNames
	 * @return the command
	 */
	private ArrayList<String> getFFMPEGCommand(String inputFileName, AudioBite b, int index)
	{
		ArrayList<String> cmdAndArgs = new ArrayList<String>();
		
		cmdAndArgs.add(ffmpegPath);
		
		cmdAndArgs.add("-i");
		
		cmdAndArgs.add(inputFileName);
		
		if ((convertWithFfmpegArguments != null) && (convertWithFfmpegArguments.size() > 0))
		{
		    cmdAndArgs.addAll(convertWithFfmpegArguments);
		}
		
		cmdAndArgs.add("-y");
		
		String name = b.getName();
		if (!useCubaseNames)
		{
			name = fixedName+"_"+String.format("%04d", index+1);
		}
		cmdAndArgs.add(outputFolder+"/"+name+"."+this.convertWithFfmpegFileEnding);
		
		/*for (int i = 0; i < cmdAndArgs.size(); i++)
		{
		    Debug.log(cmdAndArgs.get(i));
		}*/
		
		return cmdAndArgs;
	}
	
	
	/**
	 * Get the command to be sent to SoX for each AudioBite
	 * @param b current AudioBite to be extracted
	 * @param index used for naming when ! useCubaseNames
	 * @return the command
	 */
	private ArrayList<String> getSoxCommand(AudioBite b, int index)
	{
		ArrayList<String> cmdAndArgs = new ArrayList<String>();
		
		cmdAndArgs.add(soxPath);
		
		cmdAndArgs.add(inputFile.getFilename());
		
		if (convertWithFfmpeg)
		{
			try 
			{
				File f = File.createTempFile("MREFC_temp_split_file","."+inputFile.getFileExtension(), new File(temporaryFolderPath));
				cmdAndArgs.add(f.getAbsolutePath());
			} 
			catch (IOException e) 
			{
				Debug.log("Exception caucht while trying to create temporary file for SoX splitting:");
				e.printStackTrace();
			}
		}
		else
		{
			String name = b.getName();
			if (!useCubaseNames)
			{
				name = fixedName+"_"+String.format("%04d", index+1);
			}
			cmdAndArgs.add(outputFolder+"/"+name+"."+inputFile.getFileExtension());
		}
		
		cmdAndArgs.add("trim");
		cmdAndArgs.add(String.format(Locale.US,"%.8f",b.getStartSec()));//start
		cmdAndArgs.add(String.format(Locale.US,"%.8f",(b.getFunctionalEndSec()-b.getStartSec())));//length
			
		return cmdAndArgs;
	}
	
}
