package de.abas.training.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class Utils {

	public static Logger logger = Utils.getLogger();

	/**
	 * Initializes a log4j Logger instance for logging.
	 *
	 * @return A new initialized Logger instance.
	 */
	public static Logger getLogger() {
		final Throwable t = new Throwable();
		t.fillInStackTrace();
		final String fullClassName = t.getStackTrace()[1].getClassName();

		return Logger.getLogger(fullClassName);
	}

	// /**
	// * Runs a system command and returns the console output as a BufferedReader
	// * instance.
	// *
	// * @param command The command to execute.
	// * @return The console output as a BufferedReader instance.
	// * @throws IOException Thrown if the command could not be executed.
	// */
	// public static BufferedReader runSystemCommand(String command) throws
	// IOException {
	// final SystemCommand systemCommand = new SystemCommand(command, false);
	// if (systemCommand.runHidden()) {
	// BufferedReader bufferedReader =
	// new BufferedReader(systemCommand.getOut());
	// if (systemCommand.getOut() != null) {
	// String line = null;
	// while ((line = bufferedReader.readLine()) != null) {
	// logger.debug(String.format("Command output: %s", line));
	// }
	// }
	// else {
	// logger.debug("No command output");
	// }
	// return bufferedReader;
	// }
	//
	// String message = "Running system command " + command + " failed.";
	// logger.fatal(message);
	// throw new IOException(message);
	// }

	/**
	 * Runs a system command and returns the console output as a BufferedReader
	 * instance.
	 *
	 * @param command The command to execute.
	 * @return The console output as a BufferedReader instance.
	 */
	public static BufferedReader runSystemCommand(String command) {
		BufferedReader bufferedReader = null;
		try {
			Process process = Runtime.getRuntime().exec(command);
			if (process != null) {
				if (process.getErrorStream() == null) {
					bufferedReader =
							new BufferedReader(new InputStreamReader(
									process.getInputStream()));
					if (process.getInputStream() != null) {
						String line = null;
						while ((line = bufferedReader.readLine()) != null) {
							logger.debug(String.format("Command output: %s", line));
						}
					}
					else {
						logger.debug("No command output");
					}
				}
			}
			return null;
		}
		catch (IOException e) {
			logger.fatal(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				}
				catch (IOException e) {
					logger.fatal(e.getMessage(), e);
					throw new RuntimeException(e);
				}
			}
		}

	}

}
