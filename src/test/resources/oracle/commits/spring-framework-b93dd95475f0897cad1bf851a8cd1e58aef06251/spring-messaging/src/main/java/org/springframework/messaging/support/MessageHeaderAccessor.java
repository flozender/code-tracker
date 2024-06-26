/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * A base for classes providing strongly typed getters and setters as well as
 * behavior around specific categories of headers (e.g. STOMP headers).
 * Supports creating new headers, modifying existing headers (when still mutable),
 * or copying and modifying existing headers.
 *
 * <p>The method {@link #getMessageHeaders()} provides access to the underlying,
 * fully-prepared {@link MessageHeaders} that can then be used as-is (i.e.
 * without copying) to create a single message as follows:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * Message message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
 * </pre>
 *
 * <p>After the above, by default the {@code MessageHeaderAccessor} becomes
 * immutable. However it is possible to leave it mutable for further initialization
 * in the same thread, for example:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * accessor.setHeader("foo", "bar");
 * accessor.setLeaveMutable(true);
 * Message message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
 *
 * // later on in the same thread...
 *
 * MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
 * accessor.setHeader("bar", "baz");
 * accessor.setImmutable();
 * </pre>
 *
 * <p>The method {@link #toMap()} returns a copy of the underlying headers. It can
 * be used to prepare multiple messages from the same {@code MessageHeaderAccessor}
 * instance:
 * <pre class="code">
 * MessageHeaderAccessor accessor = new MessageHeaderAccessor();
 * MessageBuilder builder = MessageBuilder.withPayload("payload").setHeaders(accessor);
 *
 * accessor.setHeader("foo", "bar1");
 * Message message1 = builder.build();
 *
 * accessor.setHeader("foo", "bar2");
 * Message message2 = builder.build();
 *
 * accessor.setHeader("foo", "bar3");
 * Message  message3 = builder.build();
 * </pre>
 *
 * <p>However note that with the above style, the header accessor is shared and
 * cannot be re-obtained later on. Alternatively it is also possible to create
 * one {@code MessageHeaderAccessor} per message:
 *
 * <pre class="code">
 * MessageHeaderAccessor accessor1 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar1");
 * Message message1 = MessageBuilder.createMessage("payload", accessor1.getMessageHeaders());
 *
 * MessageHeaderAccessor accessor2 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar2");
 * Message message2 = MessageBuilder.createMessage("payload", accessor2.getMessageHeaders());
 *
 * MessageHeaderAccessor accessor3 = new MessageHeaderAccessor();
 * accessor.set("foo", "bar3");
 * Message message3 = MessageBuilder.createMessage("payload", accessor3.getMessageHeaders());
 * </pre>
 *
 * <p>Note that the above examples aim to demonstrate the general idea of using
 * header accessors. The most likely usage however is through sub-classes.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MessageHeaderAccessor {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final MimeType[] READABLE_MIME_TYPES = new MimeType[] {
			MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.APPLICATION_XML,
			new MimeType("text", "*"), new MimeType("application", "*+json"), new MimeType("application", "*+xml")
	};


	protected final Log logger = LogFactory.getLog(getClass());

	private final MutableMessageHeaders headers;

	private boolean leaveMutable = false;

	private boolean modified = false;

	private boolean enableTimestamp = false;

	private IdGenerator idGenerator;


	/**
	 * A constructor to create new headers.
	 */
	public MessageHeaderAccessor() {
		this.headers = new MutableMessageHeaders();
	}

	/**
	 * A constructor accepting the headers of an existing message to copy.
	 */
	public MessageHeaderAccessor(Message<?> message) {
		if (message != null) {
			this.headers = new MutableMessageHeaders(message.getHeaders());
		}
		else {
			this.headers = new MutableMessageHeaders();
		}
	}


	/**
	 * Build a 'nested' accessor for the given message.
	 * @param message the message to build a new accessor for
	 * @return the nested accessor (typically a specific subclass)
	 */
	protected MessageHeaderAccessor createAccessor(Message<?> message) {
		return new MessageHeaderAccessor(message);
	}


	// Configuration properties

	/**
	 * By default when {@link #getMessageHeaders()} is called, {@code "this"}
	 * {@code MessageHeaderAccessor} instance can no longer be used to modify the
	 * underlying message headers and the returned {@code MessageHeaders} is immutable.
	 * <p>However when this is set to {@code true}, the returned (underlying)
	 * {@code MessageHeaders} instance remains mutable. To make further modifications
	 * continue to use the same accessor instance or re-obtain it via:<br>
	 * {@link MessageHeaderAccessor#getAccessor(Message, Class)
	 * MessageHeaderAccessor.getAccessor(Message, Class)}
	 * <p>When modifications are complete use {@link #setImmutable()} to prevent
	 * further changes. The intended use case for this mechanism is initialization
	 * of a Message within a single thread.
	 * <p>By default this is set to {@code false}.
	 * @since 4.1
	 */
	public void setLeaveMutable(boolean leaveMutable) {
		Assert.state(this.headers.isMutable(), "Already immutable");
		this.leaveMutable = leaveMutable;
	}

	/**
	 * By default when {@link #getMessageHeaders()} is called, {@code "this"}
	 * {@code MessageHeaderAccessor} instance can no longer be used to modify the
	 * underlying message headers. However if {@link #setLeaveMutable(boolean)}
	 * is used, this method is necessary to indicate explicitly when the
	 * {@code MessageHeaders} instance should no longer be modified.
	 * @since 4.1
	 */
	public void setImmutable() {
		this.headers.setIdAndTimestamp();
		this.headers.setImmutable();
	}

	/**
	 * Whether the underlying headers can still be modified.
	 * @since 4.1
	 */
	public boolean isMutable() {
		return this.headers.isMutable();
	}

	/**
	 * Mark the underlying message headers as modified.
	 * @param modified typically {@code true}, or {@code false} to reset the flag
	 * @since 4.1
	 */
	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * Check whether the underlying message headers have been marked as modified.
	 * @return {@code true} if the flag has been set, {@code false} otherwise
	 */
	public boolean isModified() {
		return this.modified;
	}

	/**
	 * A package private mechanism to enables the automatic addition of the
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP} header.
	 * <p>By default, this property is set to {@code false}.
	 * @see IdTimestampMessageHeaderInitializer
	 */
	void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * A package-private mechanism to configure the IdGenerator strategy to use.
	 * <p>By default this property is not set in which case the default IdGenerator
	 * in {@link org.springframework.messaging.MessageHeaders} is used.
	 * @see IdTimestampMessageHeaderInitializer
	 */
	void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}


	// Accessors for the resulting MessageHeaders

	/**
	 * Return the underlying {@code MessageHeaders} instance.
	 * <p>Unless {@link #setLeaveMutable(boolean)} was set to {@code true}, after
	 * this call, the headers are immutable and this accessor can no longer
	 * modify them.
	 * <p>This method always returns the same {@code MessageHeaders} instance if
	 * invoked multiples times. To obtain a copy of the underlying headers, use
	 * {@link #toMessageHeaders()} or {@link #toMap()} instead.
	 * @since 4.1
	 */
	public MessageHeaders getMessageHeaders() {
		if (!this.leaveMutable) {
			setImmutable();
		}
		return this.headers;
	}

	/**
	 * Return a copy of the underlying header values as a {@link MessageHeaders} object.
	 * <p>This method can be invoked many times, with modifications in between
	 * where each new call returns a fresh copy of the current header values.
	 * @since 4.1
	 */
	public MessageHeaders toMessageHeaders() {
		return new MessageHeaders(this.headers);
	}

	/**
	 * Return a copy of the underlying header values as a plain {@link Map} object.
	 * <p>This method can be invoked many times, with modifications in between
	 * where each new call returns a fresh copy of the current header values.
	 */
	public Map<String, Object> toMap() {
		return new HashMap<String, Object>(this.headers);
	}


	// Generic header accessors

	/**
	 * Retrieve the value for the header with the given name.
	 * @param headerName the name of the header
	 * @return the associated value, or {@code null} if none found
	 */
	public Object getHeader(String headerName) {
		return this.headers.get(headerName);
	}

	/**
	 * Set the value for the given header name.
	 * <p>If the provided value is {@code null}, the header will be removed.
	 */
	public void setHeader(String name, Object value) {
		if (isReadOnly(name)) {
			throw new IllegalArgumentException("'" + name + "' header is read-only");
		}
		verifyType(name, value);
		if (!ObjectUtils.nullSafeEquals(value, getHeader(name))) {
			this.modified = true;
			if (value != null) {
				this.headers.getRawHeaders().put(name, value);
			}
			else {
				this.headers.getRawHeaders().remove(name);
			}
		}
	}

	protected void verifyType(String headerName, Object headerValue) {
		if (headerName != null && headerValue != null) {
			if (MessageHeaders.ERROR_CHANNEL.equals(headerName) || MessageHeaders.REPLY_CHANNEL.endsWith(headerName)) {
				if (!(headerValue instanceof MessageChannel || headerValue instanceof String)) {
					throw new IllegalArgumentException(
							"'" + headerName + "' header value must be a MessageChannel or String");
				}
			}
		}
	}

	/**
	 * Set the value for the given header name only if the header name is not
	 * already associated with a value.
	 */
	public void setHeaderIfAbsent(String name, Object value) {
		if (getHeader(name) == null) {
			setHeader(name, value);
		}
	}

	/**
	 * Remove the value for the given header name.
	 */
	public void removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName) && !isReadOnly(headerName)) {
			setHeader(headerName, null);
		}
	}

	/**
	 * Removes all headers provided via array of 'headerPatterns'.
	 * <p>As the name suggests, array may contain simple matching patterns for header
	 * names. Supported pattern styles are: "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 */
	public void removeHeaders(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<String>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)){
				if (pattern.contains("*")){
					headersToRemove.addAll(getMatchingHeaderNames(pattern, this.headers));
				}
				else {
					headersToRemove.add(pattern);
				}
			}
		}
		for (String headerToRemove : headersToRemove) {
			removeHeader(headerToRemove);
		}
	}

	private List<String> getMatchingHeaderNames(String pattern, Map<String, Object> headers) {
		List<String> matchingHeaderNames = new ArrayList<String>();
		if (headers != null) {
			for (Map.Entry<String, Object> header: headers.entrySet()) {
				if (PatternMatchUtils.simpleMatch(pattern,  header.getKey())) {
					matchingHeaderNames.add(header.getKey());
				}
			}
		}
		return matchingHeaderNames;
	}

	/**
	 * Copy the name-value pairs from the provided Map.
	 * <p>This operation will overwrite any existing values. Use
	 * {@link #copyHeadersIfAbsent(Map)} to avoid overwriting values.
	 */
	public void copyHeaders(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			Set<String> keys = headersToCopy.keySet();
			for (String key : keys) {
				if (!isReadOnly(key)) {
					setHeader(key, headersToCopy.get(key));
				}
			}
		}
	}

	/**
	 * Copy the name-value pairs from the provided Map.
	 * <p>This operation will <em>not</em> overwrite any existing values.
	 */
	public void copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			Set<String> keys = headersToCopy.keySet();
			for (String key : keys) {
				if (!isReadOnly(key)) {
					setHeaderIfAbsent(key, headersToCopy.get(key));
				}
			}
		}
	}

	protected boolean isReadOnly(String headerName) {
		return (MessageHeaders.ID.equals(headerName) || MessageHeaders.TIMESTAMP.equals(headerName));
	}


	// Specific header accessors

	public UUID getId() {
		return (UUID) getHeader(MessageHeaders.ID);
	}

	public Long getTimestamp() {
		return (Long) getHeader(MessageHeaders.TIMESTAMP);
	}

	public void setReplyChannelName(String replyChannelName) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	public Object getReplyChannel() {
        return getHeader(MessageHeaders.REPLY_CHANNEL);
    }

	public void setErrorChannelName(String errorChannelName) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

    public Object getErrorChannel() {
        return getHeader(MessageHeaders.ERROR_CHANNEL);
    }

	public void setContentType(MimeType contentType) {
		setHeader(MessageHeaders.CONTENT_TYPE, contentType);
	}

	public MimeType getContentType() {
		return (MimeType) getHeader(MessageHeaders.CONTENT_TYPE);
	}


	// Log message stuff

	/**
	 * Return a concise message for logging purposes.
	 * @param payload the payload that corresponds to the headers.
	 * @return the message
	 */
	public String getShortLogMessage(Object payload) {
		return "headers=" + this.headers.toString() + getShortPayloadLogMessage(payload);
	}

	/**
	 * Return a more detailed message for logging purposes.
	 * @param payload the payload that corresponds to the headers.
	 * @return the message
	 */
	public String getDetailedLogMessage(Object payload) {
		return "headers=" + this.headers.toString() + getDetailedPayloadLogMessage(payload);
	}

	protected String getShortPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			String payloadText = (String) payload;
			return (payloadText.length() < 80) ?
				" payload=" + payloadText :
				" payload=" + payloadText.substring(0, 80) + "...(truncated)";
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				Charset charset = getContentType().getCharSet();
				charset = (charset != null ? charset : DEFAULT_CHARSET);
				return (bytes.length < 80) ?
						" payload=" + new String(bytes, charset) :
						" payload=" + new String(Arrays.copyOf(bytes, 80), charset) + "...(truncated)";
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			String payloadText = payload.toString();
			return (payloadText.length() < 80) ?
					" payload=" + payloadText :
					" payload=" + ObjectUtils.identityToString(payload);
		}
	}

	protected String getDetailedPayloadLogMessage(Object payload) {
		if (payload instanceof String) {
			return " payload=" + ((String) payload);
		}
		else if (payload instanceof byte[]) {
			byte[] bytes = (byte[]) payload;
			if (isReadableContentType()) {
				Charset charset = getContentType().getCharSet();
				charset = (charset != null ? charset : DEFAULT_CHARSET);
				return " payload=" + new String(bytes, charset);
			}
			else {
				return " payload=byte[" + bytes.length + "]";
			}
		}
		else {
			return " payload=" + payload;
		}
	}

	protected boolean isReadableContentType() {
		for (MimeType mimeType : READABLE_MIME_TYPES) {
			if (mimeType.includes(getContentType())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[headers=" + this.headers + "]";
	}


	// Static factory methods

	/**
	 * Return the original {@code MessageHeaderAccessor} used to create the headers
	 * of the given {@code Message}, or {@code null} if that's not available or if
	 * its type does not match the required type.
	 * <p>This is for cases where the existence of an accessor is strongly expected
	 * (to be followed up with an assertion) or will created if not provided.
	 * @return an accessor instance of the specified type, or {@code null} if none
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static <T extends MessageHeaderAccessor> T getAccessor(Message<?> message, Class<T> requiredType) {
		return getAccessor(message.getHeaders(), requiredType);
	}

	/**
	 * A variation of {@link #getAccessor(org.springframework.messaging.Message, Class)}
	 * with a {@code MessageHeaders} instance instead of a {@code Message}.
	 * <p>This is for cases when a full message may not have been created yet.
	 * @return an accessor instance of the specified typem or {@code null} if none
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static <T extends MessageHeaderAccessor> T getAccessor(MessageHeaders messageHeaders, Class<T> requiredType) {
		if (messageHeaders instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) messageHeaders;
			MessageHeaderAccessor headerAccessor = mutableHeaders.getMessageHeaderAccessor();
			if (requiredType.isAssignableFrom(headerAccessor.getClass()))  {
				return (T) headerAccessor;
			}
		}
		return null;
	}

	/**
	 * Return a mutable {@code MessageHeaderAccessor} for the given message attempting
	 * to match the type of accessor used to create the message headers, or otherwise
	 * wrapping the message with a {@code MessageHeaderAccessor} instance.
	 * <p>This is for cases where a header needs to be updated in generic code
	 * while preserving the accessor type for downstream processing.
	 * @return an accessor of the required type, never {@code null}.
	 * @since 4.1
	 */
	public static MessageHeaderAccessor getMutableAccessor(Message<?> message) {
		if (message.getHeaders() instanceof MutableMessageHeaders) {
			MutableMessageHeaders mutableHeaders = (MutableMessageHeaders) message.getHeaders();
			MessageHeaderAccessor accessor = mutableHeaders.getMessageHeaderAccessor();
			if (accessor != null) {
				return (accessor.isMutable() ? accessor : accessor.createAccessor(message));
			}
		}
		return new MessageHeaderAccessor(message);
	}


	@SuppressWarnings("serial")
	private class MutableMessageHeaders extends MessageHeaders {

		private boolean immutable;

		public MutableMessageHeaders() {
			this(null);
		}

		public MutableMessageHeaders(Map<String, Object> headers) {
			super(headers, MessageHeaders.ID_VALUE_NONE, -1L);
		}

		public MessageHeaderAccessor getMessageHeaderAccessor() {
			return MessageHeaderAccessor.this;
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			Assert.state(!this.immutable, "Already immutable");
			return super.getRawHeaders();
		}

		public void setImmutable() {
			this.immutable = true;
		}

		public boolean isMutable() {
			return !this.immutable;
		}

		public void setIdAndTimestamp() {
			if (!isMutable()) {
				return;
			}
			if (getId() == null) {
				IdGenerator idGenerator = (MessageHeaderAccessor.this.idGenerator != null ?
						MessageHeaderAccessor.this.idGenerator : MessageHeaders.getIdGenerator());

				UUID id = idGenerator.generateId();
				if (id != null && id != MessageHeaders.ID_VALUE_NONE) {
					getRawHeaders().put(ID, id);
				}
			}
			if (getTimestamp() == null) {
				if (MessageHeaderAccessor.this.enableTimestamp) {
					getRawHeaders().put(TIMESTAMP, System.currentTimeMillis());
				}
			}
		}
	}

}
