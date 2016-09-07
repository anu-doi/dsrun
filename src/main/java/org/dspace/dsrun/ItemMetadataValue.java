/**
 * 
 */
package org.dspace.dsrun;

import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * @author Rahul Khanna
 *
 */
public class ItemMetadataValue {

	private static Context c;

	public static void main(String[] args) throws Exception {
		Options cliOptions = new Options();
		cliOptions.addOption(null, "handle", true, "handle of item");
		cliOptions.addOption(null, "id", true, "id of item");
		cliOptions.addOption(null, "field", true,
				"fully qualified name of field. e.g. dc.contributor.author, dc.title, dc.description.*");
		cliOptions.addOption(null, "value", true, "value to update");
		cliOptions.addOption(null, "clear", false,
				"delete existing values for specified field before adding value making the value provided the only value for the specified field");

		cliOptions.addOption("n", "dry-run", false, "dry run - changes saved");
		cliOptions.addOption("h", "help", false, "display this help");

		CommandLineParser cliParser = new PosixParser();
		CommandLine cmdLine = null;
		cmdLine = cliParser.parse(cliOptions, args);

		// if '--help' then display help and exit
		if (args.length == 0 || cmdLine.hasOption("help")) {
			printHelp(cliOptions);
			return;
		}

		// dry run
		boolean isDryRun;
		if (cmdLine.hasOption("dry-run")) {
			System.out.println("Resource traversal will be performed in Dry Run mode.");
			isDryRun = true;
		} else {
			isDryRun = false;
		}

		// clear existing field values
		boolean clearExistingValues;
		if (cmdLine.hasOption("clear")) {
			clearExistingValues = true;
		} else {
			clearExistingValues = false;
		}

		// value
		String value = null;
		if (cmdLine.hasOption("value")) {
			value = cmdLine.getOptionValue("value");
		}

		// field
		Field field = null;
		if (cmdLine.hasOption("field")) {
			field = new Field(cmdLine.getOptionValue("field"));
		}

		try {
			initContext();

			// item from handle or id
			Item item = null;
			if (cmdLine.hasOption("handle")) {
				String handle = cmdLine.getOptionValue("handle");
				DSpaceObject o = HandleManager.resolveToObject(c, handle);
				if (o instanceof Item) {
					item = (Item) o;
				}
			} else if (cmdLine.hasOption("id")) {
				int id = Integer.parseInt(cmdLine.getOptionValue("id"));
				item = (Item) Item.find(c, Constants.ITEM, id);
			} else {
				print("Handle or ID must be provided");
			}

			processItem(item, field, value, clearExistingValues, isDryRun);
			print("%s(%s): %s %s=%s", item.getHandle(), item.getID(), clearExistingValues ? "REPLACE" : "APPEND",
					field.toString(), value);
		} catch (SQLException | AuthorizeException e) {
			e.printStackTrace();
		} finally {
			closeContext();
		}
	}

	private static void processItem(Item item, Field field, String value, boolean clearExistingValues, boolean isDryRun)
			throws SQLException, AuthorizeException {
		Objects.requireNonNull(item);
		Objects.requireNonNull(field);

		// clear existing values for specified field
		if (clearExistingValues) {
			item.clearMetadata(field.schema, field.element, field.qualifier, null);
		}

		// add metadata value to specified field
		if (value != null && value.length() > 0) {
			item.addMetadata(field.schema, field.element, field.qualifier, null, value);
		}

		// if it's a dry run then don't update the record
		if (!isDryRun) {
			item.updateMetadata();
		}
	}

	private static void initContext() throws SQLException {
		c = new Context();
		c.turnOffAuthorisationSystem();
	}

	private static void closeContext() {
		if (c != null) {
			try {
				c.complete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void printHelp(Options cliOptions) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(ItemMetadataValue.class.getName(), cliOptions);
	}

	private static void print(String str, Object... varargs) {
		System.out.format(str, varargs);
		System.out.println();
	}

	private static class Field {
		private final String schema;
		private final String element;
		private final String qualifier;

		public Field(String fullyQualifiedFieldName) throws IllegalArgumentException {
			String[] parts = fullyQualifiedFieldName.split(Pattern.quote("."));
			if (!(parts.length == 2 || parts.length == 3)) {
				throw new IllegalArgumentException(String.format("Invalid field: %s", fullyQualifiedFieldName));
			}
			schema = trim(parts[0]);
			element = trim(parts[1]);
			if (parts.length == 3) {
				qualifier = trim(parts[2]);
			} else {
				qualifier = null;
			}
		}

		private String trim(String str) {
			return str.trim();
		}

		@Override
		public String toString() {
			if (qualifier != null) {
				return String.format("%s.%s.%s", schema, element, qualifier);
			} else {
				return String.format("%s.%s", schema, element);
			}
		}
	}
}
