package example.swt.osx;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Application {

	public static final String APP_NAME = "MyApp";

	public static void main(String[] args) {
		Display.setAppName(APP_NAME);
		Display display = Display.getDefault();
	
		if (SWT.getPlatform().equals("cocoa")) {
			new CocoaUIEnhancer().earlyStartup();
		}

		Shell shell = new Shell(display);
		shell.setText(APP_NAME);
		shell.setSize(300, 300);
		shell.open();


		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}

}
