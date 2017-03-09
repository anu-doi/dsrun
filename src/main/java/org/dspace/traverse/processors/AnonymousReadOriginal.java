/**
 * 
 */
package org.dspace.traverse.processors;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.traverse.ItemProcessingException;

/**
 * @author Genevieve Turner
 *
 */
public class AnonymousReadOriginal extends AbstractPermissionsProcessor {
	private static final String ANONYMOUS_GROUPNAME = "Anonymous";
	
	/**
	 *  Set the items bitstream READ permission to Anonymous
	 */
	@Override
	public void processItem(Item item) throws ItemProcessingException {
		try {
			Group anonymousGroup = Group.findByName(c, ANONYMOUS_GROUPNAME);
			for (Bundle bundle : item.getBundles()) {
				if (bundle.getName().equals("ORIGINAL")) {
					updateResourcePolicies(bundle, anonymousGroup);
					for (Bitstream bitstream : bundle.getBitstreams()) {
						updateResourcePolicies(bitstream, anonymousGroup);
					}
				}
			}
		} catch (SQLException e) {
			throw new ItemProcessingException(e);
		} catch (AuthorizeException e) {
			throw new ItemProcessingException(e);
		}
	}
	
	/**
	 * Set the collections DEFAULT_BITSTREAM_READ policy to Aonnymous
	 */
	@Override
	public void processCollection(Collection collection) throws ItemProcessingException {
		try {
			Group anonymousGroup = Group.findByName(c, ANONYMOUS_GROUPNAME);
			updateResourcePolicies(collection, anonymousGroup, Constants.DEFAULT_BITSTREAM_READ);
		} catch (SQLException e) {
			throw new ItemProcessingException(e);
		} catch (AuthorizeException e) {
			throw new ItemProcessingException(e);
		}
	}
}
