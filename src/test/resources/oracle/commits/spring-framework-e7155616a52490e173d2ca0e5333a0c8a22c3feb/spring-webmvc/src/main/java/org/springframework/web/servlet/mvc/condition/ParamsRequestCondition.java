/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

/**
 * A logical conjunction (' && ') request condition that matches a request against
 * a set parameter expressions with syntax defined in {@link RequestMapping#params()}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

	private final Set<ParamExpression> expressions;

	/**
	 * Create a new instance from the given param expressions.
	 * @param params expressions with syntax defined in {@link RequestMapping#params()};
	 * 	if 0, the condition will match to every request.
	 */
	public ParamsRequestCondition(String... params) {
		this(parseExpressions(params));
	}

	private ParamsRequestCondition(Collection<ParamExpression> conditions) {
		this.expressions = Collections.unmodifiableSet(new LinkedHashSet<ParamExpression>(conditions));
	}

	private static Collection<ParamExpression> parseExpressions(String... params) {
		Set<ParamExpression> expressions = new LinkedHashSet<ParamExpression>();
		if (params != null) {
			for (String param : params) {
				expressions.add(new ParamExpression(param));
			}
		}
		return expressions;
	}

	/**
	 * Return the contained request parameter expressions.
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<NameValueExpression<String>>(this.expressions);
	}

	@Override
	protected Collection<ParamExpression> getContent() {
		return expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * Returns a new instance with the union of the param expressions
	 * from "this" and the "other" instance.
	 */
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		Set<ParamExpression> set = new LinkedHashSet<ParamExpression>(this.expressions);
		set.addAll(other.expressions);
		return new ParamsRequestCondition(set);
	}

	/**
	 * Returns "this" instance if the request matches all param expressions;
	 * or {@code null} otherwise.
	 */
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		for (ParamExpression expression : expressions) {
			if (!expression.match(request)) {
				return null;
			}
		}
		return this;
	}

	/**
	 * Returns:
	 * <ul>
	 * 	<li>0 if the two conditions have the same number of parameter expressions
	 * 	<li>Less than 0 if "this" instance has more parameter expressions
	 * 	<li>Greater than 0 if the "other" instance has more parameter expressions
	 * </ul>
	 *
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(HttpServletRequest)} and each instance
	 * contains the matching parameter expressions only or is otherwise empty.
	 */
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
		return other.expressions.size() - this.expressions.size();
	}

	/**
	 * Parses and matches a single param expression to a request.
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		ParamExpression(String expression) {
			super(expression);
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return WebUtils.hasSubmitParameter(request, name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return value.equals(request.getParameter(name));
		}
	}

}
