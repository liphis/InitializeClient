package de.abas.training.init;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RegexTest {

	@Test
	public void regexTest() throws Exception {
		String[] fileNames =
				{ "jfopserver.oxford.LAXDEBUGSY.dat", "jfopserver.schulung.dat",
						"jfopserver.schulung.DEBUGSY13.dat" };
		for (String fileName : fileNames) {
			boolean match = fileName.matches("jfopserver\\..+\\.dat");
			assertEquals("file name " + fileName + " matches", true, match);
		}
	}

}
