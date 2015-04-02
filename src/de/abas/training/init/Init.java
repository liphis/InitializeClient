package de.abas.training.init;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Set;

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
import de.abas.erp.db.schema.part.Product;
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
		catch (CommandException | IOException e) {
			System.out.println("An error occurred: " + e.getMessage());
			logger.fatal(e.getMessage(), e);
			return;
		}
	}

	/**
	 * Removes all AJOPERF products.
	 */
	public void initAjoPerfProducts() {
		for (String client : clients) {
			DbContext ctx =
					ContextHelper.createClientContext(server, 6550, client, "sy",
							"Deleting AJOPERF products");
			logger.debug(String.format("deleting all AJOPERF products in client %s",
					client));
			SelectionBuilder<Product> selectionBuilder =
					SelectionBuilder.create(Product.class);
			selectionBuilder.add(Conditions.starts(Product.META.swd, "AJOPERF"));
			Query<Product> query = ctx.createQuery(selectionBuilder.build());
			int no = query.execute().size();
			for (Product product : query) {
				product.delete();
			}
			logger.debug(String.format("%d products with swd AJOPERF were deleted",
					no));
			ctx.close();
		}

	}

	/**
	 * Inserts or replaces .bashrc.
	 *
	 * @throws IOException Thrown if file could not be copied or InputStream instance
	 * could not be closed.
	 */
	public void initBashrc() throws IOException {
		for (String client : clients) {
			logger.debug(String.format("initializing .bashrc for client %s", client));
			copyFile(client, "", ".bashrc", "rwxrwx---");
		}
	}

	/**
	 * Replaces fop.txt with default fop.txt.
	 *
	 * @throws IOException Thrown if file could not be copied or InputStream instance
	 * could not be closed.
	 */
	public void initFopTxt() throws IOException {
		for (String client : clients) {
			logger.debug(String.format("initializing fop.txt for client %s", client));
			copyFile(client, "", "fop.txt", "rw-rw----");
		}

	}

	/**
	 * Inserts or replaces git-prompt.sh
	 *
	 * @throws IOException Thrown if file could not be copied or InputStream instance
	 * could not be closed.
	 */
	public void initGitPrompt() throws IOException {
		for (String client : clients) {
			logger.debug(String.format("initializing git-prompt.sh for client %s",
					client));
			copyFile(client, "", "git-prompt.sh", "rwxrwx---");
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
					if (!row.getEFOPSymbol().isEmpty()) {
						initRowFields(row);
					}
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

			FileVisitor<Path> fileVisitor = defineFileVisitor();

			File directory = new File(client + "/java", "projects");
			try (DirectoryStream<Path> directoryStream =
					Files.newDirectoryStream(directory.toPath())) {
				for (Path path : directoryStream) {
					Files.walkFileTree(path, fileVisitor);
				}
			}
			catch (IOException e) {
				logger.fatal(e.getMessage(), e);
				throw new RuntimeException(e);
			}
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
	 *
	 * @throws IOException Thrown if file could not be copied or InputStream instance
	 * could not be closed.
	 */
	public void initMandantClasspath() throws IOException {
		for (String client : clients) {
			logger.debug(String.format(
					"initializing mandant.classpath for client %s", client));
			copyFile(client, "java/", "mandant.classpath", "rwxrwx---");
		}

	}

	/**
	 * Inserts or replaces .vimrc.
	 *
	 * @throws IOException Thrown if file could not be copied or InputStream instance
	 * could not be closed.
	 */
	public void initVimrc() throws IOException {
		for (String client : clients) {
			logger.debug(String.format("initializing .vimrc for client %s", client));
			copyFile(client, "", ".vimrc", "rw-rw----");
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
	 * Changes file permissions.
	 *
	 * @param rights File permissions as String (e.g. rwxrwx---).
	 * @param path Path of the file for which to change permissions.
	 */
	private void chmod(String rights, Path path) {
		try {
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString(rights);
			Files.setPosixFilePermissions(path, perms);
		}
		catch (IOException e) {
			String message =
					"Changing permissions of file " + path.getFileName()
					+ " failed: " + e.getMessage();
			logger.fatal(message, e);
			throw new RuntimeException(message, e);
		}
	}

	/**
	 * Changes owner and group of file.
	 *
	 * @param owner The owner of the file (group is always users).
	 * @param path Path of the file for which to change owner and group.
	 */
	private void chown(String owner, Path path) {
		try {
			UserPrincipalLookupService lookupService =
					FileSystems.getDefault().getUserPrincipalLookupService();
			UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(owner);
			GroupPrincipal groupPrincipal =
					lookupService.lookupPrincipalByGroupName("users");
			PosixFileAttributeView fileAttributeView =
					Files.getFileAttributeView(path, PosixFileAttributeView.class,
							LinkOption.NOFOLLOW_LINKS);
			fileAttributeView.setOwner(userPrincipal);
			fileAttributeView.setGroup(groupPrincipal);
		}
		catch (IOException e) {
			String message =
					"Setting owner and group for file " + path.getFileName()
							+ " failed: " + e.getMessage();
			logger.fatal(message, e);
			throw new RuntimeException(message, e);
		}
	}

	/**
	 * Copies the specified file as resource from jar to the $MANDANTDIR of the
	 * specified client. Sets owner to the specified client and group to users. Sets
	 * the access rights as specified.
	 *
	 * @param client The client to copy to.
	 * @param filePath The path to the file.
	 * @param fileName The file to copy.
	 * @param rights The access rights of the target file.
	 * @throws IOException Thrown if file could not be copied or InputStream instance
	 * could not be closed.
	 */
	private void copyFile(String client, String filePath, String fileName,
			String rights) throws IOException {
		try (InputStream in =
				getClass().getClassLoader().getResourceAsStream(fileName)) {
			File dest = new File(client + "/" + filePath, fileName);
			Path path = dest.toPath();
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);

			chown(client, path);
			chmod(rights, path);
		}
	}

	/**
	 * Defines a new instance of FileVisitor to delete the given directory
	 * recursively.
	 *
	 * @return A FileVisitor instance.
	 */
	private FileVisitor<Path> defineFileVisitor() {
		return new FileVisitor<Path>() {

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				throw new RuntimeException(exc);
			}

		};
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
