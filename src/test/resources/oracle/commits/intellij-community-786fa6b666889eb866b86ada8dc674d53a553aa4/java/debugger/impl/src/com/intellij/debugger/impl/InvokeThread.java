package com.intellij.debugger.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.VMDisconnectedException;

import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: lex
 * Date: Mar 19, 2004
 * Time: 11:42:03 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class InvokeThread<E extends PrioritizedTask> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.impl.InvokeThread");

  private static final ThreadLocal<WorkerThreadRequest> ourWorkerRequest = new ThreadLocal<WorkerThreadRequest>();

  public static final class WorkerThreadRequest<E extends PrioritizedTask> implements Runnable {
    private final InvokeThread<E> myOwner;
    private volatile Future<?> myRequestFuture;

    WorkerThreadRequest(InvokeThread<E> owner) {
      myOwner = owner;
    }

    public void run() {
      synchronized (this) {
        while (myRequestFuture == null) {
          try {
            wait();
          }
          catch (InterruptedException e) {
          }
        }
      }
      ourWorkerRequest.set(this);
      try {
        myOwner.run(this);
      } 
      finally {
        ourWorkerRequest.set(null);
        boolean b = Thread.interrupted(); // reset interrupted status to return into pool
      }
    }

    public void interrupt() {
      assert myRequestFuture != null;
      myRequestFuture.cancel( true );  
    }

    public boolean isInterrupted() {
      assert myRequestFuture != null;
      return myRequestFuture.isCancelled();
    }

    public void join() throws InterruptedException, ExecutionException {
      assert myRequestFuture != null;
      try {
        myRequestFuture.get();
      } 
      catch(CancellationException ignored) {
      }
    }

    public void join(long timeout) throws InterruptedException, ExecutionException {
      assert myRequestFuture != null;
      try {
        myRequestFuture.get(timeout, TimeUnit.MILLISECONDS);
      }
      catch (TimeoutException ignored) {
      } 
      catch (CancellationException ignored) {
      }
    }

    final void setRequestFuture(Future<?> requestFuture) {
      synchronized (this) {
        myRequestFuture = requestFuture;
        notifyAll();
      }
    }

    public InvokeThread<E> getOwner() {
      return myOwner;
    }

    public boolean isDone() {
      assert myRequestFuture != null;
      return myRequestFuture.isDone();
    }
  }

  protected final EventQueue<E> myEvents;

  private volatile WorkerThreadRequest myCurrentRequest = null;

  public InvokeThread() {
    myEvents = new EventQueue<E>(PrioritizedTask.Priority.values().length);
    startNewWorkerThread();
  }

  protected abstract void processEvent(E e);

  protected void startNewWorkerThread() {
    final WorkerThreadRequest workerRequest = new WorkerThreadRequest<E>(this);
    myCurrentRequest = workerRequest;
    workerRequest.setRequestFuture( ApplicationManager.getApplication().executeOnPooledThread(workerRequest) );
  }

  public void run(WorkerThreadRequest current) {
    while(true) {
      try {
        if(current.isInterrupted()) {
          break;
        }

        if(getCurrentRequest() != current) {
          LOG.assertTrue(false, "Expected " + current + " instead of " + getCurrentRequest());
        }

        processEvent(myEvents.get());
      }
      catch (VMDisconnectedException e) {
        break;
      }
      catch (EventQueueClosedException e) {
        break;
      }
      catch (RuntimeException e) {
        if(e.getCause() instanceof InterruptedException) {
          break;
        }
        LOG.error(e);
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Request " + this.toString() + " exited");
    }
  }

  protected static InvokeThread currentThread() {
    final WorkerThreadRequest request = getCurrentThreadRequest();
    return request != null? request.getOwner() : null;
  }

  public void schedule(E r) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("schedule " + r + " in " + this);
    }
    myEvents.put(r, r.getPriority().ordinal());
  }

  public void pushBack(E r) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("pushBack " + r + " in " + this);
    }
    myEvents.pushBack(r, r.getPriority().ordinal());
  }

  protected void switchToRequest(WorkerThreadRequest newWorkerThread) {
    final WorkerThreadRequest request = getCurrentThreadRequest();
    LOG.assertTrue(request != null);
    myCurrentRequest = newWorkerThread;
    if (LOG.isDebugEnabled()) {
      LOG.debug("Closing " + request + " new request = " + newWorkerThread);
    }

    request.interrupt();
  }

  public WorkerThreadRequest getCurrentRequest() {
    return myCurrentRequest;
  }

  public static WorkerThreadRequest getCurrentThreadRequest() {
    return ourWorkerRequest.get();
  }

  public void close() {
    myEvents.close();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Closing evaluation");
    }
  }
}
