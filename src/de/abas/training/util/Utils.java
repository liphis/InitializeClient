package de.abas.training.util;

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

}
