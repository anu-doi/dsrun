/**
 * 
 */
package org.dspace.traverse;

import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * Traverses through child items of a specified community or collection and
 * calls an instance of a specified class with ItemProcessor interface to
 * perform some action on each item. If the specified DSpace resource is an
 * item, then only that item is processed.
 * 
 * @author Rahul Khanna
 *
 */
public class ResourceTraverser {
	
	private static Context c;
	private static ItemProcessor itemProcessor;
	private static boolean isDryRun;
	
	private static int nSuccess = 0;
	private static int nError = 0;
	
	public static int main(String[] args) {
		
		Options cliOptions = new Options();
		cliOptions.addOption("p", "processor", true, "fully qualified classname of item processor");
		cliOptions.addOption("n", "dry-run", false, "dry run - changes saved");
		cliOptions.addOption("h", "help", false, "display this help");
		
		CommandLineParser cliParser = new PosixParser();
		CommandLine cmdLine = null;
		try {
			cmdLine = cliParser.parse(cliOptions, args);
		} catch (ParseException e) {
			e.printStackTrace();
			return 1;
		}
		
		if (cmdLine.hasOption("help")) {
			printHelp(cliOptions);
			return 0;
		}
		
		if (cmdLine.hasOption("dry-run")) {
			System.out.println("Resource traversal will be performed in Dry Run mode.");
			isDryRun = true;
		} else {
			isDryRun = false;
		}
		
		int retVal = 0;
		try {
			initContext();
			
			if (cmdLine.hasOption("processor")) {
				instantiateItemProcessor(cmdLine.getOptionValue("processor"));
			} else {
				throw new ItemProcessingException("No item processor specified.");
			}
			
			String[] handles = cmdLine.getArgs();
			if (handles != null && handles.length > 0) {
				traverseHandles(handles);
			} else {
				throw new ItemProcessingException("No handles to process");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			retVal = 1;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			retVal = 1;
		} catch (InstantiationException e) {
			e.printStackTrace();
			retVal = 1;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			retVal = 1;
		} catch (ItemProcessingException e) {
			e.printStackTrace();
			retVal = 1;
		} finally {
			closeContext();
			System.out.format("Finished. %d success, %d errors, %d total.",	nSuccess, nError, nSuccess + nError);
			System.out.println();
		}
		
		return retVal;
	}

	private static void printHelp(Options cliOptions) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ResourceTraverser", cliOptions);
	}

	private static void initContext() throws SQLException {
		c = new Context();
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

	private static void instantiateItemProcessor(String className)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className);
		itemProcessor = (ItemProcessor) clazz.newInstance();
		itemProcessor.setContext(c);
		itemProcessor.setDryRun(isDryRun);
	}
	
	private static void traverseHandles(String[] handles) throws SQLException, ItemProcessingException {
		for (String handle : handles) {
			processHandle(handle);
		}
	}
	
	private static void processHandle(String handle) throws SQLException, ItemProcessingException {
		DSpaceObject resource = HandleManager.resolveToObject(c, handle);
		processResource(resource);
	}
	
	private static void processResource(DSpaceObject resource) throws SQLException, ItemProcessingException {
		if (resource.getType() == Constants.COMMUNITY) {
			processCommunity((Community) resource);
		} else if (resource.getType() == Constants.COLLECTION) {
			processCollection((Collection) resource);
		} else if (resource.getType() == Constants.ITEM) {
			processItem((Item) resource);
		}
	}

	private static void processCommunity(Community community) throws SQLException, ItemProcessingException {
		for (Collection coll : community.getAllCollections()) {
			processCollection(coll);
		}
	}
	
	private static void processCollection(Collection coll) throws SQLException, ItemProcessingException {
		ItemIterator iterator = coll.getItems();
		while (iterator.hasNext()) {
			processItem(iterator.next());
		}
	}
	
	private static void processItem(Item item) {
		try {
			itemProcessor.processItem(item);
			nSuccess++;
		} catch (ItemProcessingException e) {
			nError++;
			e.getCause().printStackTrace();
		}
	}

}
