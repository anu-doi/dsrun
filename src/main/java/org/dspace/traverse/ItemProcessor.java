/**
 * 
 */
package org.dspace.traverse;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for classes that make changes to an item. ResourceTraverser calls
 * processItem for each item within the resource (community/collection) it is
 * traversing.
 * 
 * @author Rahul Khanna
 *
 */
public interface ItemProcessor {

	public void setContext(Context c);
	
	public void setDryRun(boolean isDryRun);
	
	public void processItem(Item item) throws ItemProcessingException;
}
