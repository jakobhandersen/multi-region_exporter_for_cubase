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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * A class based on Thread that builds an InputAudioFile
 * Uses SoXi to read audio properties
 * @author Jakob Hougaard Andersen
 *
 */
public class InputAudioFileBuilder extends Thread
{
	/**
	 * Full path to the audio file
	 */
	private String audioFileName;
	
	/**
	 * The MultiRegionExporterForCubase to be called when done
	 */
	private ExporterEngine caller;
	
	/**
	 * Path to SoXi shortcut
	 */
	private String soxiPath;

	/**
	 * Constructor
	 * @param audioFileName full path to the audio file
	 * @param soxiPath path to SoXi shortcut
	 * @param caller the MultiRegionExporterForCubase to be called when done
	 */
	public InputAudioFileBuilder(String audioFileName, String soxiPath, ExporterEngine caller)
	{
		this.audioFileName = audioFileName;
		this.caller = caller;
		this.soxiPath = soxiPath;
	}
	
	public void run()
	{
		Debug.log("Running InputAudioFileBuilder thread");
		try
		{
			//Get duration
			double duration = 0;
			ArrayList<String> cmdAndArgs = getSoxiCommand("-D");
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
			Process p = pb.start();
			caller.registerStartedProcess(p);
			String output = getOutputFromProcess(p);
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			caller.unregisterStartedProcess(p);
			if (output != null && (! output.isEmpty()))
			{
				duration = Double.parseDouble(output);
			}
			else
			{
				Debug.log("Could not get duration from file");
			}
			
			if (duration <= 0)
			{
				//The input audio file is not valid for further processing...
				caller.inputAudioFileBuilderCallback(this, new InputAudioFile(audioFileName,false));
				return;
			}
			
			
			//Get channels
			int channels = 0;
			cmdAndArgs = getSoxiCommand("-c");
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			pb = new ProcessBuilder(cmdAndArgs);
			p = pb.start();
			caller.registerStartedProcess(p);
			output = getOutputFromProcess(p);
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			caller.unregisterStartedProcess(p);
			if (output != null && (! output.isEmpty()))
			{
				channels = Integer.parseInt(output);
			}
			else
			{
				Debug.log("Could not get number of channels from file");
			}
			
			//Get sample rate
			float sampleRate = 0;
			cmdAndArgs = getSoxiCommand("-r");
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			pb = new ProcessBuilder(cmdAndArgs);
			p = pb.start();
			caller.registerStartedProcess(p);
			output = getOutputFromProcess(p);
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			caller.unregisterStartedProcess(p);
			if (output != null && (! output.isEmpty()))
			{
				sampleRate = Float.parseFloat(output);
			}
			else
			{
				Debug.log("Could not get sample rate from file");
			}
			
			//Get bit depth
			int bitDepth = 0;
			cmdAndArgs = getSoxiCommand("-b");
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			pb = new ProcessBuilder(cmdAndArgs);
			p = pb.start();
			caller.registerStartedProcess(p);
			output = getOutputFromProcess(p);
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			caller.unregisterStartedProcess(p);
			if (output != null && (! output.isEmpty()))
			{
				bitDepth = Integer.parseInt(output);
			}
			else
			{
				Debug.log("Could not get bit depth from file");
			}
			
			caller.inputAudioFileBuilderCallback(this, new InputAudioFile(audioFileName,duration,bitDepth,sampleRate,channels,true));
			
		}
		catch (Exception e)
		{
			Debug.log("Exception caught while trying to get audio info from soxi:");
			e.printStackTrace();
			caller.inputAudioFileBuilderCallback(this, new InputAudioFile(audioFileName,false));
		}
		
	}
	
	/**
	 * Gets the output from the SoXi processes
	 * @param p the Process from which to get output
	 * @return the output at a String
	 */
	private String getOutputFromProcess(Process p)
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		try 
		{
			while ( (line = reader.readLine()) != null) 
			{
			   builder.append(line);
			   if (this.isInterrupted())
			   {
				   return null;
			   }
			}
		} 
		catch (IOException e) 
		{
			Debug.log("Exception caught while trying to read output from process:");
			e.printStackTrace();
		}
		String result = builder.toString();
		return result;
	}
	
	/**
	 * Builds the individual SoXi commands
	 * @param parameter the parameter specified to SoXi
	 * @return the command
	 */
	private ArrayList<String> getSoxiCommand(String parameter)
	{
		ArrayList<String> cmdAndArgs = new ArrayList<String>();
		cmdAndArgs.add(soxiPath);
		cmdAndArgs.add(parameter);
		cmdAndArgs.add(audioFileName);
		return cmdAndArgs;
	}
}
