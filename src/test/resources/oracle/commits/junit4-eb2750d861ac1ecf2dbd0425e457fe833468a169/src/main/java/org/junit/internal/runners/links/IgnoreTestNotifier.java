/**
 * 
 */
package org.junit.internal.runners.links;

import org.junit.internal.runners.model.EachTestNotifier;

public class IgnoreTestNotifier extends Notifier {
	@Override
	public void run(EachTestNotifier context) {
		context.fireTestIgnored();
	}
}