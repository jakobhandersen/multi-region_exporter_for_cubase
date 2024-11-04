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
import java.util.List;

/**
 * This interface is the 'main' interface and represents the graphic user interface.
 * However the actual functionality of the Multi-region Exporter is handled by the class ExporterEngine.
 * It might be a little bit of a strange setup with this UserInterface communicating with the ExporterEngine often through 'EngineEvents'.
 * The reason for this is that the project was first made as a combination of a Max/MSP user interface and a Java 'external'.
 * These two separate programs had to communicate through messages.
 *
 */
public interface UserInterface 
{
	/**
	 * Receive reference to currently read InputAudioFile from ExporterEngine
	 * @param f
	 */
	public void audioFileRead(InputAudioFile f);
	
	/**
	 * Is the computer a mac
	 * @return true if mac
	 */
	public boolean computerIsMac();
	
	/**
	 * Delete any range markers
	 */
	public void deleteRangeMarkers();
	
	/**
	 * Receives EngineEvent from ExporterEngine
	 * @param e
	 */
	public void receiveEvent(EngineEvent e);
	
	/**
	 * Show a message to the user
	 * @param type
	 * @param message
	 */
	public void sendMessageToUser(UserMessageType type, String message);
	
	/**
	 * Tells user interface how the outputting of files is progressing
	 * @param percentage (0 to 100) used in progress bar
	 */
	public void setOuputPercentage(int percentage);
	
	/**
	 * Tells user interface what the AudioOutputter is doing right now
	 * @param text description shown to user
	 */
	public void setOutputProcessText(String text);
	
	/**
	 * Set the markers showing the different regions.
	 * This is done by reading info in the received AudioBites
	 * @param bites
	 */
	public void setRangeMarkers(List<AudioBite> bites);
	
	/**
	 * Receive data from created waveform
	 * @param waveformData
	 */
	public void waveformCreated(String waveformPngFile);
}
