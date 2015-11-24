/**
 * 
 */
package org.dspace.bitstreams.register;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

/**
 * @author Rahul Khanna
 *
 */
public class RegisterBitstream {
	private static final int DEFAULT_ASSETSTORE = 1;

	private static Context c;

	private static CommandLine cmdLine;

	public static void main(String[] args) {
		try {
			Options options = createOptions();
			cmdLine = parseCommandLine(options, args);

			// display help and exit
			if (cmdLine.hasOption('h')) {
				displayHelp(options);
				return;
			}
			
			initContext();
			c.turnOffAuthorisationSystem();
			
			// look up item using handle or workspace item id
			Item item = lookupItem();
			
			String bundleName = getBundleName();
			Bundle bundle = createOrGetBundle(item, bundleName);
			
			int assetStoreNum = getAssetStore();
			
			// bitstream path relative to asset store
			String bitstreamPath = getBitstreamPath();
			
			// description, if provided
			String description = getDescription();
			
			System.out.format("Adding [%s] to %s(%d)... ", bitstreamPath, item.getHandle(), item.getID());
			registerBitstream(c, bundle, assetStoreNum, bitstreamPath, description);
			System.out.println("[DONE]");
			item.update();
			c.complete();
		} catch (ParseException e) {
			System.out.println("[ERROR]");
			e.printStackTrace();
			c.abort();
			return;
		} catch (SQLException e) {
			System.out.println("[ERROR]");
			e.printStackTrace();
			c.abort();
			return;
		} catch (AuthorizeException e) {
			System.out.println("[ERROR]");
			e.printStackTrace();
			c.abort();
			return;
		} catch (IOException e) {
			System.out.println("[ERROR]");
			e.printStackTrace();
			c.abort();
			return;
		}

		return;

	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption("i", "handle", true, "handle of item (for an approved item)");
		options.addOption("w", "workspaceid", true,
				"workspace item id (for an item in workspace that's not yet submitted");
		options.addOption("b", "bundle", true, "name of bundle to add bitstream to");
		options.addOption("a", "assetstore", true, "Number of assetstore");
		options.addOption("p", "path", true, "path to the bitstream relative to the assetstore location");
		options.addOption("d", "description", true, "description");
		options.addOption("h", "help", false, "help");
		return options;
	}

	private static CommandLine parseCommandLine(Options options, String[] args) throws ParseException {
		CommandLineParser parser = new PosixParser();
		return parser.parse(options, args, false);
	}

	private static void initContext() throws SQLException {
		c = new Context();
	}

	private static Item lookupItem() throws IllegalStateException, SQLException {
		Item item = null;
		if (cmdLine.hasOption('i')) {
			// item's handle provided
			String handle = cmdLine.getOptionValue("i");
			item = (Item) HandleManager.resolveToObject(c, handle);
		} else if (cmdLine.hasOption('w')) {
			// workspace item ID provided
			int workspaceItemId = parseInt(cmdLine.getOptionValue('w'));
			WorkspaceItem wItem = WorkspaceItem.find(c, workspaceItemId);
			item = wItem.getItem();
		}
		return item;
	}

	private static String getBundleName() {
		// lookup bundle if specified, else work with ORIGINAL bundle
		String bundleName;
		if (cmdLine.hasOption("b")) {
			bundleName = cmdLine.getOptionValue("b");
		} else {
			bundleName = Constants.DEFAULT_BUNDLE_NAME;
		}
		return bundleName;
	}

	private static Bundle createOrGetBundle(Item item, String bundleName) throws SQLException, AuthorizeException {
		Bundle bundle = null;
		Bundle[] bundles;
		bundles = item.getBundles(bundleName);
		if (bundles.length == 0) {
			// if bundle not found, create it.
			bundle = item.createBundle(bundleName);
		} else {
			// if bundle found, use it
			bundle = bundles[0];
		}
		return bundle;
	}

	/**
	 * Returns the asset store number specified on command line, or the default
	 * if not specified.
	 * 
	 * @return asset store number as int
	 */
	private static int getAssetStore() {
		return cmdLine.hasOption('a') ? parseInt(cmdLine.getOptionValue('a')) : DEFAULT_ASSETSTORE;
	}

	private static String getBitstreamPath() {
		String bitstreamPath = null;
		if (cmdLine.hasOption('p')) {
			bitstreamPath = cmdLine.getOptionValue('p');
		}
		return bitstreamPath;
	}

	private static String getDescription() {
		return cmdLine.hasOption('d') ? cmdLine.getOptionValue('d').trim() : "";
	}

	private static void displayHelp(Options options) {
		HelpFormatter myhelp = new HelpFormatter();
		myhelp.printHelp(RegisterBitstream.class.getName() + "\n", options);
	}

	private static void registerBitstream(Context c, Bundle targetBundle, int assetstore, String bitstreamPath,
			String description) throws AuthorizeException, IOException, SQLException {

		Bitstream bs = targetBundle.registerBitstream(assetstore, bitstreamPath);
		int iLastSlash = bitstreamPath.lastIndexOf('/');
		bs.setName(bitstreamPath.substring(iLastSlash + 1));
		BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
		bs.setFormat(bf);
		bs.setDescription(description);
		bs.update();
	}
	
	private static int parseInt(String intAsString) {
		return Integer.parseInt(intAsString, 10);
	}
}
