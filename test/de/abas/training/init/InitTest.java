package de.abas.training.init;

import static org.junit.Assert.assertNotEquals;

import java.io.BufferedReader;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.util.ContextHelper;
import de.abas.training.util.Utils;

public class InitTest {

	@Test
	public void clientContextCreationTest() throws Exception {
		DbContext ctx =
				ContextHelper.createClientContext("schulung", 6550, "i7erp12", "sy",
						"test");
		assertNotEquals("Database context is not null", null, ctx);
	}

	@Test
	public void contructorTest() throws Exception {
		ArrayList<String> argsList = new ArrayList<String>();
		argsList.add("basic");
		argsList.add("schulung");
		argsList.add("i7erp11");
		argsList.add("i7erp12");
		String[] args = new String[argsList.size()];
		args = argsList.toArray(args);
		String[] clients = new Main().getClients(args);
		Init init = new Init(args[0], args[1], clients);
		Assert.assertEquals("Training type: advanced is false", false, init.advanced);
		Assert.assertEquals("Server is schulung", "schulung", init.server);
		Assert.assertEquals("Two clients within clients Array", 2,
				init.clients.length);
	}

	@Test
	public void runSystemCommandTest() throws Exception {
		BufferedReader bufferedReader = Utils.runSystemCommand("cmd echo %cd%");
		assertNotEquals("BufferedReader instance is not null", null, bufferedReader);
	}

}
