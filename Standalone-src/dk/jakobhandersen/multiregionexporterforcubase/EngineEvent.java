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

/**
 * Different types of events to be sent from ExporterEngine to UserInterface
 * @author Jakob Hougaard Andersen
 *
 */
public enum EngineEvent 
{
	CLEAR_AUDIO_FILE,
	READY_FOR_XML,
	NOT_READY_FOR_XML,
	READY_FOR_SPLIT,
	NOT_READY_FOR_SPLIT,
	READING_AUDIO_FILE,
	ERROR_READING_AUDIO_FILE,
	GENERATING_WAVEFORM,
	DONE_GENERATING_WAVEFORM,
	FILES_TO_BE_OVERWRITTEN,
	INPUT_FILE_TO_BE_OVERWRITTEN,
	OUTPUTTING_FILES,
	DONE_OUTPUTTING_FILES
}
