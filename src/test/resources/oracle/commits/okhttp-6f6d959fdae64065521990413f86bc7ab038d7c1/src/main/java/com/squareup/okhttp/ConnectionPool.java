package com.squareup.okhttp;

import com.squareup.okhttp.internal.Platform;
import com.squareup.okhttp.internal.Util;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages reuse of HTTP and SPDY connections for reduced network latency. HTTP requests that share
 * the same {@link com.squareup.okhttp.Address} may share a {@link com.squareup.okhttp.Connection}.
 * This class implements the policy of which connections to keep open for future use.
 *
 * <p>The {@link #getDefault() system-wide default} uses system properties for tuning parameters:
 * <ul>
 * <li>{@code http.keepAlive} true if HTTP and SPDY connections should be pooled at all.
 * Default is true.
 * <li>{@code http.keepAliveDuration} Time in milliseconds to keep the connection alive in the pool
 * before closing it. Default is 20000.
 * <li>{@code http.maxConnections} maximum number of idle connections to each to keep in the pool.
 * Default is 5.
 * </ul>
 *
 * <p>The default instance <i>doesn't</i> adjust its configuration as system properties are changed.
 * This assumes that the applications that set these parameters do so before making HTTP
 * connections, and that this class is initialized lazily.
 */
public class ConnectionPool {
  private static final int MAX_CONNECTIONS_TO_CLEANUP = 2;
  private static final long DEFAULT_KEEP_ALIVE_DURATION_MS = 5 * 60 * 1000; // 5 min

  private static final ConnectionPool systemDefault;

  static {
    String keepAlive = System.getProperty("http.keepAlive");
    String keepAliveDuration = System.getProperty("http.keepAliveDuration");
    String maxIdleConnections = System.getProperty("http.maxConnections");
    long keepAliveDurationMs = keepAliveDuration != null ? Long.parseLong(keepAliveDuration)
        : DEFAULT_KEEP_ALIVE_DURATION_MS;
    if (keepAlive != null && !Boolean.parseBoolean(keepAlive)) {
      systemDefault = new ConnectionPool(0, keepAliveDurationMs);
    } else if (maxIdleConnections != null) {
      systemDefault = new ConnectionPool(Integer.parseInt(maxIdleConnections), keepAliveDurationMs);
    } else {
      systemDefault = new ConnectionPool(5, keepAliveDurationMs);
    }
  }

  /** The maximum number of idle connections for each address. */
  private final int maxIdleConnections;
  private final long keepAliveDurationNs;

  private final LinkedList<Connection> connections = new LinkedList<Connection>();

  /** We use a single background thread to cleanup expired connections. */
  private final ExecutorService executorService =
      new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
  private final Callable<Void> connectionsCleanupCallable = new Callable<Void>() {
    @Override public Void call() throws Exception {
      List<Connection> expiredConnections = new ArrayList<Connection>(MAX_CONNECTIONS_TO_CLEANUP);
      int idleConnectionCount = 0;
      synchronized (ConnectionPool.this) {
        for (Iterator<Connection> i = connections.descendingIterator(); i.hasNext(); ) {
          Connection connection = i.next();
          if (!connection.isAlive() || connection.isExpired(keepAliveDurationNs)) {
            i.remove();
            expiredConnections.add(connection);
            if (expiredConnections.size() == MAX_CONNECTIONS_TO_CLEANUP) break;
          } else if (connection.isIdle()) {
            idleConnectionCount++;
          }
        }

        for (Iterator<Connection> i = connections.descendingIterator(); i.hasNext()
            && idleConnectionCount > maxIdleConnections; ) {
          Connection connection = i.next();
          if (connection.isIdle()) {
            expiredConnections.add(connection);
            i.remove();
            --idleConnectionCount;
          }
        }
      }
      for (Connection expiredConnection : expiredConnections) {
        Util.closeQuietly(expiredConnection);
      }
      return null;
    }
  };

  public ConnectionPool(int maxIdleConnections, long keepAliveDurationMs) {
    this.maxIdleConnections = maxIdleConnections;
    this.keepAliveDurationNs = keepAliveDurationMs * 1000 * 1000;
  }

  /**
   * Returns a snapshot of the connections in this pool, ordered from newest to
   * oldest. Waits for the cleanup callable to run if it is currently scheduled.
   */
  List<Connection> getConnections() {
    waitForCleanupCallableToRun();
    synchronized (this) {
      return new ArrayList<Connection>(connections);
    }
  }

  /**
   * Blocks until the executor service has processed all currently enqueued
   * jobs.
   */
  private void waitForCleanupCallableToRun() {
    try {
      executorService.submit(new Runnable() {
        @Override public void run() {
        }
      }).get();
    } catch (Exception e) {
      throw new AssertionError();
    }
  }

  public static ConnectionPool getDefault() {
    return systemDefault;
  }

  /**
   * Returns total number of connections in the pool.
   */
  public synchronized int getConnectionCount() {
    return connections.size();
  }

  /**
   * Returns total number of spdy connections in the pool.
   */
  public synchronized int getSpdyConnectionCount() {
    int total = 0;
    for (Connection connection : connections) {
      if (connection.isSpdy()) total++;
    }
    return total;
  }

  /**
   * Returns total number of http connections in the pool.
   */
  public synchronized int getHttpConnectionCount() {
    int total = 0;
    for (Connection connection : connections) {
      if (!connection.isSpdy()) total++;
    }
    return total;
  }

  /** Returns a recycled connection to {@code address}, or null if no such connection exists. */
  public synchronized Connection get(Address address) {
    Connection foundConnection = null;
    for (Iterator<Connection> i = connections.descendingIterator(); i.hasNext(); ) {
      Connection connection = i.next();
      if (!connection.getAddress().equals(address)
          || !connection.isAlive()
          || System.nanoTime() - connection.getIdleStartTimeNs() >= keepAliveDurationNs) {
        continue;
      }
      i.remove();
      if (!connection.isSpdy()) {
        try {
          Platform.get().tagSocket(connection.getSocket());
        } catch (SocketException e) {
          Util.closeQuietly(connection);
          // When unable to tag, skip recycling and close
          Platform.get().logW("Unable to tagSocket(): " + e);
          continue;
        }
      }
      foundConnection = connection;
      break;
    }

    if (foundConnection != null && foundConnection.isSpdy()) {
      connections.addFirst(foundConnection); // Add it back after iteration.
    }

    executorService.submit(connectionsCleanupCallable);
    return foundConnection;
  }

  /**
   * Gives {@code connection} to the pool. The pool may store the connection,
   * or close it, as its policy describes.
   *
   * <p>It is an error to use {@code connection} after calling this method.
   */
  public void recycle(Connection connection) {
    executorService.submit(connectionsCleanupCallable);

    if (connection.isSpdy()) {
      return;
    }

    if (!connection.isAlive()) {
      Util.closeQuietly(connection);
      return;
    }

    try {
      Platform.get().untagSocket(connection.getSocket());
    } catch (SocketException e) {
      // When unable to remove tagging, skip recycling and close.
      Platform.get().logW("Unable to untagSocket(): " + e);
      Util.closeQuietly(connection);
      return;
    }

    synchronized (this) {
      connections.addFirst(connection);
      connection.resetIdleStartTime();
    }
  }

  /**
   * Shares the SPDY connection with the pool. Callers to this method may
   * continue to use {@code connection}.
   */
  public void maybeShare(Connection connection) {
    executorService.submit(connectionsCleanupCallable);
    if (!connection.isSpdy()) {
      // Only SPDY connections are sharable.
      return;
    }
    if (connection.isAlive()) {
      synchronized (this) {
        connections.addFirst(connection);
      }
    }
  }
}
