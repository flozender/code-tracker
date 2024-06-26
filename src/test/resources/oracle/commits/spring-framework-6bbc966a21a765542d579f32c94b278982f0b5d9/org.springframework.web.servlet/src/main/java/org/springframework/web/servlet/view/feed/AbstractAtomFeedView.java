/*
 * Copyright 2008 the original author or authors.
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

package org.springframework.web.servlet.view.feed;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

/**
 * Abstract superclass for Atom Feed views, using java.net's <a href="https://rome.dev.java.net/">ROME</a> package.
 * Application-specific view classes will extend this class. The view will be held in the subclass itself, not in a
 * template.
 *
 * <p/>Main entry points are the {@link #buildFeedMetadata(Map, WireFeed, HttpServletRequest)} and
 * {@link #buildFeedEntries(Map, HttpServletRequest, HttpServletResponse)}.
 *
 * @author Jettro Coenradie
 * @author Sergio Bossa
 * @author Arjen Poutsma
 * @since 3.0
 * @see #buildFeedMetadata(Map, WireFeed, HttpServletRequest)
 * @see #buildFeedEntries(Map, HttpServletRequest, HttpServletResponse)
 */
public abstract class AbstractAtomFeedView extends AbstractFeedView<Feed> {

	public static final String DEFAULT_FEED_TYPE = "atom_1.0";

	private String feedType = DEFAULT_FEED_TYPE;

	/** Sets the appropriate content type: "application/atom+xml". */
	public AbstractAtomFeedView() {
		setContentType("application/atom+xml");
	}

	/**
	 * Sets the Rome feed type to use.
	 * <p/>
	 * Defaults to Atom 1.0.
	 *
	 * @see Feed#setFeedType(String)
	 * @see #DEFAULT_FEED_TYPE
	 */
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}

	/**
	 * Create a new feed to hold the entries.
	 * <p/>
	 * By default returns an Atom 1.0 feed, but the subclass can specify any Feed.
	 *
	 * @return the newly created Feed instance
	 * @see #setFeedType(String)
	 * @see com.sun.syndication.feed.atom.Feed#Feed(String)
	 */
	@Override
	protected Feed newFeed() {
		return new Feed(feedType);
	}

	/** Invokes {@link #buildFeedEntries(Map, HttpServletRequest, HttpServletResponse)} to get a list of feed entries. */
	@Override
	protected final void buildFeedEntries(Map model,
										  Feed feed,
										  HttpServletRequest request,
										  HttpServletResponse response) throws Exception {
		List<Entry> entries = buildFeedEntries(model, request, response);
		feed.setEntries(entries);
	}

	/**
	 * Subclasses must implement this method to build feed entries, given the model.
	 * <p/>
	 * Note that the passed-in HTTP response is just supposed to be used for setting cookies or other HTTP headers. The
	 * built feed itself will automatically get written to the response after this method returns.
	 *
	 * @param model	the model Map
	 * @param request  in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 * @return the feed entries to be added to the feed
	 * @throws Exception any exception that occured during document building
	 * @see Entry
	 */
	protected abstract List<Entry> buildFeedEntries(Map model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;
}
