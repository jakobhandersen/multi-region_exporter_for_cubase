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
import java.text.DecimalFormat;

/**
 * Class holding info about an audio file
 * @author Jakob Hougaard Andersen
 *
 */
public class InputAudioFile 
{
	/**
	 * Complete file name (full path)
	 */
	private String filename;
	
	/**
	 * File extension (wav, aiff ...)
	 */
	private String fileExtension;
	
	/**
	 * Lenght in seconds
	 */
	private double length;
	
	/**
	 * Bit depth of each sample
	 */
	private int bitDepth;
	
	/**
	 * Samples per second (Hz)
	 */
	private float sampleRate;
	
	/**
	 * Number of audio channels
	 */
	private int channels;
	
	/**
	 * Is the file valid / does it seem valid for further processing
	 */
	private boolean isValid;
	
	/**
	 * Constructor
	 * @param filename
	 * @param length
	 * @param bitDepth
	 * @param sampleRate
	 * @param channels
	 * @param isValid
	 */
	public InputAudioFile(String filename, double length, int bitDepth, float sampleRate, int channels, boolean isValid)
	{
		this.filename = filename;
		this.length = length;
		this.fileExtension = getFileExtension(filename);
		this.bitDepth = bitDepth;
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.isValid = isValid;
	}
	
	/**
	 * Constructor with minimum initializing
	 * @param fileName
	 * @param isValid
	 */
	public InputAudioFile(String fileName, boolean isValid)
	{
		this.isValid = isValid;
		this.filename = fileName;
	}
	
	/**
	 * @return
	 */
	public String getFilename()
	{
		return filename;
	}
	
	/**
	 * @return
	 */
	public double getLength()
	{
		return length;
	}
	
	/**
	 * @return
	 */
	public String getFileExtension()
	{
		return fileExtension;
	}
	
	/**
	 * @return
	 */
	public int getChannels()
	{
		return channels;
	}
	
	/**
	 * @return
	 */
	public int getBitDepth()
	{
		return bitDepth;
	}
	
	/**
	 * @return
	 */
	public float getSampleRate()
	{
		return sampleRate;
	}
	
	/**
	 * Extract the file extension of a given file name
	 * @param fileName
	 * @return
	 */
	private  String getFileExtension(String fileName) 
	{
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
        {
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        }
        else 
        {
        	return "";
        }
    }
	
	/**
	 * @return
	 */
	public boolean getIsValid()
	{
		return isValid;
	}
	
	/**
	 * @return a string with info on this audio file
	 */
	public String getInfoString()
	{
		String channelsStr;
		if (channels > 0)
		{
			channelsStr = channels + "";
		}
		else
		{
			channelsStr = "?";
		}
		
		String lengthStr = getLengthString();
		
		String sampleRateStr;
		if (sampleRate > 0)
		{
			sampleRateStr = sampleRate + " Hz";
		}
		else
		{
			sampleRateStr = "? Hz";
		}
		
		String bitDepthStr;
		if (bitDepth > 0)
		{
			bitDepthStr = bitDepth + "";
		}
		else
		{
			bitDepthStr = "?";
		}
		
		return "channels: "+channelsStr+", length: "+lengthStr+", sample rate: "+sampleRateStr+", bit depth: "+bitDepthStr;
	}
	
	/**
	 * @return a string with the length formatted as hours, minutes and seconds
	 */
	private String getLengthString()
	{
		if (length > 0)
		{
			double hourPart = Math.floor(length / (double)3600);
			double rest = length - (hourPart * (double)3600);
			double minutePart = Math.floor(rest/(double)60);
			double secondPart = rest-(minutePart *(double)60);
			String str = "";
			if (hourPart > 0)
			{
				str += (int)hourPart + "h ";
				str += (int)minutePart + "m ";
			}
			else if (minutePart > 0)
			{
				str += (int)minutePart + "m ";
			}
			str += new DecimalFormat("0.##").format(secondPart) + "s";
			return str;
		}
		else
		{
			return "?";
		}
	}
}
