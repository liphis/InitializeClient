package de.abas.training.util;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Arrays;

import org.apache.log4j.Logger;

public class Utils {

	private static Logger logger = getLogger();

	/**
	 * Determines whether a specified port is available or not.
	 *
	 * @param port
	 *            Port to check availability.
	 * @return True if the port is available, otherwise false.
	 */
	public static boolean available(int port) {
		try (ServerSocket ss = new ServerSocket(port); DatagramSocket ds = new DatagramSocket(port)) {
			ss.setReuseAddress(true);
			ds.setReuseAddress(true);
			return true;
		} catch (final IOException e) {
			logger.error(String.format("Port %d is already in use: %s", port, e.getMessage()));
			return false;
		}
	}

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
	 * Runs a system command.
	 *
	 * @param directory
	 *            Directory in which to execute command.
	 * @param commandAndArgs
	 *            Command to execute.
	 * @throws IOException
	 *             Thrown if an error occurs while executing the commands.
	 */
	public static void runSystemCommand(File directory, String... commandAndArgs) throws IOException {
		Utils.logger.debug("Executing commands:" + Arrays.toString(commandAndArgs) + " in dir:" + directory);
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb = pb.directory(directory);
			logger.debug(String.format("Execution directory set to %s", pb.directory().getAbsolutePath()));
			pb.command(commandAndArgs);
			final Process process = pb.start();
			if (process != null) {
				final int exitCode = process.waitFor();
				if (exitCode != 0) {
					final String message = "Command execution failed with exit code " + exitCode;
					logger.fatal(message);
					throw new RuntimeException(message);
				}
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			final String message = "Command execution interrupted";
			logger.fatal(message, e);
			throw new RuntimeException(message);
		}
	}

	/**
	 * Runs a system command.
	 *
	 * @param commandAndArgs
	 *            Command to execute.
	 * @throws IOException
	 *             Thrown if an error occurs while executing the commands.
	 */
	public static void runSystemCommand(String... commandAndArgs) throws IOException {
		Utils.logger.debug("Executing commands:" + Arrays.toString(commandAndArgs));
		try {
			final ProcessBuilder pb = new ProcessBuilder();
			pb.command(commandAndArgs);
			final Process process = pb.start();
			if (process != null) {
				final int exitCode = process.waitFor();
				if (exitCode != 0) {
					final String message = "Command execution failed with exit code " + exitCode;
					logger.fatal(message);
					throw new RuntimeException(message);
				}
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			final String message = "Command execution interrupted";
			logger.fatal(message, e);
			throw new RuntimeException(message);
		}
	}

}
