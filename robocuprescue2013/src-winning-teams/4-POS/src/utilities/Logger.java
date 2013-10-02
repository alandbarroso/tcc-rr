package utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Logger {
	private PrintWriter writer = null;
	private Timer timer = null;

	public Logger(String fileAddress, Timer timer) {
		this.timer = timer;
		try {
			if (fileAddress != null)
				writer = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(new File(
								fileAddress)))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public PrintWriter getWriter() {
		return writer;
	}

	public void logTime(int time) {
		if (writer != null) {
			logTimeTag();
			writer.println("Time: " + time);
			writer.flush();
		}
	}

	public void logEndOfCycle() {
		if (writer != null) {
			logTimeTag();
			writer.println("------------End Of Cycle------------");
			writer.println();
			writer.flush();
		}
	}

	public void log(String msg) {
		if (writer != null) {
			logTimeTag();
			writer.println("\t" + msg);
			writer.flush();
		}
	}

	public void logTimeTag() {
		if (writer == null)
			return;
		String outTime = "????";
		if (timer != null)
			outTime = String.format("%1$04d", timer.getElapsedTime());

		String logStr = "  " + outTime + ". ";
		writer.write(logStr);
	}

	protected void finalize() throws Throwable {
		if (writer != null)
			writer.close();
	}
}
