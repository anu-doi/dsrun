package org.dspace.traverse.processors;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.traverse.AbstractItemProcessor;

/**
 * 
 * @author Rahul Khanna
 * @author Genevieve Turner
 *
 */
public abstract class AbstractPermissionsProcessor extends AbstractItemProcessor {
	protected void updateResourcePolicies(DSpaceObject resource, Group group) throws SQLException, AuthorizeException {
		updateResourcePolicies(resource, group, Constants.READ);
	}
	
	protected void updateResourcePolicies(DSpaceObject resource, Group group, int actionType) throws SQLException, AuthorizeException {
		String resourcePath = generateResourcePath(resource);
		List<ResourcePolicy> policies = AuthorizeManager.getPolicies(c, resource);

		// check if anonymous group already has read access
		boolean isPolicyUpdateRequired = true;
		for (ResourcePolicy policy : policies) {
			if (policy.getAction() == actionType && group.equals(policy.getGroup())) {
				isPolicyUpdateRequired = false;
				break;
			}
		}

		// edit an existing policy if possible
		if (isPolicyUpdateRequired) {
			// count number of read policies
			int nReadPolicies = 0;
			for (ResourcePolicy policy : policies) {
				if (policy.getAction() == actionType) {
					nReadPolicies++;
				}
			}

			if (nReadPolicies == 0) {
				if (!isDryRun) {
					AuthorizeManager.addPolicy(c, resource, actionType, group);
				}
				print("%s ADD: %s", resourcePath, group.getName());
			} else if (nReadPolicies >= 1) {
				boolean isFirstPolicyUpdated = false;
				for (int i = 0; i < policies.size(); i++) {
					if (policies.get(i).getAction() == actionType) {
						if (!isFirstPolicyUpdated) {
							// update the first read policy
							String oldGroup = policies.get(i).getGroup() != null ? policies.get(i).getGroup().getName()
									: policies.get(i).getEPerson().getName();
							if (!isDryRun) {
								policies.get(i).setGroup(group);
								policies.get(i).setEPerson(null);
								policies.get(i).update();
							}
							print("%s UPDATE: %s -> %s", resourcePath, oldGroup, group.getName());
							isFirstPolicyUpdated = true;
						} else {
							// delete read policies after the first
							String oldGroup = policies.get(i).getGroup() != null ? policies.get(i).getGroup().getName()
									: policies.get(i).getEPerson().getName();
							if (!isDryRun) {
								policies.get(i).delete();
								policies.get(i).update();
							}
							print("%s DELETE: %s", resourcePath, oldGroup);
						}
					}
				}
			}
		}
	}

	protected String generateResourcePath(DSpaceObject resource) throws SQLException {
		String path = null;
		if (resource.getType() == Constants.ITEM) {
			path = String.format("%s(%d)", resource.getHandle(), resource.getID());
		} else if (resource.getType() == Constants.BUNDLE) {
			DSpaceObject parentItem = resource.getParentObject();
			path = String.format("%s(%d) > %s(%d)", parentItem.getHandle(), parentItem.getID(), resource.getName(),
					resource.getID());
		} else if (resource.getType() == Constants.BITSTREAM) {
			Item parentItem = (Item) resource.getParentObject();
			Bundle[] bundles = ((Bitstream) resource).getBundles();
			if (bundles.length > 0) {
				path = String.format("%s(%d) > %s(%d) > %s(%d)", parentItem.getHandle(), parentItem.getID(),
						bundles[0].getName(), bundles[0].getID(), resource.getName(), resource.getID());
			}
		}
		return path;
	}
}
