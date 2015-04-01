package de.abas.training.init;

import org.junit.Assert;
import org.junit.Test;

public class SubstringTest {

	@Test
	public void substringTest() throws Exception {
		String testString = "bla";
		String substring = testString.substring(testString.length() - 1);
		Assert.assertEquals("Substring holds only letter 'a'", "a", substring);
	}

}
