/**
 * 
 */
package org.dspace.dsrun;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

/**
 * @author Rahul Khanna
 *
 */
public class DisplayItem {

	private static Context context;

	
	public static void main(String[] args) throws Exception {

		Options cliOptions = new Options();
		cliOptions.addOption(null, "handle", true, "handle of item");
		cliOptions.addOption(null, "id", true, "id of item");
		cliOptions.addOption("h", "help", false, "display this help");

		CommandLineParser cliParser = new PosixParser();
		CommandLine cmdLine = null;
		cmdLine = cliParser.parse(cliOptions, args);

		// if '--help' then display help and exit
		if (args.length == 0 || cmdLine.hasOption("help")) {
			printHelp(cliOptions);
			return;
		}
		

		try {
			initContext();
			
			Item item = null;
			if (cmdLine.hasOption("handle")) {
				String handle = cmdLine.getOptionValue("handle");
				DSpaceObject o = HandleManager.resolveToObject(context, handle);
				if (o instanceof Item) {
					item = (Item) o;
				}
			} else if (cmdLine.hasOption("id")) {
				int id = Integer.parseInt(cmdLine.getOptionValue("id"));
				item = (Item) Item.find(context, Constants.ITEM, id);
			} else {
				print("Handle or ID must be provided");
			}

			displayItem(item);
		} finally {
			closeContext();
		}
	}
	
	private static void displayItem(Item item) {
		print("Handle: %s", item.getHandle());
		print("Internal ID: %s", item.getID());
		print("Last Modified: %s", item.getLastModified());
		print("Archived: %s", Boolean.valueOf(item.isArchived()));
		print("Discoverable: %s", Boolean.valueOf(item.isDiscoverable()));

		EPerson submitter;
		try {
			submitter = item.getSubmitter();
			print("SUBMITTER");
			print("\tUsername: %s", submitter.getName());
			print("\tNetID: %s", submitter.getNetid());
			print("\tFirst Name: %s", submitter.getFirstName());
			print("\tLast Name: %s", submitter.getLastName());
			print("\tLast Active: %s", submitter.getLastActive());
			print("\tSelf Registered: %s", Boolean.toString(submitter.getSelfRegistered()));
			print("");
		} catch (SQLException e) {
			print("Error retrieving submitter: %s", e.getMessage());
		}

		try {
			int i = 1;
			List<ResourcePolicy> itemPolicies = AuthorizeManager.getPolicies(context, item);
			for (ResourcePolicy policy : itemPolicies) {
				if (policy.getEPerson() != null) {
					print("\tItem Policy %d/%d: %d %s %s(%s)", i, itemPolicies.size(), policy.getID(),
							policy.getActionText(), policy.getEPerson().getEmail(), policy.getEPerson().getNetid());
				} else if (policy.getGroup() != null) {
					print("\tItem Policy %d/%d: %d %s %s", i, itemPolicies.size(), policy.getID(),
							policy.getActionText(), policy.getGroup().getName());
				}
				i++;
			}

		} catch (SQLException e) {
			print("Error retrieving policiesfor item %s(id=%d): %s", item.getHandle(), item.getID(), e.getMessage());
		}

		Metadatum[] metadata = item.getMetadataByMetadataString("*.*.*");
		print("METADATA: %d fields", metadata.length);
		for (Metadatum m : metadata) {
			print("%s %d (%s): %s", m.getField(), m.confidence, m.language, m.value);
		}

		Bundle[] bundles;
		try {
			bundles = item.getBundles();
			for (Bundle bundle : bundles) {
				print("BUNDLE %s", bundle.getName());

				Bitstream[] bitstreams = bundle.getBitstreams();
				for (Bitstream bs : bitstreams) {
					print("\tBITSTREAM SequenceID:%d %s", bs.getSequenceID(),
							bs.getID() == bundle.getPrimaryBitstreamID() ? "<PRIMARY>" : "");
					print("\t\tName: %s", bs.getName());
					print("\t\tSize: %d bytes", bs.getSize());
					print("\t\tMime Type: %s", bs.getFormat().getMIMEType());
					print("\t\t%s: %s", bs.getChecksumAlgorithm(), bs.getChecksum());
					print("\t\tDescription: %s", bs.getDescription());
					print("\t\tSource: %s", bs.getSource());
					print("\t\tID: %d", bs.getID());
					Metadatum[] bsMetadata = bs.getMetadataByMetadataString("*.*.*");
					for (Metadatum bsm : bsMetadata) {
						print("\t\t%s: %s", bsm.getField(), bsm.value);
					}

					try {
						List<ResourcePolicy> bsPolicies = AuthorizeManager.getPolicies(context, bs);
						int i = 1;
						for (ResourcePolicy policy : bsPolicies) {
							if (policy.getEPerson() != null) {
								print("\t\tBitstream Policy %d/%d: %d %s %s(%s)", i, bsPolicies.size(), policy.getID(),
										policy.getActionText(), policy.getEPerson().getEmail(),
										policy.getEPerson().getNetid());
							} else if (policy.getGroup() != null) {
								print("\t\tBitstream Policy %d/%d: %d %s %s", i, bsPolicies.size(), policy.getID(),
										policy.getActionText(), policy.getGroup().getName());
							}
							i++;
						}
					} catch (SQLException e) {
						print("Error retrieving policies for bitstream id=%d name=%s: %s", bs.getID(), bs.getName(),
								e.getMessage());
					}
				}

				try {
					List<ResourcePolicy> bundlePolicies = AuthorizeManager.getPolicies(context, bundle);
					int i = 1;
					for (ResourcePolicy policy : bundlePolicies) {
						if (policy.getEPerson() != null) {
							print("\tBundle Policy %d/%d: %d %s %s(%s)", i, bundlePolicies.size(), policy.getID(),
									policy.getActionText(), policy.getEPerson().getEmail(),
									policy.getEPerson().getNetid());
						} else if (policy.getGroup() != null) {
							print("\tBundle Policy %d/%d: %d %s %s", i, bundlePolicies.size(), policy.getID(),
									policy.getActionText(), policy.getGroup().getName());
						}
						i++;
					}
				} catch (SQLException e) {
					print("Error retrieving policies for bundle %s: %s", bundle.getName(), e.getMessage());
				}
			}

		} catch (SQLException e) {
			print("Error retrieving bundles: %s", e.getMessage());
		}

	}

	private static void initContext() throws SQLException {
		context = new Context(Context.READ_ONLY);
		context.turnOffAuthorisationSystem();
	}

	private static void closeContext() {
		if (context != null) {
			try {
				context.complete();
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

}
