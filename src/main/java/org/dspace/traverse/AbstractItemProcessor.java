/**
 * 
 */
package org.dspace.traverse;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * @author Rahul Khanna
 *
 */
public abstract class AbstractItemProcessor implements ItemProcessor {

	protected Context c;
	protected boolean isDryRun;

	@Override
	public void setContext(Context c) {
		this.c = c;
	}

	@Override
	public void setDryRun(boolean isDryRun) {
		this.isDryRun = isDryRun;
	}

	protected static void print(String str, Object... varargs) {
		System.out.format(str, varargs);
		System.out.println();
	}

	@Override
	public void processItem(Item item) throws ItemProcessingException {
		// Do nothing
	}

	@Override
	public void processCollection(Collection collection) throws ItemProcessingException {
		// Do nothing
	}
	
	@Override
	public void processCommunity(Community community) throws ItemProcessingException {
		// Do nothing
	}
}
