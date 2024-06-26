/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.slave;

import java.util.List;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.jgroups.impl.MessageSerializationHelper;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * @author Lukasz Moren
 */
public class JGroupsReceiver extends ReceiverAdapter {

	public static volatile int queues;
	public static volatile int works;
	private SearchFactoryImplementor searchFactory;

	public JGroupsReceiver(SearchFactoryImplementor searchFactory) {
		this.searchFactory = searchFactory;
	}

	public static void reset() {
		queues = 0;
		works = 0;
	}

	@Override
	public void receive(Message message) {
		try {
			final byte[] rawBuffer = message.getRawBuffer();
			final int messageOffset = message.getOffset();
			final int bufferLength = message.getLength();
			String indexName = MessageSerializationHelper.extractIndexName( messageOffset, rawBuffer );
			byte[] serializedQueue = MessageSerializationHelper.extractSerializedQueue( messageOffset, bufferLength, rawBuffer );
			IndexManager indexManager = searchFactory.getIndexManagerHolder().getIndexManager( indexName );
			List<LuceneWork> queue = indexManager.getSerializer().toLuceneWorks( serializedQueue );
			queues++;
			works += queue.size();
		}
		catch (ClassCastException e) {
			throw new SearchException( e );
		}
	}
}
