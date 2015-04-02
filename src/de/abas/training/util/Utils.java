package de.abas.training.util;

import java.io.IOException;

import org.apache.log4j.Logger;

public class Utils {

	private static Logger logger = getLogger();

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

	/**
	 * Runs a system command and returns the console output as a BufferedReader
	 * instance.
	 *
	 * @param command The command to execute.
	 * @return The console output as a BufferedReader instance.
	 * @throws InterruptedException
	 */
	public static void runSystemCommand(String... commandAndArgs) throws IOException {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(commandAndArgs);
			Process process = pb.start();
			if (process != null) {
				int exitCode = process.waitFor();
				if (exitCode != 0) {
					String message =
							"Command execution failed with exit code " + exitCode;
					logger.fatal(message);
					throw new RuntimeException(message);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			String message = "Command execution interrupted";
			logger.fatal(message, e);
			throw new RuntimeException(message);
		}
	}

}
