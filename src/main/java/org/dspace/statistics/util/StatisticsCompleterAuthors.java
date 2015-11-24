/**
 * 
 */
package org.dspace.statistics.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.SolrLogger.ResultProcessor;

/**
 * Appends additional information to entries in the statistics core.
 * 
 * @author Rahul Khanna
 *
 */
public class StatisticsCompleterAuthors {

	private static int nProcessed = 0;
	
	// maximum number items to process
	private static int max2Process = Integer.MAX_VALUE;
	
	private static boolean isVerbose = false;

	private static String identifier;

	private static Context c;
	
	private static int curYear = Calendar.getInstance().get(Calendar.YEAR);
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();

		Options options = new Options();

		options.addOption("i", "identifier", true, "ONLY process bitstreams belonging to identifier (handle)");
		options.addOption("m", "maximum", true, "process no more than maximum items");
		options.addOption("v", "verbose", false, "print all extracted text and other details to STDOUT");
		options.addOption("h", "help", false, "help");

		CommandLine line = parser.parse(options, args);

		// display help and exit
		if (line.hasOption("h")) {
			printHelp(options, 0);
		}

		if (line.hasOption("i")) {
			identifier = line.getOptionValue("i");
		}

		if (line.hasOption("m")) {
			max2Process = Integer.parseInt(line.getOptionValue("m"), 10);
		}

		try {
			c = new Context(Context.READ_ONLY);
			if (identifier == null) {
				processAllItems();
			} else {
				Item itemToProcess = resolveItemToProcess();
				processItem(itemToProcess);
			}
			print("Processed " + nProcessed + " items.", true);
			
			print("", true);
			print("Committing changes to Solr... ", false);
			SolrLogger.solr.commit();
			print("done.", true);
		} finally {
			// shutdown connection to solr instance
			if (SolrLogger.solr != null) {
				try {
					SolrLogger.solr.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// cleanly close context
			if (c != null) {
				try {
					c.complete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static Item resolveItemToProcess() throws SQLException {
		DSpaceObject dso = HandleManager.resolveToObject(c, identifier);

		if (dso == null) {
			throw new IllegalArgumentException(String.format("Cannot resolve %s to a DSpace object", identifier));
		}

		if (dso.getType() != Constants.ITEM) {
			throw new IllegalArgumentException(
					String.format("%s is a %s. Only Items accepted. ", identifier, dso.getTypeText()));
		}
		return (Item) dso;
	}

	private static void processAllItems() throws SQLException, IOException, SolrServerException {
		// iterate over all items in the repository and update each one's solr
		// entries
		ItemIterator i = Item.findAll(c);
		try {
			while (i.hasNext() && nProcessed < max2Process) {
				Item iItem = i.next();
				if (iItem != null && iItem.getType() == Constants.ITEM) {
					processItem(iItem);
				}
			}
		} finally {
			if (i != null) {
				i.close();
			}
		}
	}

	private static void processItem(Item item) throws IOException, SolrServerException {
		String handle = item.getHandle();
		DCValue[] values = item.getMetadata("dc.contributor.author");
		final Set<String> metadataAuthors = new HashSet<String>(values.length);
		for (DCValue iValue : values) {
			metadataAuthors.add(iValue.value);
		}

		print(String.format("Processing item %d [handle=%s;id=%s] %d authors %s... ", nProcessed + 1, handle,
				item.getID(), values.length, metadataAuthors), false);

		/* Result Process to alter record to be identified as a bot */
		ResultProcessor processor = new ResultProcessor() {
			// public long totalEntries = 0L;
			// public long updatedEntries = 0L;

			public void process(SolrDocument doc) throws IOException, SolrServerException {
				java.util.Collection<Object> solrDocFieldValues = doc.getFieldValues("author");
				Set<String> solrEntryAuthors;
				if (solrDocFieldValues != null) {
					solrEntryAuthors = new HashSet<String>(solrDocFieldValues.size());
					for (Object iValue : solrDocFieldValues) {
						solrEntryAuthors.add((String) iValue);
					}
				} else {
					solrEntryAuthors = new HashSet<String>(0);
				}

				boolean requiresUpdate = false;

				if (metadataAuthors.size() != solrEntryAuthors.size()) {
					requiresUpdate = true;
				} else {
					for (String iAuthor : metadataAuthors) {
						if (!solrEntryAuthors.contains(iAuthor)) {
							requiresUpdate = true;
							break;
						}
					}
				}

				if (requiresUpdate) {
					doc.removeFields("author");
					doc.addField("author", metadataAuthors);
					SolrInputDocument newInput = ClientUtils.toSolrInputDocument(doc);
					SolrLogger.solr.add(newInput);
					// updatedEntries++;
				}
				// totalEntries++;
			}

			@Override
			public void process(List<SolrDocument> docs) throws IOException, SolrServerException {
				for (SolrDocument doc : docs) {
					try {
						process(doc);
					} catch (SolrServerException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (SolrException e) {
						e.printStackTrace();
					}
				}
			}

		};

		// process all solr entries for the item and bitstreams belonging to the
		// item
		for (int iYear = curYear; iYear >= 2011; iYear--) {
			for (int iMonth = 1; iMonth <= 12; iMonth++) {
				// can use 31 for months with less than 31 days
				String timeRange = String.format("[%d-%02d-01T00:00:00Z TO %d-%02d-31T23:59:59Z]", iYear, iMonth, iYear,
						iMonth);
				processor.execute(String.format("owningItem:%d AND type:%d AND time:%s AND -isBot:true", item.getID(),
						Constants.BITSTREAM, timeRange));
				// processor.commit();
				processor.execute(String.format("id:%d AND type:%d AND time:%s AND -isBot:true", item.getID(),
						Constants.ITEM, timeRange));
				processor.commit();
			}
		}
		nProcessed++;
		print("done.", true);
	}

	/**
	 * Print the help message
	 *
	 * @param options
	 *            The command line options the user gave
	 * @param exitCode
	 *            the system exit code to use
	 */
	private static void printHelp(Options options, int exitCode) {
		// print the help message
		HelpFormatter myhelp = new HelpFormatter();
		myhelp.printHelp("StatisticsImporter\n", options);
		System.exit(exitCode);
	}

	private static void print(String string, boolean appendNewline) {
		if (appendNewline) {
			System.out.println(string);
		} else {
			System.out.print(string);
		}
	}
}
