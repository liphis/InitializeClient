package de.abas.training.init;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import de.abas.erp.db.DbContext;
import de.abas.erp.db.EditorAction;
import de.abas.erp.db.Query;
import de.abas.erp.db.exception.CommandException;
import de.abas.erp.db.schema.company.Password;
import de.abas.erp.db.schema.company.PasswordEditor;
import de.abas.erp.db.schema.infosystem.Infosystem;
import de.abas.erp.db.schema.infosystem.InfosystemEditor;
import de.abas.erp.db.schema.infosystem.InfosystemEditor.Row;
import de.abas.erp.db.schema.infrastructure.JFOPServer;
import de.abas.erp.db.schema.infrastructure.JFOPServerEditor;
import de.abas.erp.db.selection.Conditions;
import de.abas.erp.db.selection.SelectionBuilder;
import de.abas.erp.db.util.ContextHelper;
import de.abas.erp.db.util.QueryUtil;
import de.abas.training.util.Utils;

public class Init {

	String server;
	String[] clients;
	boolean advanced;

	Logger logger = Utils.getLogger();

	public Init(String trainingType, String server, String... clients) {
		switch (trainingType) {
			case "basic":
				advanced = false;
				break;
			case "advanced":
				advanced = true;
				break;
			default:
				throw new RuntimeException("Training type not valid");
		}
		this.server = server;
		this.clients = clients;
	}

	/**
	 * Initializes all clients.
	 */
	public void init() {
		logger.debug("initializing clients");
		try {
			initJavaProjects();
			initMandantClasspath();
			initFopTxt();
			initBashrc();
			initVimrc();
			initGitPrompt();
			initInfosystems();
			initJfopServerInstances();
		}
		catch (CommandException e) {
			System.out.println("An error occurred: " + e.getMessage());
			logger.fatal(e.getMessage(), e);
			return;
		}
	}

	/**
	 * Inserts or replaces .bashrc.
	 */
	public void initBashrc() {
		for (String client : clients) {
			logger.debug(String.format("initializing .bashrc for client %s", client));
			Utils.runSystemCommand("scp files/.bashrc " + client + "@" + server
					+ ":~");
		}
	}

	/**
	 * Replaces fop.txt with default fop.txt.
	 */
	public void initFopTxt() {
		for (String client : clients) {
			logger.debug(String.format("initializing fop.txt for client %s", client));
			Utils.runSystemCommand("scp files/fop.txt " + client + "@" + server
					+ ":~");
		}

	}

	/**
	 * Inserts or replaces git-prompt.sh
	 */
	public void initGitPrompt() {
		for (String client : clients) {
			logger.debug(String.format("initializing git-prompt.sh for client %s",
					client));
			Utils.runSystemCommand("scp files/git-prompt.sh " + client + "@"
					+ server + ":~");
		}
	}

	/**
	 * Initializes all infosystems used during AJO trainings.
	 *
	 * @throws CommandException Thrown if infosystem could not be opened for editing.
	 */
	public void initInfosystems() throws CommandException {
		for (String client : clients) {
			logger.debug(String.format("initializing infosystems for client %s",
					client));
			DbContext ctx =
					ContextHelper.createClientContext(server, 6550, client, "sy",
							"Initializing client");
			ArrayList<String> infosystemNames = getInfosystems();
			for (String infosystemName : infosystemNames) {
				logger.debug(String.format(
						"initializing infosystem %s for client %s", infosystemName,
						client));
				Infosystem infosystem = selectInfosystem(ctx, infosystemName);
				InfosystemEditor infosytemEditor = infosystem.createEditor();
				infosytemEditor.open(EditorAction.UPDATE);
				initHeaderFields(infosytemEditor);
				Iterable<Row> editableRows =
						infosytemEditor.table().getEditableRows();
				for (Row row : editableRows) {
					initRowFields(row);
				}
				infosytemEditor.commit();
			}
			ctx.close();
		}
	}

	/**
	 * Removes all projects from $MANDANTDIR/java/projects.
	 */
	public void initJavaProjects() {
		for (String client : clients) {
			logger.debug(String.format(
					"initializing java/projects folder for client %s", client));
			Utils.runSystemCommand("su "
					+ client
					+ " && cd ~ && eval$(sh denv.sh) && cd ~/java/projects && rm -rf *");
		}
	}

	/**
	 * Initializes the JFOP Server instances according to the training type.
	 *
	 * @throws CommandException Thrown if password could not be opened for editing.
	 */
	public void initJfopServerInstances() throws CommandException {
		for (String client : clients) {
			logger.debug(String.format(
					"initializing JFOP Server instances for client %s", client));
			DbContext ctx =
					ContextHelper.createClientContext(server, 6550, client, "sy",
							"Initialization for training");
			deleteAllJfopServerInstances(ctx);
			Password password = selectPassword(ctx);
			PasswordEditor passwordEditor = password.createEditor();
			passwordEditor.open(EditorAction.UPDATE);
			JFOPServer jfopServerInit = null;
			passwordEditor.setJfopServer(jfopServerInit);
			passwordEditor.commit();
			logger.debug(String
					.format("JFOP Server field in password definition of client %s successfully initialized",
							client));
			if (advanced) {
				advancedTrainingSetup(client, ctx, password);
			}
			ctx.close();
		}

	}

	/**
	 * Replaces mandant.classpath with default mandant.classpath.
	 */
	public void initMandantClasspath() {
		for (String client : clients) {
			logger.debug(String.format(
					"initializing mandant.classpath for client %s", client));
			Utils.runSystemCommand("scp files/mandant.classpath " + client + "@"
					+ server + ":~/java/");
		}

	}

