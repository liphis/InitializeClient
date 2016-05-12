package de.abas.training.init;

import java.util.ArrayList;

public class Main {

	/**
	 * Main method.
	 *
	 * @param args
	 *            Runtime arguments. Have to contain training type, server name
	 *            and all client names in that order.
	 */
	public static void main(String[] args) {
		final Main main = new Main();
		final String[] clients = main.getClients(args);
		final Init init = new Init(args[0], args[1], clients);
		init.init();
	}

	/**
	 * Extracts clients from args.
	 *
	 * @param args
	 *            Main calling parameters.
	 * @return All clients as String array.
	 */
	public String[] getClients(String[] args) {
		final ArrayList<String> clientList = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (i > 1) {
				clientList.add(args[i]);
			}
		}
		String[] clients = new String[clientList.size()];
		clients = clientList.toArray(clients);
		return clients;
	}

}
