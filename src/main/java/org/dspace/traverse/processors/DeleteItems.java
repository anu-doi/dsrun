/**
 * 
 */
package org.dspace.traverse.processors;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.traverse.AbstractItemProcessor;
import org.dspace.traverse.ItemProcessingException;

/**
 * @author Rahul Khanna
 *
 */
public class DeleteItems extends AbstractItemProcessor {

	private Context c;
	private boolean isDryRun;

	
	/* (non-Javadoc)
	 * @see org.dspace.traverse.ItemProcessor#setContext(org.dspace.core.Context)
	 */
	@Override
	public void setContext(Context c) {
		this.c = c;
		c.turnOffAuthorisationSystem();
	}

	/* (non-Javadoc)
	 * @see org.dspace.traverse.ItemProcessor#setDryRun(boolean)
	 */
	@Override
	public void setDryRun(boolean isDryRun) {
		this.isDryRun = isDryRun;
	}

	/* (non-Javadoc)
	 * @see org.dspace.traverse.ItemProcessor#processItem(org.dspace.content.Item)
	 */
	@Override
	public void processItem(Item item) throws ItemProcessingException {
		
		try {
			Collection owningCollection = item.getOwningCollection();
			String itemHandle = item.getHandle();
			if (!isDryRun) {
				owningCollection.removeItem(item);
			}
			print("DELETE %s (owner %s;%s)", itemHandle, owningCollection.getName(), owningCollection.getHandle()); 
		} catch (SQLException | AuthorizeException | IOException e) {
			throw new ItemProcessingException(e);
		}

	}

}
