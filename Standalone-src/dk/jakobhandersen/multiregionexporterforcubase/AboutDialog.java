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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * SWT dialog - the About page showing info about this version etc.
 * @author Jakob Hougaard Andersen
 */
public class AboutDialog extends Dialog 
{

	protected Object result;
	protected Shell shlAboutMultiregionExporter;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public AboutDialog(Shell parent, int style) 
	{
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() 
	{
		createContents();
		shlAboutMultiregionExporter.open();
		shlAboutMultiregionExporter.layout();
		Display display = getParent().getDisplay();
		while (!shlAboutMultiregionExporter.isDisposed()) 
		{
			if (!display.readAndDispatch()) 
			{
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() 
	{
		shlAboutMultiregionExporter = new Shell(getParent(), getStyle());
		int width = 520;
		int height = 600;
		shlAboutMultiregionExporter.setSize(width, height);
		Rectangle b = getParent().getBounds();
		int xPos = (b.width / 2) - (width / 2) + b.x;
		int yPos = (b.height / 2) - (height / 2) + b.y;
		shlAboutMultiregionExporter.setLocation(xPos,yPos);
		shlAboutMultiregionExporter.setText("About Multi-region Exporter - for Cubase");
		
		Button btnOk = new Button(shlAboutMultiregionExporter, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() 
		{
			@Override
			public void widgetSelected(SelectionEvent e) 
			{
				shlAboutMultiregionExporter.close();
			}
		});
		//Listen for Enter on widgets on mac, since it doesn't work automatically here
		btnOk.addTraverseListener(new TraverseListener() 
		{
			public void keyTraversed(TraverseEvent e) 
			{
				if (e.detail == SWT.TRAVERSE_RETURN && MultiRegionExporterForCubase.isMac) 
				{
					shlAboutMultiregionExporter.close();
				}
			}
		});
		btnOk.setBounds(373, 529, 108, 32);
		btnOk.setText("OK");
		
		Label lblNewLabel = new Label(shlAboutMultiregionExporter, SWT.WRAP);
		lblNewLabel.setFont(SWTResourceManager.getFont("Arial", 10, SWT.NORMAL));
		if (! MultiRegionExporterForCubase.isMac)
		{
			lblNewLabel.setFont(SWTResourceManager.getFont("Arial", 8, SWT.NORMAL));
		}
		lblNewLabel.setBounds(111, 44, 370, 479);
		String osString;
		if (MultiRegionExporterForCubase.isMac)
		{
			osString = "OS X";
		}
		else
		{
			osString = "Windows";
		}
		lblNewLabel.setText("Version "+Constants.versionString+" for "+osString+"\r\nBuilt on "+Constants.buildDateString+"\r\nCopyright (C) 2017 Jakob Hougaard Andsersen\r\n\r\nThe Multi-region Exporter is licensed under the GNU General Public License (GPL) version 3 (http://www.gnu.org/licenses/gpl-3.0.html).\r\n\r\nThe audio splitting is done by SoX - Sound eXchange (http://sox.sourceforge.net/) which is distributed in binary form with The Multi-region Exporter - for Cubase. SoX itself is licenced under the GNU General Public License version 2.\r\nA big thanks to SoX for creating and sharing their tool.\r\n\r\nThe generation of audio waveform and the optional conversion to mp3 is done by FFMPEG (https://www.ffmpeg.org/) which is distributed with the program in binary form. FFMPEG is itself licensed under the GNU Lesser General Public License (LGPL) version 2.1 or later (read more here: https://www.ffmpeg.org/legal.html).\r\nA big thanks to FFMPEG for creating and sharing their tool.\r\n\r\nFFMPEG in turn uses the LAME mp3 encoder for mp3 conversion which is also open source and also licenced under LGPL. Read more on the LAME website: http://lame.sourceforge.net\r\nAlso a big thanks LAME for creating and sharing their tool.\r\n\r\nThe Multi-region Exporter is also distributed with its 'own' copy of the Java Runtime Environment (JRE). The included JRE has its own licence agreement, which can be found on Oracle's JRE download website (http://www.oracle.com/technetwork/java/javase/downloads/index.html).\r\n\r\nIt must be mentioned that Cubase and Steinberg have nothing to do with the development of this tool and therefore they are in no way responsible for it, its functionality / lack of functionality or for supporting it.");
		
		Label label = new Label(shlAboutMultiregionExporter, SWT.NONE);
		label.setImage(SWTResourceManager.getImage(AboutDialog.class, "/Assets/Logo85.png"));
		label.setBounds(12, 17, 85, 91);
		
		Label lblAboutMultiregionExporter = new Label(shlAboutMultiregionExporter, SWT.NONE);
		lblAboutMultiregionExporter.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		lblAboutMultiregionExporter.setBounds(111, 20, 281, 26);
		lblAboutMultiregionExporter.setText("About Multi-region Exporter - for Cubase\n");

	}
}
