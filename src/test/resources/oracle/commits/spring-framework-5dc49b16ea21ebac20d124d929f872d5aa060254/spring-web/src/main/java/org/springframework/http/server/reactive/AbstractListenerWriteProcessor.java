/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@code Processor} implementations that bridge between
 * event-listener write APIs and Reactive Streams.
 *
 * <p>Specifically a base class for writing to the HTTP response body with
 * Servlet 3.1 non-blocking I/O and Undertow XNIO as well for writing WebSocket
 * messages through the Java WebSocket API (JSR-356), Jetty, and Undertow.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of element signaled to the {@link Subscriber}
 */
public abstract class AbstractListenerWriteProcessor<T> implements Processor<T, Void> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	@Nullable
	private Subscription subscription;

	@Nullable
	private volatile T currentData;

	private volatile boolean subscriberCompleted;

	private final WriteResultPublisher resultPublisher;

	private final String logPrefix;


	public AbstractListenerWriteProcessor() {
		this("");
	}

	/**
	 * Create an instance with the given log prefix.
	 * @since 5.1
	 */
	public AbstractListenerWriteProcessor(String logPrefix) {
		this.logPrefix = logPrefix;
		this.resultPublisher = new WriteResultPublisher(logPrefix);
	}


	/**
	 * Create an instance with the given log prefix.
	 * @since 5.1
	 */
	public String getLogPrefix() {
		return this.logPrefix;
	}


	// Subscriber methods and async I/O notification methods...

	@Override
	public final void onSubscribe(Subscription subscription) {
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(T data) {
		if (logger.isTraceEnabled()) {
			logger.trace(getLogPrefix() + "Item to write");
		}
		this.state.get().onNext(this, data);
	}

	/**
	 * Error signal from the upstream, write Publisher. This is also used by
	 * sub-classes to delegate error notifications from the container.
	 */
	@Override
	public final void onError(Throwable ex) {
		if (logger.isTraceEnabled()) {
			logger.trace(getLogPrefix() + "Write source error: " + ex);
		}
		this.state.get().onError(this, ex);
	}

	/**
	 * Completion signal from the upstream, write Publisher. This is also used
	 * by sub-classes to delegate completion notifications from the container.
	 */
	@Override
	public final void onComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace(getLogPrefix() + "No more items to write");
		}
		this.state.get().onComplete(this);
	}

	/**
	 * Invoked when writing is possible, either in the same thread after a check
	 * via {@link #isWritePossible()}, or as a callback from the underlying
	 * container.
	 */
	public final void onWritePossible() {
		if (logger.isTraceEnabled()) {
			logger.trace(getLogPrefix() + "onWritePossible");
		}
		this.state.get().onWritePossible(this);
	}

	/**
	 * Invoked during an error or completion callback from the underlying
	 * container to cancel the upstream subscription.
	 */
	public void cancel() {
		logger.trace(getLogPrefix() + "Cancellation");
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}

	// Publisher implementation for result notifications...

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	// Write API methods to be implemented or template methods to override...

	/**
	 * Whether the given data item has any content to write.
	 * If false the item is not written.
	 */
	protected abstract boolean isDataEmpty(T data);

	/**
	 * Template method invoked after a data item to write is received via
	 * {@link Subscriber#onNext(Object)}. The default implementation saves the
	 * data item for writing once that is possible.
	 */
	protected void dataReceived(T data) {
		if (this.currentData != null) {
			throw new IllegalStateException("Current data not processed yet: " + this.currentData);
		}
		this.currentData = data;
	}

	/**
	 * Whether writing is possible.
	 */
	protected abstract boolean isWritePossible();

	/**
	 * Write the given item.
	 * <p><strong>Note:</strong> Sub-classes are responsible for releasing any
	 * data buffer associated with the item, once fully written, if pooled
	 * buffers apply to the underlying container.
	 * @param data the item to write
	 * @return whether the current data item was written and another one
	 * requested ({@code true}), or or otherwise if more writes are required.
	 */
	protected abstract boolean write(T data) throws IOException;

	/**
	 * Invoked after the current data has been written and before requesting
	 * the next item from the upstream, write Publisher.
	 * <p>The default implementation is a no-op.
	 * @deprecated originally introduced for Undertow to stop write notifications
	 * when no data is available, but deprecated as of as of 5.0.6 since constant
	 * switching on every requested item causes a significant slowdown.
	 */
	@Deprecated
	protected void writingPaused() {
	}

	/**
	 * Invoked after onComplete or onError notification.
	 * <p>The default implementation is a no-op.
	 */
	protected void writingComplete() {
	}

	/**
	 * Invoked when an I/O error occurs during a write. Sub-classes may choose
	 * to ignore this if they know the underlying API will provide an error
	 * notification in a container thread.
	 * <p>Defaults to no-op.
	 */
	protected void writingFailed(Throwable ex) {
	}


	// Private methods for use from State's...

	private boolean changeState(State oldState, State newState) {
		boolean result = this.state.compareAndSet(oldState, newState);
		if (result && logger.isTraceEnabled()) {
			logger.trace(getLogPrefix() + oldState + " -> " + newState);
		}
		return result;
	}

	private void changeStateToReceived(State oldState) {
		if (changeState(oldState, State.RECEIVED)) {
			writeIfPossible();
		}
	}

	private void changeStateToComplete(State oldState) {
		if (changeState(oldState, State.COMPLETED)) {
			writingComplete();
			this.resultPublisher.publishComplete();
		}
		else {
			this.state.get().onComplete(this);
		}
	}

	private void writeIfPossible() {
		boolean result = isWritePossible();
		if (!result && logger.isTraceEnabled()) {
			logger.trace(getLogPrefix() + "Writing not possible");
		}
		if (result) {
			onWritePossible();
		}
	}


	/**
	 * Represents a state for the {@link Processor} to be in.
	 *
	 * <p><pre>
	 *        UNSUBSCRIBED
	 *             |
	 *             v
	 *   +--- REQUESTED -------------> RECEIVED ---+
	 *   |        ^                       ^        |
	 *   |        |                       |        |
	 *   |        + ------ WRITING <------+        |
	 *   |                    |                    |
	 *   |                    v                    |
	 *   +--------------> COMPLETED <--------------+
	 * </pre>
	 */
	private enum State {

		UNSUBSCRIBED {
			@Override
			public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
				Assert.notNull(subscription, "Subscription must not be null");
				if (processor.changeState(this, REQUESTED)) {
					processor.subscription = subscription;
					subscription.request(1);
				}
				else {
					super.onSubscribe(processor, subscription);
				}
			}
		},

		REQUESTED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				if (processor.isDataEmpty(data)) {
					Assert.state(processor.subscription != null, "No subscription");
					processor.subscription.request(1);
				}
				else {
					processor.dataReceived(data);
					processor.changeStateToReceived(this);
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.changeStateToComplete(this);
			}
		},

		RECEIVED {
			@SuppressWarnings("deprecation")
			@Override
			public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
				if (processor.changeState(this, WRITING)) {
					T data = processor.currentData;
					Assert.state(data != null, "No data");
					try {
						if (processor.write(data)) {
							if (processor.changeState(WRITING, REQUESTED)) {
								processor.currentData = null;
								if (processor.subscriberCompleted) {
									processor.changeStateToComplete(REQUESTED);
								}
								else {
									processor.writingPaused();
									Assert.state(processor.subscription != null, "No subscription");
									processor.subscription.request(1);
								}
							}
						}
						else {
							processor.changeStateToReceived(WRITING);
						}
					}
					catch (IOException ex) {
						processor.writingFailed(ex);
					}
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		WRITING {
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		COMPLETED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				// ignore
			}
			@Override
			public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
				// ignore
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// ignore
			}
		};

		public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
			subscription.cancel();
		}

		public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
			throw new IllegalStateException(toString());
		}

		public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
			if (processor.changeState(this, COMPLETED)) {
				processor.writingComplete();
				processor.resultPublisher.publishError(ex);
			}
			else {
				processor.state.get().onError(processor, ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
			// ignore
		}
	}

}
