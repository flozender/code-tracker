//$Id$
package org.hibernate.search.test.jms.master;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.Context;

import org.apache.activemq.broker.BrokerService;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * Tests  that the Master node in a JMS cluster can propertly process messages placed onto the queue.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class JMSMasterTest extends SearchTestCase {

	/**
	 * Name of the test queue as found in JNDI  (see jndi.properties).
	 */
	private static final String QUEUE_NAME = "queue/searchtest";

	/**
	 * Name of the connection factort as found in JNDI (see jndi.properties).
	 */
	private static final String CONNECTION_FACTORY_NAME = "java:/ConnectionFactory";

	/**
	 * ActiveMQ message broker.
	 */
	private BrokerService brokerService;

	private QueueSession queueSession;

	public void testMessageSending() throws Exception {

		TShirt shirt = createObjectWithSQL();
		List<LuceneWork> queue = createDocumentAndWorkQueue( shirt );

		registerMessageListener();
		sendMessage( queue );

		// need to sleep to give JMS processing and indexing time
		Thread.sleep( 1000 );

		FullTextSession ftSess = Search.getFullTextSession( openSession() );
		ftSess.getTransaction().begin();
		QueryParser parser = new QueryParser( "id", new StopAnalyzer() );
		Query luceneQuery = parser.parse( "logo:jboss" );
		org.hibernate.Query query = ftSess.createFullTextQuery( luceneQuery );
		List result = query.list();
		assertEquals( 1, result.size() );
		ftSess.delete( result.get( 0 ) );
		ftSess.getTransaction().commit();
		ftSess.close();
	}

	private void registerMessageListener() throws Exception {
		MessageConsumer consumer = getQueueSession().createConsumer( getMessageQueue() );
		consumer.setMessageListener( new MDBSearchController( getSessions() ) );
	}

	private void sendMessage(List<LuceneWork> queue) throws Exception {
		ObjectMessage message = getQueueSession().createObjectMessage();
		message.setObject( ( Serializable ) queue );
		QueueSender sender = getQueueSession().createSender( getMessageQueue() );
		sender.send( message );
	}

	private Queue getMessageQueue() throws Exception {
		Context ctx = new javax.naming.InitialContext();
		return ( Queue ) ctx.lookup( QUEUE_NAME );
	}

	private QueueSession getQueueSession() throws Exception {
		if ( queueSession == null ) {
			Context ctx = new javax.naming.InitialContext();
			QueueConnectionFactory factory = ( QueueConnectionFactory ) ctx.lookup( CONNECTION_FACTORY_NAME );
			QueueConnection conn = factory.createQueueConnection();
			conn.start();
			queueSession = conn.createQueueSession( false, QueueSession.AUTO_ACKNOWLEDGE );

		}
		return queueSession;
	}

	/**
	 * Manually create the work queue. This lists gets send by the Slaves to the Master for indexing.
	 *
	 * @param shirt The shirt to index
	 *
	 * @return A manually create <code>LuceneWork</code> list.
	 */
	private List<LuceneWork> createDocumentAndWorkQueue(TShirt shirt) {
		Document doc = new Document();
		Field field = new Field(
				DocumentBuilder.CLASS_FIELDNAME, shirt.getClass().getName(), Field.Store.YES, Field.Index.NOT_ANALYZED
		);
		doc.add( field );
		field = new Field( "id", "1", Field.Store.YES, Field.Index.NOT_ANALYZED );
		doc.add( field );
		field = new Field( "logo", shirt.getLogo(), Field.Store.NO, Field.Index.ANALYZED );
		doc.add( field );
		LuceneWork luceneWork = new AddLuceneWork(
				shirt.getId(), String.valueOf( shirt.getId() ), shirt.getClass(), doc
		);
		List<LuceneWork> queue = new ArrayList<LuceneWork>();
		queue.add( luceneWork );
		return queue;
	}

	/**
	 * Create a test object without trigggering indexing. Use SQL directly.
	 *
	 * @return a <code>TShirt</code> test object.
	 *
	 * @throws SQLException in case the inset fails.
	 */
	private TShirt createObjectWithSQL() throws SQLException {
		Session s = openSession();
		s.getTransaction().begin();
		Statement statement = s.connection().createStatement();
		statement.executeUpdate(
				"insert into TShirt_Master(id, logo, size) values( '1', 'JBoss balls', 'large')"
		);
		statement.close();
		TShirt ts = ( TShirt ) s.get( TShirt.class, 1 );
		s.getTransaction().commit();
		s.close();
		return ts;
	}

	protected void setUp() throws Exception {
		// create and start the brokerService
		brokerService = new BrokerService();
		brokerService.setPersistent( false );
		brokerService.start();

		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if ( brokerService != null ) {
			brokerService.stop();
		}
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// explcitily set the backend even though lucene is default.
		cfg.setProperty( Environment.WORKER_BACKEND, "lucene" );
	}

	protected Class[] getMappings() {
		return new Class[] {
				TShirt.class
		};
	}
}
