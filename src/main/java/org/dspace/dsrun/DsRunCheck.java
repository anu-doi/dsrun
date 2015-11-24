package org.dspace.dsrun;

import java.sql.SQLException;

import org.dspace.core.Context;

/**
 * @author Rahul Khanna
 *
 */
public class DsRunCheck {
	private static Context c;
	
	public static void main(String[] args) {
		System.out.println("Command line options specified:");
		for (int i = 0; i < args.length; i++) {
			System.out.format("%d %s", i + 1, args[i]);
			System.out.println();
		}
		
		try {
			System.out.println("Initialising Context...");
			initContext();
			System.out.println("Context initialised.");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			closeContext();
		}
	}
	
	private static void initContext() throws SQLException {
		c = new Context();
	}
	
	private static void closeContext() {
		if (c != null) {
			try {
				System.out.println("Closing context...");
				c.complete();
				System.out.println("Context closed.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
