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
import java.util.ArrayList;
import java.util.List;
/**
 * An AudioSplitter (Thread) implementation that uses FFMPEG.
 * Not currently in use
 * @author Jakob Hougaard Andersen
 *
 */
public class FFMPEGSplitter extends AudioSplitter
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
	 * Path to ffmpeg
	 */
	private String ffmpegPath;
	
	/**
	 * Should cubase names/descriptions be used for naming output files?
	 */
	private boolean useCubaseNames;
	
	/**
	 * If useCubaseNames == false, this string will be used for naming the output audio files
	 */
	private String fixedName;
	
	/**
	 * Constructor
	 * @param inputFile the audio input file (AudioInputFile) that should be split.
	 * @param audioBites list of AudioBites that the inputFile should be split into.
	 * @param outputFolder where to output the files
	 * @param ffmpegPath path to ffmpeg
	 * @param useCubaseNames should cubase names/descriptions be used for naming output files?
	 * @param fixedName if useCubaseNames == false, this string will be used for naming the output audio files
	 * @param caller the MultiRegionExporterForCubase to call when done splitting
	 */
	public FFMPEGSplitter(InputAudioFile inputFile, List<AudioBite> audioBites, String outputFolder, String ffmpegPath, boolean useCubaseNames, String fixedName, ExporterEngine caller)
	{
		this.audioBites = audioBites;
		this.inputFile = inputFile;
		this.outputFolder = outputFolder;
		this.caller = caller;
		this.ffmpegPath = ffmpegPath;
		this.useCubaseNames = useCubaseNames;
		this.fixedName = fixedName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() 
	{
		Debug.log("Running FFMPEGSplitter thread");
		int successes = 0;
		for (int i = 0; i < audioBites.size(); i++)
		{
			AudioBite b = audioBites.get(i);
			try
			{
				//FFMPEG here....
				ArrayList<String> cmdAndArgs = getFFMPEGCommand(b,i);
				
				ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
				Process p = pb.start();
				int result = p.waitFor();
				if (result == 0)
				{
					successes += 1;
				}
			}
			catch (Exception e)
			{
				Debug.log("Exception caught while trying write audio file for AudioBite with name "+ b.getName() +":");
				e.printStackTrace();
			}
		}
		caller.audioSplitterCallback(successes, audioBites.size(),this);
	}
	
	/**
	 * Get the command to be sent to FFMPEG for each AudioBite
	 * @param b current AudioBite to be extracted
	 * @param index used for naming when ! useCubaseNames
	 * @return the command
	 */
	private ArrayList<String> getFFMPEGCommand(AudioBite b, int index)
	{
		ArrayList<String> cmdAndArgs = new ArrayList<String>();
		
		cmdAndArgs.add(ffmpegPath);
		
		cmdAndArgs.add("-ss");//Seek to start in input stream
		cmdAndArgs.add(""+b.getStart());
		
		cmdAndArgs.add("-i");
		cmdAndArgs.add(inputFile.getFilename());
		
		cmdAndArgs.add("-t");//length of output stream
		cmdAndArgs.add(""+(b.getFunctionalEnd()-b.getStart()));
		
		cmdAndArgs.add("-acodec");//Copy audio codec
		cmdAndArgs.add("copy");
		
		cmdAndArgs.add("-y");//Overwrite
		
		String name = b.getName();
		if (!useCubaseNames)
		{
			name = fixedName+"_"+String.format("%04d", index+1);
		}
		
		cmdAndArgs.add(outputFolder+"/"+name+"."+inputFile.getFileExtension());
		return cmdAndArgs;
	}
	
}
