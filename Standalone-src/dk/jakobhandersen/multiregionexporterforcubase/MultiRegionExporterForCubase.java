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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;


import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.widgets.Combo;


/**
 * This class is the 'main' class and represents the graphic user interface.
 * However the actual functionality of the Multi-region Exporter is handled by the class ExporterEngine.
 * It might be a little bit of a strange setup with this UserInterface communicating with the ExporterEngine often through 'EngineEvents'.
 * The reason for this is that the project was first made as a combination of a Max/MSP user interface and a Java 'external'.
 * These two separate programs had to communicate through messages.
 * 
 * This class might be a little bit messy and it is not as well documented as the other classes. 
 * This is because it was largely made with the SWT graphical user interface in Eclipse and because many variables are more or less self-explaining.
 *
 */
public class MultiRegionExporterForCubase implements UserInterface
{
	/**
	 * Are we running on a Mac?
	 * Note that SWT behaves a little bit different on Windows and Mac.
	 * Therefore we need to do some conditional layout etc.
	 */
	public static boolean isMac;
	
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) 
	{	
		try 
		{
			System.setProperty("user.dir", new File(MultiRegionExporterForCubase.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath());
			Display.setAppName("Multi-region Exporter - for Cubase");
			MultiRegionExporterForCubase window = new MultiRegionExporterForCubase();
			window.open();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		Debug.log("Exiting system");
		System.exit(0);
	}
	//SWT variables
	//Note that some SWT variables (buttons etc.) need to be accessible in the whole class while others are only defined in the createContents function.
	protected Shell shell;
	protected Display display;
	private Device device = Display.getCurrent ();
	private Button btnLoadAudioFile;
	private Button btnLoadTrackFile;
	private Button btnOutputFiles;
	private Table logWindowTable;
	private CLabel lblAudioFileName;
	private CLabel lblAudioFileInfo;
	private CLabel lblTrackFileName;
	private Label waveformLabel;
	private Combo comboConstantVariableBitrate;
	private Combo comboBitrateVariable;
	private Combo comboBitrateConstant;
	
	
	//Other variables
	
	/**
	 * Reference to the ExporterEngine handling the actual functionality
	 */
	private ExporterEngine engine;
	
	/**
	 * File name of the last loaded XML file
	 */
	private String lastLoadedXmlFileName;
	
	/**
	 * Chosen fixed name for file naming
	 */
	private Text txtFixedName;
	
	/**
	 * Reference to the currently loaded InputAudioFile (passed by ExporterEngine)
	 */
	private InputAudioFile currentInputAudioFile;
	
	/**
	 * Waveform data. Min and max for each horizontal pixel.
	 */
	private String currentWaveformPng;
	
	/**
	 * Rectangles representing the range/region markers
	 */
	private Rectangle[] rangeMarkers;
	
	/**
	 * Color of the range markers
	 */
	private Color rangeMarkerColor = new Color(device,0,255,0);

	
	/**
	 * Alpha of the range markers
	 */
	private int rangeMarkerAlpha = 110;
	
	/**
	 * Path to the online documentation
	 */
	private String pathToOnlineDocumentation;
	
	/**
	 * Path to the local documentation file
	 */
	private String pathToLocalDocumentation;
	
	/**
	 * Above path represented as File
	 */
	private File localDocumentationFile;
	
	/**
	 * Mapping from constant mp3 bitrate menu to ffmpeg string argument
	 */
	private String[] constantMp3BitrateArguments = new String[] {"-b:a 8k","-b:a 16k","-b:a 24k","-b:a 32k","-b:a 40k","-b:a 48k","-b:a 64k","-b:a 80k","-b:a 96k","-b:a 112k","-b:a 128k","-b:a 160k","-b:a 192k","-b:a 224k","-b:a 256k","-b:a 320k"};
	
	/**
	 * Mapping from variable mp3 bitrate menu to ffmpeg string argument
	 */
	private String[] variableMp3BitrateArguments = new String[] {"-q:a 9","-q:a 8","-q:a 7","-q:a 6","-q:a 5","-q:a 4","-q:a 3","-q:a 2","-q:a 1","-q:a 0"};
	
	/**
	 * Dialog shown when outputting files
	 */
	private OutputtingDialog outputtingDialog;
	
	/**
	 * Text shown when the waveform is being generated
	 */
	private String generatingWaveformText = "";
	
	/**
	 * Milliseconds between each tick of generatingWaveformTextTimer
	 */
	private final int generatingWaveformTextTimerTime = 250;
	
	
	/**
	 * Timer to handle movement in the 'generating waveform' user notification
	 */
	private Runnable generatingWaveformTextTimer = new Runnable() 
	{
		int i = 0;
		public void run() 
		{
			String dotString = "";
			for (int j = 0; j < i; j++)
			{
				dotString += ".";
			}
			generatingWaveformText = dotString + "generating waveform" + dotString;
			waveformLabel.redraw();
			i = (i + 1) % 4;
	        display.timerExec(generatingWaveformTextTimerTime, this);
		}
	};
	
	@Override
	public void audioFileRead(InputAudioFile f) 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    	shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
		    	currentInputAudioFile = f;
		    	btnLoadAudioFile.setEnabled(true);
		    	lblAudioFileName.setText(f.getFilename());
		    	lblAudioFileInfo.setText(f.getInfoString());
		    }
		});
		
	}

	@Override
	public boolean computerIsMac()
	{
		return isMac;
	}
	
	@Override
	public void deleteRangeMarkers() 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    	rangeMarkers = null;
		    	waveformLabel.redraw();
		    }
		});
	}
	
	/**
	 * Open the window.
	 */
	public void open() 
	{
		isMac = SWT.getPlatform().equals("cocoa");
		engine = new ExporterEngine(this);
		display = Display.getDefault();
		
		
		createContents();
		
		setup();
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) 
		{
			if (!display.readAndDispatch()) 
			{
				display.sleep();
			}
		}
		
		engine.cleanUp();
	}
	
	@Override
	public void receiveEvent(EngineEvent e) 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
				switch(e)
				{
				case CLEAR_AUDIO_FILE:
					clearAudioFile();
					break;
					
				case READY_FOR_XML:
					btnLoadTrackFile.setEnabled(true);
					btnLoadTrackFile.forceFocus();
					break;
					
				case NOT_READY_FOR_XML:
					btnLoadTrackFile.setEnabled(false);
					break;
					
				case READY_FOR_SPLIT:
					btnOutputFiles.setEnabled(true);
					btnOutputFiles.forceFocus();
					lblTrackFileName.setText(lastLoadedXmlFileName);
					break;
					
				case NOT_READY_FOR_SPLIT:
					btnOutputFiles.setEnabled(false);
					lblTrackFileName.setText("none");
					break;
					
				case READING_AUDIO_FILE:
					btnLoadAudioFile.setEnabled(false);
					shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));
					break;
					
				case ERROR_READING_AUDIO_FILE:
					shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
					btnLoadAudioFile.setEnabled(true);
					btnLoadAudioFile.forceFocus();
					break;
					
				case GENERATING_WAVEFORM:
					display.timerExec(0, generatingWaveformTextTimer);
					break;
					
				case DONE_GENERATING_WAVEFORM:
					display.timerExec(-1, generatingWaveformTextTimer);
					generatingWaveformText = "";
					waveformLabel.redraw();
					break;
					
				case FILES_TO_BE_OVERWRITTEN:
					doOverwriteDialog();
					break;
					
				case INPUT_FILE_TO_BE_OVERWRITTEN:
					doInputFileOverwriteErrorDialog();
					break;
					
				case OUTPUTTING_FILES:
					startOutputtingFiles();
					break;
					
				case DONE_OUTPUTTING_FILES:
					stopOutputtingFiles();
					break;
					
					default:
						break;
				}
		    }
		});
		
	}
	
	@Override
	public void sendMessageToUser(UserMessageType type, String message) 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    	//Non-wrapped solution
				/*TableItem tableItem = new TableItem(table_1, SWT.NONE);
				tableItem.setText(getStartStringForUserMessageType(type) + message);
				tableItem.setForeground(getColorForUserMessageType(type));
				table_1.showItem(tableItem);*/
				
				//Wrapped text solution
				List<String> splitStrings = getWrappedString(getStartStringForUserMessageType(type) + message,573,logWindowTable);
				for (int i = 0; i < splitStrings.size(); i++)
				{
					TableItem tableItem = new TableItem(logWindowTable, SWT.NONE);
					tableItem.setText(splitStrings.get(i));
					tableItem.setForeground(getColorForUserMessageType(type));
					logWindowTable.showItem(tableItem);
				}
		    }
		});
		
	}
	
	@Override
	public void setOuputPercentage(int percentage) 
	{
		outputtingDialog.setProgressPercentage(percentage);
	}
	
	
	@Override
	public void setOutputProcessText(String text)
	{
		outputtingDialog.setProcessText(text);
	}
	
	@Override
	public void setRangeMarkers(List<AudioBite> bites) 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    	if (currentInputAudioFile != null)
		    	{
			    	rangeMarkers = new Rectangle[bites.size()];
			    	int waveformWidth = waveformLabel.getBounds().width;
		    		int waveformHeight = waveformLabel.getBounds().height;
			    	for (int i = 0; i < bites.size(); i++)
			    	{
			    		AudioBite b = bites.get(i);
			    		double startFraction = b.getStart() / currentInputAudioFile.getLength();
			    		double lengthFraction = (b.getFunctionalEnd() - b.getStart()) / currentInputAudioFile.getLength();
			    		rangeMarkers[i] = new Rectangle((int)(startFraction * waveformWidth),0,(int)(lengthFraction * waveformWidth),waveformHeight);
			    	}
		    	}
		    	else
		    	{
		    		Debug.log("Error: Can't set range markers since currentInputAudioFile == null");
		    		rangeMarkers = null;
		    	}
		    	waveformLabel.redraw();
		    }
		});
		
	}
	
	@Override
	public void waveformCreated(String waveformPngFile) 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    	currentWaveformPng = waveformPngFile;
		    	waveformLabel.redraw();
		    }
		});
		
	}
	
	/**
	 * Clear info on currently loaded audio file
	 */
	private void clearAudioFile()
	{
		//Clear text related to audio file
		lblAudioFileName.setText("none");
		lblAudioFileInfo.setText("");
		
		//clear waveform
		generatingWaveformText = "";
		display.timerExec(-1, generatingWaveformTextTimer);
		if ((currentWaveformPng != null) && (!currentWaveformPng.isEmpty()))
		{
			Utils.deleteFile(currentWaveformPng);
			currentWaveformPng = null;
		}
		waveformLabel.redraw();
		
		//clear currentInputAudioFile
		currentInputAudioFile = null;
		
	}
	
	/**
	 * Show dialog when trying to overwrite input audio file
	 */
	private void doInputFileOverwriteErrorDialog()
	{
		MessageBox messageDialog = new MessageBox(shell, 
		        SWT.ICON_ERROR | 
		        SWT.OK);
	    messageDialog.setText("Error outputting files");
	    messageDialog.setMessage("Outputting to the specified folder would overwrite the loaded input audio file since it shares the name of one of the files to be extracted. This is not allowed / possible. Select another output folder or change file naming settings.");
	    messageDialog.open();
	}

	/**
	 * Show overwrite files dialog 
	 */
	private void doOverwriteDialog()
	{
		MessageBox messageDialog = new MessageBox(shell, 
			        SWT.ICON_QUESTION | 
			        SWT.OK
			        | SWT.CANCEL);
	    messageDialog.setText("Overwrite files");
	    messageDialog.setMessage("One or more of the files about to be created share the name of a file already existing in the specified output folder.\n\nDo you want to overwrite these files.");
	    int returnCode = messageDialog.open();
	    if (returnCode == 32)//ok
	    {
	    	Debug.log("Overwrite accepted");
	    	engine.createFilesOverwriteAccepted();
	    }
	    else
	    {
	    	Debug.log("Overwrite canceled");
	    }
	}

	/**
	 * Get relevant color of user message of type t
	 * @param t
	 * @return
	 */
	private Color getColorForUserMessageType(UserMessageType t)
	{
		Color ret = SWTResourceManager.getColor(SWT.COLOR_BLACK);
		switch (t)
		{
		case ERROR:
			ret = SWTResourceManager.getColor(SWT.COLOR_DARK_RED);
			break;
			
		case WARNING:
			ret = SWTResourceManager.getColor(SWT.COLOR_DARK_BLUE);
			break;
			
		case SUCCESS:
			ret = SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
			break;
		
			default: break;
		}
		return ret;
	}

	/**
	 * Get relevant start string for user message of type t
	 * @param t 
	 * @return 
	 */
	private String getStartStringForUserMessageType(UserMessageType t)
	{
		String ret = "";
		switch (t)
		{
		case ERROR:
			ret = "ERROR: ";
			break;
			
		case WARNING:
			ret = "WARNING: ";
		
			default: break;
		}
		return ret;
	}
	

	/**
	 * Returns wrapped string (List<String>)
	 * @param fullString
	 * @param maxWidth
	 * @param control
	 * @return
	 */
	private List<String> getWrappedString(String fullString,int maxWidth, Control control)
	{
		GC gc = new GC(control);
		int stringWidth = gc.stringExtent(fullString).x;
		List<String> splitStrings = new ArrayList<String>();
		if (stringWidth <= maxWidth)
		{
			splitStrings.add(fullString);
			return splitStrings;
		}
		String restString = fullString;
		int restStringWidth = stringWidth;
		while (restStringWidth > maxWidth)
		{
			String biteFromRest;
			String restFromRest;
			int startI = restString.length()-1;
			for (int i = startI; i > 0; i--)
			{
				biteFromRest = restString.substring(0,i);
				restFromRest = restString.substring(i);
				if (gc.stringExtent(biteFromRest).x <= maxWidth)
				{
					splitStrings.add(biteFromRest);
					restString = restFromRest;
					restStringWidth = gc.stringExtent(restString).x;
					break;
				}
			}
		}
		splitStrings.add(restString);
		return splitStrings;
	}

	/**
	 * Called when load audio file button is pressed
	 */
	private void loadAudioFile()
	{
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
        fd.setText("Load an audio file");
        String[] filterExt = {"*.wav;*.aiff;*.aif;*.w64;*.flac"};
        fd.setFilterExtensions(filterExt);
        String selected = fd.open();
        if (selected != null)
        {
        	engine.readInputAudioFile(selected);
        }
	}
	
	/**
	 * Called when the load track file button is pressed
	 */
	private void loadTrackFile()
	{
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
        fd.setText("Load a Cubase track file (.xml)");
        String[] filterExt = {"*.xml"};
        fd.setFilterExtensions(filterExt);
        String selected = fd.open();
        if (selected != null)
        {
        	this.lastLoadedXmlFileName = selected;
        	engine.readXML(selected);
        }
	}
	
	
	/**
	 * Called when the output files button is pressed
	 */
	private void outputFilesToFolder()
	{
		DirectoryDialog dd = new DirectoryDialog(shell, SWT.SAVE);
        dd.setText("Choose where to put the audio files");
        String selected = dd.open();
        if (selected != null)
        {
        	engine.setOutputFolder(selected);
        }
	}
	
	/**
	 * Called when mp3 conversion bitrate is changed/initialized
	 */
	private void sendMp3ConversionArgToEngine()
	{
		if (comboConstantVariableBitrate.getSelectionIndex() == 0)//Constant bitrate
		{
			engine.setMp3ConversionBitrateArgument(constantMp3BitrateArguments[comboBitrateConstant.getSelectionIndex()]);
		}
		else//Variable bitrate
		{
			engine.setMp3ConversionBitrateArgument(variableMp3BitrateArguments[comboBitrateVariable.getSelectionIndex()]);
		}
	}
	
	/**
	 * Set up path(s). Calculated from paths relative to jar file.
	 */
	private void setPaths()
	{
		//Calculate paths
//		if (isMac)
//		{
//			String pathToContentsFolder = Utils.getJarPath(2);
//			String fileSeparator = Utils.getFileSeparator();
//			pathToLocalDocumentation = pathToContentsFolder + fileSeparator + "documentation" + fileSeparator + "documentation.pdf";
//		}
		if (!isMac)//	Desktop.getDesktop().open() apparently doesn't work in bundled OS X App anymore, so path to local documentation is only relevant on Windows
		{
			String pathToMainFolder = Utils.getJarPath(1);
			String fileSeparator = Utils.getFileSeparator();
			pathToLocalDocumentation = pathToMainFolder + fileSeparator + "documentation" + fileSeparator + "documentation.pdf";
			Debug.log("Path to local documentation set to: " + pathToLocalDocumentation);
			localDocumentationFile = new File(pathToLocalDocumentation);
		}
		
		pathToOnlineDocumentation = "http://jakobhandersen.dk/projects/multi-region-exporter/documentation/";
		
		Debug.log("Path to online documentation set to: " + pathToOnlineDocumentation);
		
	}
	
	/**
	 * Do some startup setting up
	 */
	private void setup()
	{
		btnOutputFiles.setEnabled(false);
		btnLoadTrackFile.setEnabled(false);
		setPaths();
		engine.setWaveformWidth(waveformLabel.getBounds().width);
		engine.setWaveformHeight(waveformLabel.getBounds().height);
		clearAudioFile();
		lblTrackFileName.setText("none");
		setUseCubaseNames();
		outputtingDialog = new OutputtingDialog(shell,SWT.APPLICATION_MODAL);
		
	}
	
	/**
	 * Tell engine to use Cubase names
	 */
	private void setUseCubaseNames()
	{
		engine.useCubaseNames();
	}
	
	/**
	 * Tell engine to use fixed name
	 * @param s the name to use
	 */
	private void setUseFixedName(String s)
	{
		engine.useFixedName(s);
	}

	/**
	 * Show the About dialog
	 */
	private void showAbout()
	{
		AboutDialog d = new AboutDialog(shell, SWT.APPLICATION_MODAL);
		d.open();
	}
	
	/**
	 * Do things when outputting files
	 */
	private void startOutputtingFiles()
	{
		shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));
		outputtingDialog.open();
	}
	
	/**
	 * Stop doing things when outputting files
	 */
	private void stopOutputtingFiles()
	{
		shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
		outputtingDialog.close();
	}
	
	/**
	 * Create contents of the window.
	 */
	protected void createContents() 
	{
		
		shell = new Shell();
		shell.setSize(800, 800);
		shell.setText("Multi-region Exporter - for Cubase");
		shell.setBackgroundMode(SWT.INHERIT_DEFAULT); 
		
		Group grpAudioFile = new Group(shell, SWT.NONE);
		grpAudioFile.setToolTipText("Info on loaded audio file");
		grpAudioFile.setFont(SWTResourceManager.getFont("Arial", 13, SWT.NORMAL));
		grpAudioFile.setText("Audio file:");
		grpAudioFile.setBounds(172, 106, 590, 63);
		if (! isMac)
		{
			grpAudioFile.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		}
		
		lblAudioFileName = new CLabel(grpAudioFile, SWT.NONE);
		lblAudioFileName.setBounds(10, 3, 561, 15);
		lblAudioFileName.setFont(SWTResourceManager.getFont("Arial", 12, SWT.ITALIC));
		lblAudioFileName.setText("none");
		lblAudioFileName.setToolTipText(grpAudioFile.getToolTipText());
		if (! isMac)
		{
			lblAudioFileName.setFont(SWTResourceManager.getFont("Arial", 10, SWT.ITALIC));
			lblAudioFileName.setBounds(10, 20, 561, 20);
		}
		
		lblAudioFileInfo = new CLabel(grpAudioFile, SWT.NONE);
		lblAudioFileInfo.setBounds(10, 20, 561, 15);
		lblAudioFileInfo.setFont(SWTResourceManager.getFont("Arial", 11, SWT.ITALIC));
		lblAudioFileInfo.setToolTipText(grpAudioFile.getToolTipText());
		if (! isMac)
		{
			lblAudioFileInfo.setFont(SWTResourceManager.getFont("Arial", 9, SWT.ITALIC));
			lblAudioFileInfo.setBounds(10, 40, 561, 15);
		}
		
		Group grpTrackFile = new Group(shell, SWT.NONE);
		grpTrackFile.setToolTipText("Loaded track file");
		grpTrackFile.setFont(SWTResourceManager.getFont("Arial", 13, SWT.NORMAL));
		grpTrackFile.setText("Cubase track file:");
		grpTrackFile.setBounds(172, 253, 590, 54);
		if (! isMac)
		{
			grpTrackFile.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		}
		
		lblTrackFileName = new CLabel(grpTrackFile, SWT.NONE);
		lblTrackFileName.setBounds(10, 5, 561, 15);
		lblTrackFileName.setFont(SWTResourceManager.getFont("Arial", 12, SWT.ITALIC));
		lblTrackFileName.setText("none");
		lblTrackFileName.setToolTipText(grpTrackFile.getToolTipText());
		if (! isMac)
		{
			lblTrackFileName.setBounds(10, 20, 561, 20);
			lblTrackFileName.setFont(SWTResourceManager.getFont("Arial", 10, SWT.ITALIC));
		}
		
		Label lblTitle = new Label(shell, SWT.NONE);
		lblTitle.setFont(SWTResourceManager.getFont("Arial", 19, SWT.NORMAL));
		lblTitle.setBounds(172, 29, 391, 25);
		lblTitle.setText("Multi-region Exporter");
		if (! isMac)
		{
			lblTitle.setFont(SWTResourceManager.getFont("Arial", 16, SWT.NORMAL));
		}
		
		Label lblForCubase = new Label(shell, SWT.NONE);
		lblForCubase.setText("for Cubase");
		lblForCubase.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		lblForCubase.setBounds(172, 51, 82, 25);
		if (! isMac)
		{
			lblForCubase.setBounds(172, 55, 82, 25);
			lblForCubase.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		}
		
		Label lblLogoImage = new Label(shell, SWT.NONE);
		lblLogoImage.setImage(SWTResourceManager.getImage(MultiRegionExporterForCubase.class, "/Assets/Logo85.png"));
		lblLogoImage.setBounds(50, 10, 94, 94);
		
		Label lblVersion = new Label(shell, SWT.NONE);
		lblVersion.setAlignment(SWT.RIGHT);
		lblVersion.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		lblVersion.setBounds(662, 29, 100, 25);
		lblVersion.setText(Constants.versionString);
		if (! isMac)
		{
			lblVersion.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		}
		
		btnLoadAudioFile = new Button(shell, SWT.NONE);
		if (!isMac)
		{
			btnLoadAudioFile = new Button(shell, SWT.WRAP);
		}
		btnLoadAudioFile.setToolTipText("Click here to load an input audio file.\r\nThis should be the full mixdown you exported from Cubase\r\ncontaining all the desired regions.");
		btnLoadAudioFile.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				loadAudioFile();
			}
		});
		btnLoadAudioFile.setBounds(30, 115, 131, 54);
		btnLoadAudioFile.setText("Load audio file");
		
		btnLoadTrackFile = new Button(shell, SWT.NONE);
		if (!isMac)
		{
			btnLoadTrackFile = new Button(shell, SWT.WRAP);
		}
		btnLoadTrackFile.setToolTipText("Click here to load a Cubase track file (XML)");
		btnLoadTrackFile.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				loadTrackFile();
			}
		});
		btnLoadTrackFile.setBounds(30, 253, 131, 54);
		btnLoadTrackFile.setText("Load Cubase\r\ntrack file (.xml)");
		
		btnOutputFiles = new Button(shell, SWT.NONE);
		if (!isMac)
		{
			btnOutputFiles = new Button(shell, SWT.WRAP);
		}
		btnOutputFiles.setToolTipText("Click here to select output destination and create extracted audio files");
		btnOutputFiles.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				outputFilesToFolder();
			}
		});
		btnOutputFiles.setText("Output audio\nfiles to folder");
		btnOutputFiles.setBounds(30, 350, 131, 54);
		
		
		if (isMac)
	    {
	        Menu systemMenu = Display.getDefault().getSystemMenu();
	        for (MenuItem systemItem : systemMenu.getItems())
	        {
	            if (systemItem.getID() == SWT.ID_ABOUT)
	            {
	                systemItem.addSelectionListener(new SelectionAdapter() 
	        		{
	        			@Override
	        			public void widgetSelected(SelectionEvent e) 
	        			{
	        				showAbout();
	        			}
	        		});
	            }
	            else  if (systemItem.getID() == SWT.ID_PREFERENCES)
	            {
	                systemItem.dispose();
	            }
	        }
	    }
		
		Menu appMenuBar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(appMenuBar);
		
		if (! isMac)
		{
			MenuItem mntmMultiregionExporterFor = new MenuItem(appMenuBar, SWT.CASCADE);
			mntmMultiregionExporterFor.setText("File");
			
			Menu fileMenu = new Menu(mntmMultiregionExporterFor);
			mntmMultiregionExporterFor.setMenu(fileMenu);
			
			MenuItem exitItem = new MenuItem(fileMenu, SWT.NONE);
			exitItem.setText("Exit\tAlt+F4");
			exitItem.addSelectionListener(new SelectionAdapter() 
			{
				@Override
				public void widgetSelected(SelectionEvent e) 
				{
					//Exit application
					shell.dispose();
				}
			});
		}
		
		MenuItem mntmMultiregionExporterFor = new MenuItem(appMenuBar, SWT.CASCADE);
		mntmMultiregionExporterFor.setText("Help");
		
		Menu helpMenu = new Menu(mntmMultiregionExporterFor);
		mntmMultiregionExporterFor.setMenu(helpMenu);
		
		if (! isMac)
		{
			MenuItem mntmAboutMultiregionExporter = new MenuItem(helpMenu, SWT.NONE);
			mntmAboutMultiregionExporter.addSelectionListener(new SelectionAdapter() 
			{
				@Override
				public void widgetSelected(SelectionEvent e) 
				{
					showAbout();
				}
			});
			mntmAboutMultiregionExporter.setText("About Multi-region Exporter");
		}
		
		MenuItem mntmOnlineDocumentation = new MenuItem(helpMenu, SWT.NONE);
		mntmOnlineDocumentation.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
			    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			        try 
			        {
			            desktop.browse(new URI(pathToOnlineDocumentation));
			        } 
			        catch (Exception ex) 
			        {
			            ex.printStackTrace();
			        }
			    }
			}
		});
		mntmOnlineDocumentation.setText("Online Documentation");
		
		if (! isMac) //	Desktop.getDesktop().open() apparently doesn't work in bundled OS X App anymore, so only add link to local documentation on Windows
		{
			MenuItem mntmLocalDocumentation = new MenuItem(helpMenu, SWT.NONE);
			mntmLocalDocumentation.addSelectionListener(new SelectionAdapter() 
			{
				@Override
				public void widgetSelected(SelectionEvent e) 
				{
					
				        try 
				        {
				        	Desktop.getDesktop().open(localDocumentationFile);
				        } 
				        catch (Exception ex) 
				        {
				            ex.printStackTrace();
				        }
				    
				}
			});
			mntmLocalDocumentation.setText("Local Documentation");
		}
		
		logWindowTable = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.NO_SCROLL | SWT.V_SCROLL);
		logWindowTable.setToolTipText("Log window");
		logWindowTable.setHeaderVisible(true);
		logWindowTable.setLinesVisible(true);
		logWindowTable.setBounds(170, 542, 592, 173);
		
		TableColumn tblclmnMessage = new TableColumn(logWindowTable, SWT.NONE);
		tblclmnMessage.setResizable(false);
		tblclmnMessage.setWidth(588);
		tblclmnMessage.setText("Log window");
		
		Button btnClearLog = new Button(shell, SWT.NONE);
		btnClearLog.setToolTipText("Click here to clear the log window below");
		btnClearLog.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		btnClearLog.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				logWindowTable.clearAll();
				logWindowTable.setItemCount(0);
			}
		});
		btnClearLog.setBounds(68, 687, 94, 28);
		btnClearLog.setText("Clear log");
		
		Group grpOptions = new Group(shell, SWT.NONE);
		grpOptions.setFont(SWTResourceManager.getFont("Arial", 13, SWT.NORMAL));
		grpOptions.setText("Options:");
		grpOptions.setBounds(170, 324, 592, 205);
		if (! isMac)
		{
			grpOptions.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		}
		
		Group grpFileNaming = new Group(grpOptions, SWT.NONE);
		grpFileNaming.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		grpFileNaming.setToolTipText("Choose how audio files should be named:\r\neither by the descriptions/names of the individual events\r\n(audio parts / cycle markers) in the track file (set in Cubase)\r\nor by a fixed name followed by an index number (from 1 to number of regions)");
		grpFileNaming.setText("File naming:");
		grpFileNaming.setBounds(20, 10, 330, 90);
		if (! isMac)
		{
			grpFileNaming.setBounds(20, 25, 330, 90);
			grpFileNaming.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		}
		
		Button btnUseCubaseNamesdescriptions = new Button(grpFileNaming, SWT.RADIO);
		btnUseCubaseNamesdescriptions.setToolTipText("Choose this option if the audio files should be named by the descriptions/names of the individual events (audio parts / cycle markers) in the track file (set in Cubase)");
		btnUseCubaseNamesdescriptions.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				txtFixedName.setEnabled(false);
				setUseCubaseNames();
			}
		});
		btnUseCubaseNamesdescriptions.setSelection(true);
		btnUseCubaseNamesdescriptions.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		btnUseCubaseNamesdescriptions.setBounds(10, 10, 200, 18);
		btnUseCubaseNamesdescriptions.setText("Use Cubase names/descriptions");
		if (! isMac)
		{
			btnUseCubaseNamesdescriptions.setBounds(10, 26, 200, 18);
			btnUseCubaseNamesdescriptions.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		}
		
		Button btnUseFixedName = new Button(grpFileNaming, SWT.RADIO);
		btnUseFixedName.setToolTipText("Choose this option if the audio files should be named by a fixed string followed by an index number (from 1 to number of regions)");
		btnUseFixedName.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				txtFixedName.setEnabled(true);
				setUseFixedName(txtFixedName.getText());
			}
		});
		btnUseFixedName.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		btnUseFixedName.setBounds(10, 34, 115, 18);
		btnUseFixedName.setText("Use fixed name:");
		if (! isMac)
		{
			btnUseFixedName.setBounds(10, 50, 115, 18);
			btnUseFixedName.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		}
		
		txtFixedName = new Text(grpFileNaming, SWT.BORDER);
		txtFixedName.setToolTipText("Write a fixed name of your choice");
		txtFixedName.addModifyListener(new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				setUseFixedName(txtFixedName.getText());
			}
		});
		txtFixedName.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN) 
				{
					shell.forceFocus();
				}
			}
		});
		txtFixedName.setEnabled(false);
		txtFixedName.setText("region");
		txtFixedName.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		txtFixedName.setBounds(117, 34, 189, 19);
		if (! isMac)
		{
			txtFixedName.setBounds(130, 50, 170, 19);
			txtFixedName.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		}
		
		Group grpTrailingTime = new Group(grpOptions, SWT.NONE);
		grpTrailingTime.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		grpTrailingTime.setText("Trailing time:");
		grpTrailingTime.setBounds(367, 10, 201, 90);
		grpTrailingTime.setToolTipText("The selected number of milliseconds is added to the end of each region.\r\nThis can be useful if, for example, you have added a reverb or delay effect to a track.");
		if (! isMac)
		{
			grpTrailingTime.setBounds(367, 25, 201, 90);
			grpTrailingTime.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		}
		
		Spinner trailingTimeSpinner = new Spinner(grpTrailingTime, SWT.BORDER);
		trailingTimeSpinner.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		trailingTimeSpinner.setToolTipText(grpTrailingTime.getToolTipText());
		trailingTimeSpinner.setMaximum(Integer.MAX_VALUE);
		trailingTimeSpinner.addModifyListener(new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				engine.setTrailingTime((double)trailingTimeSpinner.getSelection()/(double)1000);
			}
		});
		trailingTimeSpinner.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN) 
				{
					shell.forceFocus();
				}
			}
		});
		trailingTimeSpinner.setBounds(10, 20, 91, 22);
		if (! isMac)
		{
			trailingTimeSpinner.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
			trailingTimeSpinner.setBounds(10, 36, 91, 22);
		}
		
		Label lblMilliseconds = new Label(grpTrailingTime, SWT.NONE);
		lblMilliseconds.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		lblMilliseconds.setBounds(108, 26, 79, 14);
		lblMilliseconds.setText("Milliseconds");
		if (! isMac)
		{
			lblMilliseconds.setBounds(108, 42, 79, 14);
			lblMilliseconds.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		}
		
		Group grpConversion = new Group(grpOptions, SWT.NONE);
		grpConversion.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		grpConversion.setToolTipText("Settings for optional conversion to mp3");
		grpConversion.setText("Conversion:");
		grpConversion.setBounds(20, 105, 548, 67);
		if (! isMac)
		{
			grpConversion.setBounds(20, 125, 548, 67);
			grpConversion.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		}
		
	
		
		comboBitrateConstant = new Combo(grpConversion, SWT.READ_ONLY);
		comboBitrateConstant.setItems(new String[] {"8 kbit/s", "16  kbit/s", "24  kbit/s", "32 kbit/s", "40  kbit/s", "48 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "320 kbit/s"});
		comboBitrateConstant.setBounds(300, 11, 110, 22);
		comboBitrateConstant.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		comboBitrateConstant.setToolTipText("Choose constant bitrate for mp3 conversion");
		comboBitrateConstant.select(12);
		if (! isMac)
		{
			comboBitrateConstant.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
			comboBitrateConstant.setBounds(300, 26, 110, 22);
		}
		comboBitrateConstant.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				sendMp3ConversionArgToEngine();
			}
		});
		
		
		comboBitrateVariable = new Combo(grpConversion, SWT.READ_ONLY);
		comboBitrateVariable.setItems(new String[] {"65 kbit/s (avg)", "85 kbit/s (avg)", "100 kbit/s (avg)", "115 kbit/s (avg)", "130 kbit/s (avg)", "165 kbit/s (avg)", "175 kbit/s (avg)", "190 kbit/s (avg)", "225 kbit/s (avg)", "245 kbit/s (avg)"});
		comboBitrateVariable.setBounds(300, 11, 120, 22);
		comboBitrateVariable.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		comboBitrateVariable.setToolTipText("Choose average bitrate (variable) for mp3 conversion");
		comboBitrateVariable.select(7);
		if (! isMac)
		{
			comboBitrateVariable.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
			comboBitrateVariable.setBounds(300, 26, 120, 22);
		}
		comboBitrateVariable.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				sendMp3ConversionArgToEngine();
			}
		});
		
		
		comboConstantVariableBitrate = new Combo(grpConversion, SWT.READ_ONLY);
		comboConstantVariableBitrate.setItems(new String[] {"Constant bit rate", "Variable bit rate"});
		comboConstantVariableBitrate.setBounds(140, 11, 120, 22);
		comboConstantVariableBitrate.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		comboConstantVariableBitrate.setToolTipText("Choose between constant or variable bitrate for mp3 conversion");
		comboConstantVariableBitrate.select(0);
		if (! isMac)
		{
			comboConstantVariableBitrate.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
			comboConstantVariableBitrate.setBounds(140, 26, 120, 22);
		}
		comboConstantVariableBitrate.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				int selected = comboConstantVariableBitrate.getSelectionIndex();
				if (selected == 0)
				{
					comboBitrateVariable.setVisible(false);
					comboBitrateConstant.setVisible(true);
				}
				else
				{
					comboBitrateVariable.setVisible(true);
					comboBitrateConstant.setVisible(false);
				}
				sendMp3ConversionArgToEngine();
			}
		});
		
	
		
		
		//Set mp3 options to disabled on start
		comboBitrateVariable.setEnabled(false);
		comboBitrateConstant.setEnabled(false);
		comboConstantVariableBitrate.setEnabled(false);
		
		//Set variable bitrate selector to hidden on start
		comboBitrateVariable.setVisible(false);
		
		Button btnCheckConvertToMp3 = new Button(grpConversion, SWT.CHECK);
		btnCheckConvertToMp3.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		btnCheckConvertToMp3.setBounds(10, 10, 110, 18);
		btnCheckConvertToMp3.setText("Convert to mp3");
		btnCheckConvertToMp3.setToolTipText("Choose whether the output files should be converted to mp3 after extraction");
		grpConversion.setTabList(new Control[]{btnCheckConvertToMp3, comboConstantVariableBitrate, comboBitrateVariable, comboBitrateConstant});
		if (! isMac)
		{
			btnCheckConvertToMp3.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
			btnCheckConvertToMp3.setBounds(10, 26, 110, 18);
		}
		btnCheckConvertToMp3.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				boolean checked = btnCheckConvertToMp3.getSelection();
				comboBitrateVariable.setEnabled(checked);
				comboBitrateConstant.setEnabled(checked);
				comboConstantVariableBitrate.setEnabled(checked);
				sendMp3ConversionArgToEngine();
				engine.setConvertToMp3(checked);
			}
		});
		btnCheckConvertToMp3.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN) 
				{
					btnCheckConvertToMp3.setSelection(!btnCheckConvertToMp3.getSelection());
					boolean checked = btnCheckConvertToMp3.getSelection();
					comboBitrateVariable.setEnabled(checked);
					comboBitrateConstant.setEnabled(checked);
					comboConstantVariableBitrate.setEnabled(checked);
					sendMp3ConversionArgToEngine();
					engine.setConvertToMp3(checked);
				}
			}
		});
		
		
		
		waveformLabel = new Label(shell, SWT.NONE);
		waveformLabel.setToolTipText("Waveform and region preview. \r\nNote that the visual waveform and region markers are not totally accurate\r\n- they are only approximations.\r\nSo don't worry if the waveform looks strange or if the regions don't quite match.\r\nThe actual export will be precise and, where possible, lossless.");
		waveformLabel.addPaintListener(new PaintListener() 
		{
			public void paintControl(PaintEvent e) 
			{
				GC gc = e.gc;
				if ((currentWaveformPng != null) && (!currentWaveformPng.isEmpty()))
				{
					Image image = new Image(display,currentWaveformPng);
					gc.drawImage(image, 0,0);
					image.dispose();
				}
				if ((generatingWaveformText != null) &&  (!generatingWaveformText.isEmpty()))
				{
					gc.setFont(SWTResourceManager.getFont("Arial", 14, SWT.NORMAL));
					int textWidth = gc.stringExtent(generatingWaveformText).x;
					int textHeight = gc.stringExtent(generatingWaveformText).y;
					int xPos = (waveformLabel.getBounds().width / 2) - (textWidth / 2);
					int yPos = (waveformLabel.getBounds().height / 2) - (textHeight / 2);
					gc.drawText(generatingWaveformText, xPos, yPos, true);
				}
				if (rangeMarkers != null)
				{
					Color prevBackgroundColor = gc.getBackground();
					int prevGCAplha = gc.getAlpha();
					gc.setBackground(rangeMarkerColor);
					gc.setAlpha(rangeMarkerAlpha);
					for (int i = 0; i < rangeMarkers.length; i++)
					{
						gc.fillRectangle(rangeMarkers[i]);
					}
					gc.setBackground(prevBackgroundColor);
					gc.setAlpha(prevGCAplha);
				}
				gc.dispose();
			}
		});
		waveformLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		waveformLabel.setBounds(172, 176, 590, 71);
		
		shell.setImage(SWTResourceManager.getImage(MultiRegionExporterForCubase.class, "/Assets/Logo256.png"));
		shell.setTabList(new Control[]{btnLoadAudioFile, btnLoadTrackFile, btnOutputFiles, grpOptions, logWindowTable, btnClearLog, grpAudioFile, grpTrackFile});
		
	}
}
