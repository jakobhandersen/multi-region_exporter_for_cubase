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
import java.io.*;
import WavFile.*;

/**
 * Class based on Thread that handles the creation of the downsampled waveform audio file.
 * Uses SoX to do the downsampling.
 * @author Jakob Hougaard Andersen
 */
public class WaveformGenerator extends Thread
{
	/**
	 * The input file (InputAudioFile) pointing to the file to be downsampled
	 */
	private InputAudioFile inputFile;
	
	/**
	 * Output destination
	 */
	private String waveformFile;
	
	/**
	 * The MultiRegionExporterForCubase to be called when done
	 */
	private ExporterEngine caller;
	
	/**
	 * Path to SoX
	 */
	private String soxPath;
	
	/**
	 * Width in pixels of waveform display
	 */
	private int waveformWidth;

	
	/**
	 * Constructor
	 * @param inputFile the input file (InputAudioFile) pointing to the file to be downsampled
	 * @param waveformFile output destination
	 * @param soxPath path to SoX
	 * @param waveformWidth width in pixels of waveform display
	 * @param caller the MultiRegionExporterForCubase to be called when done
	 */
	public WaveformGenerator(InputAudioFile inputFile, String waveformFile, String soxPath, int waveformWidth, ExporterEngine caller)
	{
		this.inputFile = inputFile;
		this.waveformFile = waveformFile;
		this.caller = caller;
		this.soxPath = soxPath;
		this.waveformWidth = waveformWidth;
	}
	
	public void run() 
	{
		Debug.log("Running WaveformGenerator thread");
		boolean success = false;
		double[][] waveFormData = new double[waveformWidth][2];
		try
		{
			//Sox here....
			ArrayList<String> cmdAndArgs = getSoxCommand();
			
			ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
			Process p = pb.start();
			caller.registerStartedProcess(p);
			int result = p.waitFor();
			if (this.isInterrupted())
			{
				Debug.log("Thread interrupted. Exiting.");
				return;
			}
			caller.unregisterStartedProcess(p);
			if (result == 0)//succes with sox conversion
			{
			
				File f = new File(waveformFile);
				WavFile wavFile = WavFile.openWavFile(f);
				wavFile.display();
				int numChannels = wavFile.getNumChannels();
				long numFrames = wavFile.getNumFrames();
				double numFramesPerWidthPixel = (double)numFrames / (double)waveformWidth;
				double offset = 0;
				double lastMin = 0;
				double lastMax = 0;
				for (int i = 0; i < waveformWidth; i++)
				{
					if (this.isInterrupted())
					{
						Debug.log("Thread interrupted. Exiting.");
						return;
					}
					long startFrame = (long)offset;
					offset += numFramesPerWidthPixel;
					long endFrame = (long) offset;
					double min = 0;
					double max = 0;
					int framesToRead = (int)(endFrame - startFrame);
					if (framesToRead > 0)
					{
						double[] buffer = new double[framesToRead * numChannels];
						int framesRead = wavFile.readFrames(buffer, framesToRead);
						// Loop through frames and look for minimum and maximum value
			            for (int s=0 ; s< framesRead * numChannels ; s++)
			            {
			               if (buffer[s] > max) max = buffer[s];
			               if (buffer[s] < min) min = buffer[s];
			            }
					}
					else
					{
						min = lastMin;
						max = lastMax;
					}
					waveFormData[i][0] = min;
					waveFormData[i][1] = max;
		            lastMin = min;
		            lastMax = max;
		            //Debug.log("min: "+ min +", max: "+max);
				}
				wavFile.close();
				System.gc();
				f.delete();
				success = true;
			}
		}
		catch (InterruptedException e)
		{
			Debug.log("Thread interrupted. Exiting.");
			return;
		}
		catch (Exception e)
		{
			Debug.log("Exception caught while trying to create waveform:");
			e.printStackTrace();
		}
		
		caller.waveformGeneratorCallback(this,success, inputFile,waveFormData);
	}
	
	/**
	 * Creates the command for SoX
	 * @return the command
	 */
	private ArrayList<String> getSoxCommand()
	{
		ArrayList<String> cmdAndArgs = new ArrayList<String>();
		
		cmdAndArgs.add(soxPath);
		
		cmdAndArgs.add(inputFile.getFilename());
		
		cmdAndArgs.add("-b");
		cmdAndArgs.add("8");
		
		cmdAndArgs.add(waveformFile);
		
		cmdAndArgs.add("channels");
		cmdAndArgs.add("1");//Mixdown to mono
		
		cmdAndArgs.add("rate");//Resample
		cmdAndArgs.add("-q");//quick algorithm
		double sampleRate = 8000;
		cmdAndArgs.add(sampleRate+"");
		
		//cmdAndArgs.add("downsample");
		//cmdAndArgs.add("200");
			
		return cmdAndArgs;
	}
	
}
