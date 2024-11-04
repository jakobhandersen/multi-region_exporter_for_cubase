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
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Class that contains utility function(s)
 * @author Jakob Hougaard Andersen
 *
 */
public class Utils 
{
	/**
	 * Function for deleting folder with files/folders inside (calling itself recursively)
	 * Found here http://javarevisited.blogspot.dk/2015/03/how-to-delete-directory-in-java-with-files.html
	 * @param dir directory to delete
	 * @return true if deletion was success, otherwise false
	 */
	public static boolean deleteDirectory(File dir) 
	{ 
		try
		{
			if (dir.isDirectory()) 
			{ 
				File[] children = dir.listFiles(); 
				for (int i = 0; i < children.length; i++) 
				{ 
					boolean success = deleteDirectory(children[i]); 
					if (!success) 
					{ 
						return false; 
					} 
					
				} 
				
			} 
		
			// either file or an empty directory 
			
			boolean success =  dir.delete();
			if (! success)
			{
				Debug.log("Error deleting file while deleting directory : " + dir.getName()); 
			}
			return success;
		}
		catch (Exception e)
		{
			Debug.log("Exception caught while trying to delete directory:");
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Delete a file
	 * @param fileName
	 */
	public static  boolean deleteFile(String fileName)
	{
		//Try to delete tempFile
		try
		{
			File file = new File(fileName);
			if (file.delete())
			{
				//Debug.log("File ("+fileName+") was successfully deleted");
				return true;
			}
			else
			{
				Debug.log("File ("+fileName+") could not be deleted");
				return false;
			}
		}
		catch (Exception e)
		{
			Debug.log("Exception caught while trying to delete file "+fileName+": ");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get the file separator string for current platform
	 * @return
	 */
	public static String getFileSeparator()
	{
		return System.getProperty("file.separator");
	}
	
	/**
	 * Get the path of this jar file
	 * @param foldersUp number of directories to go up from this location
	 * @return path
	 */
	public static String getJarPath(int foldersUp)
	{
		File f = null;
		try 
		{
			f = new File(MultiRegionExporterForCubase.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} 
		catch (URISyntaxException e) 
		{
			e.printStackTrace();
		}
		try 
		{
			for (int i = 0; i < foldersUp; i++)
			{
				f = new File(f.getParent());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try 
		{
			return f.getCanonicalPath();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Creates a valid string to be used as a file name based on input
	 * @param input desired file name
	 * @return valid file name based on desired name
	 */
	public static String getValidFileNameString(String input)
	{
		return input.replaceAll("[\\/:*?\"<>|]", "_");
	}

}
