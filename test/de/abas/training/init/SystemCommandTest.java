package de.abas.training.init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.abas.eks.jfop.FOPException;
import de.abas.eks.jfop.remote.ContextRunnable;
import de.abas.eks.jfop.remote.FOPSessionContext;
import de.abas.erp.db.DbContext;

public class SystemCommandTest implements ContextRunnable {

	@Override
	public int runFop(FOPSessionContext arg0, String[] arg1) throws FOPException {

		DbContext ctx = arg0.getDbContext();

		try {
			Runtime rt = Runtime.getRuntime();
			String[] commands = { "pwd" };
			Process proc = rt.exec(commands);

			BufferedReader stdInput =
					new BufferedReader(new InputStreamReader(proc.getInputStream()));

			BufferedReader stdError =
					new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			// read the output from the command
			ctx.out().println("Here is the standard output of the command:");
			String s = null;
			while ((s = stdInput.readLine()) != null) {
				ctx.out().println(s);
			}

			// read any errors from the attempted command
			ctx.out().println("Here is the standard error of the command (if any):");
			while ((s = stdError.readLine()) != null) {
				ctx.out().println(s);
			}

			ctx.out().println("end");

			return 0;
		}
		catch (IOException e) {
			ctx.out().println(e.getMessage());
			return 1;
		}
	}

}
