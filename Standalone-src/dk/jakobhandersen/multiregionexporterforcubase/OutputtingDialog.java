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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.ProgressBar;

/**
 * SWT dialog - Showing that the application is busy outputting the files.
 * @author Jakob Hougaard Andersen
 *
 */
public class OutputtingDialog extends Dialog 
{

	protected Object result;
	protected Shell shell;
	private Display display;
	private Label lblHeader;
	private ProgressBar progressBar;
	private Label lblProcessText;


	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public OutputtingDialog(Shell parent, int style) 
	{
		super(parent, style);
		setText("");
	}

	/**
	 * Close the dialog
	 */
	public void close()
	{
		//Delay actual closing by 500ms in order to display full progress bar etc.
		display.timerExec(500,
				new Runnable()
				{
					public void run()
					{
						shell.close();
					}
				})
		;
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() 
	{
		display = getParent().getDisplay();
		createContents();
		
		progressBar.setSelection(0);
		lblProcessText.setText("");
		
		shell.open();
		shell.layout();
		
		while (!shell.isDisposed()) 
		{
			if (!display.readAndDispatch()) 
			{
				display.sleep();
			}
		}
		return result;
	}
	
	/**
	 * Set the text describing what the outputting process is doing right now
	 * @param text shown in label below progress bar
	 */
	public void setProcessText(String text)
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    		lblProcessText.setText(text);
		    }
		});
		
	}
	
	
	/**
	 * Set the progress percentage of progress bar
	 * @param percentage (0 to 100)
	 */
	public void setProgressPercentage(int percentage)
	{
		Display.getDefault().syncExec(new Runnable() 
		{
		    public void run() 
		    {
		    		progressBar.setSelection(percentage);
		    }
		});
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
		
		lblHeader = new Label(shell, SWT.NONE);
		lblHeader.setFont(SWTResourceManager.getFont("Arial", 18, SWT.NORMAL));
		lblHeader.setAlignment(SWT.CENTER);
		lblHeader.setBounds(0, 30, 450, 35);
		lblHeader.setText("Outputting files");
		
		
		progressBar = new ProgressBar(shell, SWT.NONE);
		progressBar.setBounds(33, 88, 377, 14);
		
		
		
		lblProcessText = new Label(shell, SWT.NONE);
		lblProcessText.setAlignment(SWT.CENTER);
		lblProcessText.setFont(SWTResourceManager.getFont("Arial", 12, SWT.NORMAL));
		lblProcessText.setBounds(33, 115, 377, 25);
		
		shell.setCursor(new Cursor(display, SWT.CURSOR_WAIT));
	}
}
