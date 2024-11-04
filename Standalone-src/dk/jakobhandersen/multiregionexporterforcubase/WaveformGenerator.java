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
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

/**
 * Class based on Thread that handles the reading of waveform data.
 * Uses FFMPEG to generate png file
 * @author Jakob Hougaard Andersen
 */
public class WaveformGenerator extends Thread
{
	/**
	 * The input file (InputAudioFile) pointing to the file to be read
	 */
	private InputAudioFile inputFile;
	
	/**
	 * The MultiRegionExporterForCubase to be called when done
	 */
	private ExporterEngine caller;
	
	/**
	 * Path to FFMPEG
	 */
	private String ffmpegPath;
	
	/**
	 * Path to the temporary files folder
	 */
	private String temporaryFolderPath;
	
	/**
	 * Width in pixels of waveform display
	 */
	private int waveformWidth;
	
	/**
	 * Height in pixels of waveform display
	 */
	private int waveformHeight;

	
	/**
	 * Constructor
	 * @param inputFile the input file (InputAudioFile) pointing to the file to be downsampled
	 * @param waveformFile output destination
	 * @param soxPath path to SoX
	 * @param waveformWidth width in pixels of waveform display
	 * @param caller the MultiRegionExporterForCubase to be called when done
	 */
	public WaveformGenerator(InputAudioFile inputFile, String ffmpegPath, String temporaryFolderPath, int waveformWidth, int waveformHeight, ExporterEngine caller)
	{
		this.inputFile = inputFile;
		this.caller = caller;
		this.ffmpegPath = ffmpegPath;
		this.temporaryFolderPath = temporaryFolderPath;
		this.waveformWidth = waveformWidth;
		this.waveformHeight = waveformHeight;
	}
	
	public void run() 
	{
		Debug.log("Running WaveformGenerator thread");
		boolean success = false;
		String waveformPngFile = null;
		try
		{
			if (inputFile.getLength() > 0)
			{
				
				ArrayList<String> cmdAndArgs = getFFMPEGCommand(inputFile.getFilename());
				
				waveformPngFile = cmdAndArgs.get(8);
				
				ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
				
				Process p = pb.start();
				
				caller.registerStartedProcess(p);
				
				int result = p.waitFor();
				
				caller.unregisterStartedProcess(p);
				
				if (result == 0)
				{
					success = true;
				}
			}
		}
		catch (Exception e)
		{
			Debug.log("Exception caught while trying to create waveform:");
			e.printStackTrace();
		}
		
		caller.waveformGeneratorCallback(this,success, inputFile,waveformPngFile);
	}
	
	
	/**
	 * Get the command to be sent to FFMPEG for generation of waveform png
	 * @param inputFileName path to audio file for waveform
	 * @return the command
	 */
	private ArrayList<String> getFFMPEGCommand(String inputFileName)
	{
		ArrayList<String> cmdAndArgs = new ArrayList<String>();
		
		cmdAndArgs.add(ffmpegPath);
		
		cmdAndArgs.add("-i");
		
		cmdAndArgs.add(inputFileName);
		
		cmdAndArgs.add("-y");
		
		cmdAndArgs.add("-filter_complex");
		
		//Note that below the scaling is set to square-root in an attempt to make waveform more clear when shown on a low resolution.
		//This, however, also makes the visual representation dynamics less 'true'.
		cmdAndArgs.add("showwavespic=split_channels=1:s="+waveformWidth+"x"+waveformHeight+":colors=0x000000:scale=sqrt");
		
		cmdAndArgs.add("-frames:v");
		
		cmdAndArgs.add("1");
		
		try 
		{
			File f = File.createTempFile("MREFC_temp_waveform_file",".png", new File(temporaryFolderPath));
			cmdAndArgs.add(f.getAbsolutePath());
		}
		catch (IOException e) 
		{
			Debug.log("Exception caucht while trying to create temporary file for waveform generation:");
			e.printStackTrace();
		}
		
		
		return cmdAndArgs;
	}
	
}
