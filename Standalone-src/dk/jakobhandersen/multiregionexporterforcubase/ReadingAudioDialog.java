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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * SWT dialog Showing that the application is busy reading an audio input file.
 * Not currently in use.
 * @author Jakob Hougaard Andersen
 *
 */
public class ReadingAudioDialog extends Dialog 
{

	protected Object result;
	protected Shell shell;
	private Display display;
	private Label lblExpor;
	private final int textTimerTime = 250;
	private Runnable textTimer = new Runnable() 
	{
		int i = 0;
		public void run() 
		{
			String dotString = "";
			for (int j = 0; j < i; j++)
			{
				dotString += ".";
			}
			lblExpor.setText(dotString + "reading audio file" + dotString);
			i = (i + 1) % 4;
	        display.timerExec(textTimerTime, this);
		}
	};

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ReadingAudioDialog(Shell parent, int style) 
	{
		super(parent, style);
		setText("");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() 
	{
		display = getParent().getDisplay();
		createContents();
		shell.open();
		shell.layout();
		display.timerExec(0, textTimer);
		
		while (!shell.isDisposed()) 
		{
			if (!display.readAndDispatch()) 
			{
				display.sleep();
			}
		}
		return result;
	}
	
	public void close()
	{
		display.timerExec(-1, textTimer);
		shell.close();
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() 
	{
		shell = new Shell(getParent(), getStyle());
		int width = 450;
		int height = 200;
		shell.setSize(width, height);
		Rectangle b = getParent().getBounds();
		int xPos = (b.width / 2) - (width / 2) + b.x;
		int yPos = (b.height / 2) - (height / 2) + b.y;
		shell.setLocation(xPos,yPos);
		shell.setText(getText());
		
		lblExpor = new Label(shell, SWT.NONE);
		lblExpor.setFont(SWTResourceManager.getFont("Arial", 18, SWT.NORMAL));
		lblExpor.setAlignment(SWT.CENTER);
		lblExpor.setBounds(0, 66, 450, 35);
		lblExpor.setText("outputting files");
		shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));

	}
}