	/**
	 * Inserts or replaces .vimrc.
	 */
	public void initVimrc() {
		for (String client : clients) {
			logger.debug(String.format("initializing .vimrc for client %s", client));
			Utils.runSystemCommand("scp files/.vimrc " + client + "@" + server
					+ ":~");
		}
	}

	/**
	 * Creates and registers new JFOP Server instance for advanced AJO training.
	 *
	 * @param client The current client.
	 * @param ctx The database context.
	 * @param password The password definition object.
	 * @throws CommandException Thrown if password definition object could not opened
	 * for updating.
	 */
	private void advancedTrainingSetup(String client, DbContext ctx,
			Password password) throws CommandException {
		logger.debug(String.format(
				"Doing JFOP Server setup for advanced training for client %s",
				client));
		String no = client.substring(client.length() - 1);
		logger.debug(String.format("Number of client %s was %s", client, no));
		JFOPServerEditor jfopServerEditor = ctx.newObject(JFOPServerEditor.class);
		jfopServerEditor.setSwd("DEBUGSY" + no);
		jfopServerEditor.setDescr("JFOP Server Instance for Password 26");
		jfopServerEditor.setJfopServerInstanceName("DEBUGSY" + no);
		int portSuffix = Integer.parseInt(no);
		logger.debug("Client number successfully converted to integer");
		jfopServerEditor.setJfopServerDebuggerPort(8010 + portSuffix);
		logger.debug(String.format("Debugger port field set to port number %d",
				jfopServerEditor.getJfopServerDebuggerPort()));
		jfopServerEditor.commit();
		logger.debug(String.format(
				"New JFOP Server instance %s created for debugging in client %s",
				jfopServerEditor.objectId().getIdno(), client));
		JFOPServer jfopServer = jfopServerEditor.objectId();
		PasswordEditor passwordEditor = password.createEditor();
		passwordEditor.open(EditorAction.UPDATE);
		passwordEditor.setJfopServer(jfopServer);
		passwordEditor.commit();
		logger.debug(String
				.format("JFOP Server instance %s successfully entered in password definition of client %s",
						jfopServer.getIdno(), client));
	}

	/**
	 * Deletes all JFOP Server instances.
	 *
	 * @param ctx The database context.
	 */
	private void deleteAllJfopServerInstances(DbContext ctx) {
		logger.debug("Deleting all JFOP Server instances");
		SelectionBuilder<JFOPServer> selectionBuilder =
				SelectionBuilder.create(JFOPServer.class);
		Query<JFOPServer> query = ctx.createQuery(selectionBuilder.build());
		int no = query.execute().size();
		for (JFOPServer jfopServer : query) {
			jfopServer.delete();
		}
		logger.debug(String.format("%d JFOP Server instances were deleted", no));
	}

	/**
	 * All infosystems to initialize.
	 *
	 * @return Returns names of all infosystems to initialize as an ArrayList.
	 */
	private ArrayList<String> getInfosystems() {
		ArrayList<String> infosystemNames = new ArrayList<String>();
		infosystemNames.add("VARNAMELIST");
		infosystemNames.add("GIT");
		infosystemNames.add("INVENTUR");
		return infosystemNames;
	}

	/**
	 * Resets header fields of infosystem.
	 *
	 * @param infosytemEditor The editor instance of the current infosystem.
	 */
	private void initHeaderFields(InfosystemEditor infosytemEditor) {
		infosytemEditor.setArchiveFOP("");
		infosytemEditor.setReportHead("");
		infosytemEditor.setReportFoot("");
		infosytemEditor.setGrpHead("");
		infosytemEditor.setGrpFoot("");
		infosytemEditor.setTab("");
		infosytemEditor.setScrEnterEFOP("");
		infosytemEditor.setScrValidationEFOP("");
		infosytemEditor.setScrExitEFOP("");
		infosytemEditor.setScrEndEFOP("");
		infosytemEditor.setScrCancelEFOP("");
		infosytemEditor.setRowInsBeforeEFOP("");
		infosytemEditor.setRowInsAfterEFOP("");
		infosytemEditor.setRowDelBeforeEFOP("");
		infosytemEditor.setRowDelAfterEFOP("");
		infosytemEditor.setRowHighlightEFOP("");
		infosytemEditor.setRowMoveBeforeEFOP("");
		infosytemEditor.setRowMoveAfterEFOP("");
		infosytemEditor.setRowChangeEFOP("");
	}

	/**
	 * Resets row fields of infosystem.
	 *
	 * @param row The current table row of the infosystem.
	 */
	private void initRowFields(Row row) {
		row.setBtnBeforeEFOP("");
		row.setBtnAfterEFOP("");
		row.setFieldFillEFOP("");
		row.setFieldValidationEFOP("");
		row.setFieldExitEFOP("");
	}

	/**
	 * Selects an infosystem.
	 *
	 * @param ctx The database context.
	 * @param swd The search word of the infosystem.
	 * @return The infosystem as an instance of the class Infosystem.
	 */
	private Infosystem selectInfosystem(DbContext ctx, String swd) {
		SelectionBuilder<Infosystem> selectionBuilder =
				SelectionBuilder.create(Infosystem.class);
		selectionBuilder.add(Conditions.eq(Infosystem.META.swd, swd));
		return QueryUtil.getFirst(ctx, selectionBuilder.build());
	}

	private Password selectPassword(DbContext ctx) {
		SelectionBuilder<Password> selectionBuilder =
				SelectionBuilder.create(Password.class);
		selectionBuilder.add(Conditions.eq(Password.META.idno, "26"));
		Password password = QueryUtil.getFirst(ctx, selectionBuilder.build());
		return password;
	}

}
