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
	//SWT variables
	//Note that some SWT variables (buttons etc.) need to be accessible in the whole class while others are only defined in the createContents function.
	protected Shell shell;
	protected Display display;
	private Device device = Display.getCurrent ();
	private Button btnLoadAudioFile;
	private Button btnLoadTrackFile;
	private Button btnOutputFiles;
	private Table table_1;
	private CLabel lblAudioFileName;
	private CLabel lblAudioFileInfo;
	private CLabel lblTrackFileName;
	private Label waveformLabel;
	private Label lblgeneratingWaveform;
	private Spinner spinner;
	
	
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
	private double[][] currentWaveformData;
	
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
	 * Path to the SoX executable
	 */
	private String pathToSox;
	
	/**
	 * Path to the SoXi executable
	 */
	private String pathToSoxi;
	
	/**
	 * Path to the local documentation file
	 */
	private String pathToLocalDocumentation;
	
	/**
	 * Above path represented as File
	 */
	private File localDocumentationFile;
	
	/**
	 * File name of the temporary, downsampled wave file to be created in the waveform generation process
	 */
	private String pathToWaveform;
	
	/**
	 * Are we running on a Mac?
	 * Note that SWT behaves a little bit different on Windows and Mac.
	 * Therefore we need to do some conditional layout etc.
	 */
	public static boolean isMac;
	
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
			lblgeneratingWaveform.setText(dotString + "generating waveform" + dotString);
			i = (i + 1) % 4;
	        display.timerExec(generatingWaveformTextTimerTime, this);
		}
	};
	
	
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
	
	/**
	 * Open the window.
	 */
	public void open() 
	{
		
		engine = new ExporterEngine(this);
		display = Display.getDefault();
		isMac = SWT.getPlatform().equals("cocoa");
		
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
		engine.stopStartedProcesses();
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() 
	{
		
		shell = new Shell();
		shell.setSize(800, 800);
		shell.setText("Multi-region Exporter - for Cubase");
		
		
		lblgeneratingWaveform = new Label(shell, SWT.NONE);
		lblgeneratingWaveform.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		lblgeneratingWaveform.setFont(SWTResourceManager.getFont("Arial", 14, SWT.NORMAL));
		lblgeneratingWaveform.setAlignment(SWT.CENTER);
		lblgeneratingWaveform.setText("...generating waveform...");
		lblgeneratingWaveform.setBounds(172, 194, 590, 36);
		
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
		lblVersion.setText("version 1.0.0.0");
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
		//Listen for Enter on widgets on mac, since it doesn't work automatically here
		btnLoadAudioFile.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					loadAudioFile();
				}
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
		//Listen for Enter on widgets on mac, since it doesn't work automatically here
		btnLoadTrackFile.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					loadTrackFile();
				}
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
		btnOutputFiles.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					outputFilesToFolder();
				}
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
		
		table_1 = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.NO_SCROLL | SWT.V_SCROLL);
		table_1.setToolTipText("Log window");
		table_1.setHeaderVisible(true);
		table_1.setLinesVisible(true);
		table_1.setBounds(170, 542, 592, 173);
		
		TableColumn tblclmnMessage = new TableColumn(table_1, SWT.NONE);
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
				table_1.clearAll();
				table_1.setItemCount(0);
			}
		});
		btnClearLog.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					table_1.clearAll();
					table_1.setItemCount(0);
				}
			}
		});
		btnClearLog.setBounds(665, 508, 94, 28);
		btnClearLog.setText("Clear log");
		
		Group grpOptions = new Group(shell, SWT.NONE);
		grpOptions.setFont(SWTResourceManager.getFont("Arial", 13, SWT.NORMAL));
		grpOptions.setText("Options:");
		grpOptions.setBounds(170, 341, 592, 151);
		if (! isMac)
		{
			grpOptions.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		}
		
		Group grpFileNaming = new Group(grpOptions, SWT.NONE);
		grpFileNaming.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		grpFileNaming.setToolTipText("Choose how audio files should be named:\r\neither by the descriptions/names of the individual events\r\n(audio parts / cycle markers) in the track file (set in Cubase)\r\nor by a fixed name followed by an index number (from 1 to number of regions)");
		grpFileNaming.setText("File naming:");
		grpFileNaming.setBounds(20, 20, 330, 90);
		if (! isMac)
		{
			grpFileNaming.setBounds(20, 35, 330, 90);
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
		btnUseCubaseNamesdescriptions.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					btnUseCubaseNamesdescriptions.setSelection(true);
					txtFixedName.setEnabled(false);
					setUseCubaseNames();
				}
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
		btnUseFixedName.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					btnUseFixedName.setSelection(true);
					txtFixedName.setEnabled(true);
					setUseFixedName(txtFixedName.getText());
				}
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
				if (e.detail == SWT.TRAVERSE_RETURN && isMac) 
				{
					spinner.forceFocus();
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
		grpTrailingTime.setToolTipText("");
		grpTrailingTime.setText("Trailing time");
		grpTrailingTime.setBounds(367, 20, 201, 90);
		grpTrailingTime.setToolTipText("The selected number of milliseconds is added to the end of each region.\r\nThis can be useful if, for example, you have added a reverb or delay effect to a track.");
		if (! isMac)
		{
			grpTrailingTime.setBounds(367, 35, 201, 90);
			grpTrailingTime.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		}
		
		spinner = new Spinner(grpTrailingTime, SWT.BORDER);
		spinner.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		spinner.setToolTipText(grpTrailingTime.getToolTipText());
		spinner.setMaximum(Integer.MAX_VALUE);
		spinner.addModifyListener(new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				engine.setTrailingTime((double)spinner.getSelection()/(double)1000);
			}
		});
		spinner.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN) 
				{
					if (btnOutputFiles.getEnabled())
					{
						btnOutputFiles.forceFocus();
					}
					else
					{
						shell.forceFocus();
					}
				}
			}
		});
		spinner.setBounds(10, 20, 91, 22);
		if (! isMac)
		{
			spinner.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
			spinner.setBounds(10, 36, 91, 22);
		}
		
		Label lblMilliseconds = new Label(grpTrailingTime, SWT.NONE);
		lblMilliseconds.setFont(SWTResourceManager.getFont("Arial", 11, SWT.NORMAL));
		lblMilliseconds.setBounds(108, 26, 79, 14);
		lblMilliseconds.setText("milliseconds");
		if (! isMac)
		{
			lblMilliseconds.setBounds(108, 42, 79, 14);
			lblMilliseconds.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		}
		
		waveformLabel = new Label(shell, SWT.NONE);
		waveformLabel.setToolTipText("Waveform and region preview. \r\nNote that the visual waveform and region markers are not 100% accurate\r\n- they are only approximations. \r\nThe actual export will be precise and, where possible, lossless.");
		waveformLabel.addPaintListener(new PaintListener() 
		{
			public void paintControl(PaintEvent e) 
			{
				
				if (currentWaveformData != null)
				{
					GC gc = e.gc;
					
					int halfHeight = (int)(waveformLabel.getBounds().height * 0.5);
					for (int i = 0; i < currentWaveformData.length; i++)
					{
						gc.drawLine(i, (int)(currentWaveformData[i][0] * halfHeight + halfHeight), i, (int)(currentWaveformData[i][1]* halfHeight + halfHeight));
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
					
				}
			}
		});
		waveformLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		waveformLabel.setBounds(172, 176, 590, 71);
		
		shell.setImage(SWTResourceManager.getImage(MultiRegionExporterForCubase.class, "/Assets/Logo256.png"));
		shell.setTabList(new Control[]{btnLoadAudioFile, btnLoadTrackFile, btnOutputFiles, grpOptions, btnClearLog, grpAudioFile, grpTrackFile, table_1});
		
	}
	
	/**
	 * Do some startup setting up
	 */
	private void setup()
	{
		btnOutputFiles.setEnabled(false);
		btnLoadTrackFile.setEnabled(false);
		lblgeneratingWaveform.setVisible(false);
		setPaths();
		engine.setWaveformWidth(waveformLabel.getBounds().width);
		clearAudioFile();
		lblTrackFileName.setText("none");
		setUseCubaseNames();
		
	}
	
	/**
	 * Set up the different paths. Calculated from paths relative to jar file.
	 */
	private void setPaths()
	{
		//Calculate paths
		if (isMac)
		{
			String pathToContentsFolder = Utils.getJarPath(2);
			String fileSeparator = Utils.getFileSeparator();
			pathToSox = pathToContentsFolder + fileSeparator + "sox" + fileSeparator + "sox";
			pathToSoxi = pathToContentsFolder + fileSeparator + "sox" + fileSeparator + "soxi";
			pathToWaveform = pathToContentsFolder + fileSeparator + "waveform" + fileSeparator + "waveform.wav";
			pathToLocalDocumentation = pathToContentsFolder + fileSeparator + "documentation" + fileSeparator + "documentation.pdf";
		}
		else
		{
			String pathToMainFolder = Utils.getJarPath(1);
			String fileSeparator = Utils.getFileSeparator();
			String pathToResourcesFolder = pathToMainFolder + fileSeparator + "resources";
			
			pathToSox = pathToResourcesFolder +fileSeparator + "sox" + fileSeparator + "sox";
			pathToSoxi = pathToResourcesFolder + fileSeparator + "sox" + fileSeparator + "soxi";
			pathToWaveform = pathToResourcesFolder + fileSeparator + "waveform" + fileSeparator + "waveform.wav";
			pathToLocalDocumentation = pathToMainFolder + fileSeparator + "documentation" + fileSeparator + "documentation.pdf";
		}
		pathToOnlineDocumentation = "http://jakobhandersen.dk/projects/multi-region-exporter/documentation/";
		
		//Set paths
		engine.setSoxPath(pathToSox);
		engine.setSoxiPath(pathToSoxi);
		engine.setWaveformFileName(pathToWaveform);
		localDocumentationFile = new File(pathToLocalDocumentation);
		
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
		lblgeneratingWaveform.setVisible(false);
		display.timerExec(-1, generatingWaveformTextTimer);
		currentWaveformData = null;
		waveformLabel.redraw();
		
		//clear currentInputAudioFile
		currentInputAudioFile = null;
		
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
	 * Called when load audio file button is pressed
	 */
	private void loadAudioFile()
	{
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
        fd.setText("Load an audio file");
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
	 * Show the About dialog
	 */
	private void showAbout()
	{
		AboutDialog d = new AboutDialog(shell, SWT.APPLICATION_MODAL);
		d.open();
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
					lblgeneratingWaveform.setVisible(true);
					display.timerExec(0, generatingWaveformTextTimer);
					break;
					
				case DONE_GENERATING_WAVEFORM:
					lblgeneratingWaveform.setVisible(false);
					display.timerExec(-1, generatingWaveformTextTimer);
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
				List<String> splitStrings = getWrappedString(getStartStringForUserMessageType(type) + message,573,table_1);
				for (int i = 0; i < splitStrings.size(); i++)
				{
					TableItem tableItem = new TableItem(table_1, SWT.NONE);
					tableItem.setText(splitStrings.get(i));
					tableItem.setForeground(getColorForUserMessageType(type));
					table_1.showItem(tableItem);
				}
		    }
		});
		
	}

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
	 * Do things when outputting files
	 */
	private void startOutputtingFiles()
	{
		shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));
		btnLoadAudioFile.setEnabled(false);
		btnLoadTrackFile.setEnabled(false);
		btnOutputFiles.setEnabled(false);
	}
	
	
	/**
	 * Stop doing things when outputting files
	 */
	private void stopOutputtingFiles()
	{
		shell.setCursor(new Cursor(display, SWT.CURSOR_ARROW));
		btnLoadAudioFile.setEnabled(true);
		btnLoadTrackFile.setEnabled(true);
		btnOutputFiles.setEnabled(true);
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

	@Override
	public void waveformCreated(double[][] waveformData) 
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    	currentWaveformData = waveformData;
		    	waveformLabel.redraw();
		    }
		});
		
	}
}
