// ========================================================================
// $Id$
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================
package org.eclipse.jetty.rewrite.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.server.handler.HandlerWrapper;

/* ------------------------------------------------------------ */
/**
 *<p> Rewrite handler is responsible for managing the rules. Its capabilities
 * is not only limited for url rewrites such as RewritePatternRule or RewriteRegexRule. 
 * There is also handling for cookies, headers, redirection, setting status or error codes 
 * whenever the rule finds a match. 
 * 
 * <p> The rules can be matched by the ff. options: pattern matching of PathMap 
 * (class PatternRule), regular expressions (class RegexRule) or certain conditions set 
 * (e.g. MsieSslRule - the requests must be in SSL mode).
 * 
 * Here are the list of rules:
 * <ul>
 * <li> CookiePatternRule - adds a new cookie in response. </li>
 * <li> HeaderPatternRule - adds/modifies the HTTP headers in response. </li>
 * <li> RedirectPatternRule - sets the redirect location. </li>
 * <li> ResponsePatternRule - sets the status/error codes. </li>
 * <li> RewritePatternRule - rewrites the requested URI. </li>
 * <li> RewriteRegexRule - rewrites the requested URI using regular expression for pattern matching. </li>
 * <li> MsieSslRule - disables the keep alive on SSL for IE5 and IE6. </li>
 * <li> LegacyRule - the old version of rewrite. </li>
 * <li> ForwardedSchemeHeaderRule - set the scheme according to the headers present. </li>
 * </ul>
 *
 * <p> The rules can be grouped into rule containers (class RuleContainerRule), and will only 
 * be applied if the request matches the conditions for their container
 * (e.g., by virtual host name)
 *
 * Here are a list of rule containers:
 * <ul>
 * <li> VirtualHostRuleContainerRule - checks whether the request matches one of a set of virtual host names.</li>
 * </ul>
 * 
 * Here is a typical jetty.xml configuration would be: <pre>
 * 
 *   &lt;Set name="handler"&gt;
 *     &lt;New id="Handlers" class="org.eclipse.jetty.rewrite.handler.RewriteHandler"&gt;
 *       &lt;Set name="rules"&gt;
 *         &lt;Array type="org.eclipse.jetty.rewrite.handler.Rule"&gt;
 *
 *           &lt;Item&gt; 
 *             &lt;New id="rewrite" class="org.eclipse.jetty.rewrite.handler.RewritePatternRule"&gt;
 *               &lt;Set name="pattern"&gt;/*&lt;/Set&gt;
 *               &lt;Set name="replacement"&gt;/test&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *
 *           &lt;Item&gt; 
 *             &lt;New id="response" class="org.eclipse.jetty.rewrite.handler.ResponsePatternRule"&gt;
 *               &lt;Set name="pattern"&gt;/session/&lt;/Set&gt;
 *               &lt;Set name="code"&gt;400&lt;/Set&gt;
 *               &lt;Set name="reason"&gt;Setting error code 400&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *
 *           &lt;Item&gt; 
 *             &lt;New id="header" class="org.eclipse.jetty.rewrite.handler.HeaderPatternRule"&gt;
 *               &lt;Set name="pattern"&gt;*.jsp&lt;/Set&gt;
 *               &lt;Set name="name"&gt;server&lt;/Set&gt;
 *               &lt;Set name="value"&gt;dexter webserver&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *
 *           &lt;Item&gt; 
 *             &lt;New id="header" class="org.eclipse.jetty.rewrite.handler.HeaderPatternRule"&gt;
 *               &lt;Set name="pattern"&gt;*.jsp&lt;/Set&gt;
 *               &lt;Set name="name"&gt;title&lt;/Set&gt;
 *               &lt;Set name="value"&gt;driven header purpose&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *
 *           &lt;Item&gt; 
 *             &lt;New id="redirect" class="org.eclipse.jetty.rewrite.handler.RedirectPatternRule"&gt;
 *               &lt;Set name="pattern"&gt;/test/dispatch&lt;/Set&gt;
 *               &lt;Set name="location"&gt;http://jetty.eclipse.org&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *
 *           &lt;Item&gt; 
 *             &lt;New id="regexRewrite" class="org.eclipse.jetty.rewrite.handler.RewriteRegexRule"&gt;
 *               &lt;Set name="regex"&gt;/test-jaas/$&lt;/Set&gt;
 *               &lt;Set name="replacement"&gt;/demo&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *           
 *           &lt;Item&gt; 
 *             &lt;New id="forwardedHttps" class="org.eclipse.jetty.rewrite.handler.ForwardedSchemeHeaderRule"&gt;
 *               &lt;Set name="header"&gt;X-Forwarded-Scheme&lt;/Set&gt;
 *               &lt;Set name="headerValue"&gt;https&lt;/Set&gt;
 *               &lt;Set name="scheme"&gt;https&lt;/Set&gt;
 *             &lt;/New&gt;
 *           &lt;/Item&gt;
 *           
 *           &lt;Item&gt;
 *             &lt;New id="virtualHost" class="org.eclipse.jetty.rewrite.handler.VirtualHostRuleContainer"&gt;
 *
 *               &lt;Set name="virtualHosts"&gt;
 *                 &lt;Array type="java.lang.String"&gt;
 *                   &lt;Item&gt;eclipse.com&lt;/Item&gt;
 *                   &lt;Item&gt;www.eclipse.com&lt;/Item&gt;
 *                   &lt;Item&gt;eclipse.org&lt;/Item&gt;
 *                   &lt;Item&gt;www.eclipse.org&lt;/Item&gt;
 *                 &lt;/Array&gt;
 *               &lt;/Set&gt;
 *
 *               &lt;Call name="addRule"&gt;
 *                 &lt;Arg&gt;
 *                   &lt;New class="org.eclipse.jetty.rewrite.handler.CookiePatternRule"&gt;
 *                     &lt;Set name="pattern"&gt;/*&lt;/Set&gt;
 *                     &lt;Set name="name"&gt;CookiePatternRule&lt;/Set&gt;
 *                     &lt;Set name="value"&gt;1&lt;/Set&gt;
 *                   &lt;/New&gt;
 *                 &lt;/Arg&gt;
 *               &lt;/Call&gt;
 *    
 *             &lt;/New&gt;
 *           &lt;/      Item&gt;
 * 
 *         &lt;/Array&gt;
 *       &lt;/Set&gt;
 *
 *       &lt;Set name="handler"&gt;
 *         &lt;New id="Handlers" class="org.eclipse.jetty.server.handler.HandlerCollection"&gt;
 *           &lt;Set name="handlers"&gt;
 *            &lt;Array type="org.eclipse.jetty.server.Handler"&gt;
 *              &lt;Item&gt;
 *                &lt;New id="Contexts" class="org.eclipse.jetty.server.handler.ContextHandlerCollection"/&gt;
 *              &lt;/Item&gt;
 *              &lt;Item&gt;
 *                &lt;New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/&gt;
 *              &lt;/Item&gt;
 *              &lt;Item&gt;
 *                &lt;New id="RequestLog" class="org.eclipse.jetty.server.handler.RequestLogHandler"/&gt;
 *              &lt;/Item&gt;
 *            &lt;/Array&gt;
 *           &lt;/Set&gt;
 *         &lt;/New&gt;
 *       &lt;/Set&gt;
 *
 *     &lt;/New&gt;
 *   &lt;/Set&gt;
 * </pre>
 * 
 */
public class RewriteHandler extends HandlerWrapper
{
    
    private RuleContainer _rules;
    
    /* ------------------------------------------------------------ */
    public RewriteHandler()
    {
        _rules = new RuleContainer();
    }

    /* ------------------------------------------------------------ */
    /**
     * To enable configuration from jetty.xml on rewriteRequestURI, rewritePathInfo and
     * originalPathAttribute
     * 
     * @param legacyRule old style rewrite rule
     */
    public void setLegacyRule(LegacyRule legacyRule)
    {
        _rules.setLegacyRule(legacyRule);
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the list of rules.
     * @return an array of {@link Rule}.
     */
    public Rule[] getRules()
    {
        return _rules.getRules();
    }

    /* ------------------------------------------------------------ */
    /**
     * Assigns the rules to process.
     * @param rules an array of {@link Rule}. 
     */
    public void setRules(Rule[] rules)
    {
        _rules.setRules(rules);
    }

    /*------------------------------------------------------------ */
    /**
     * Assigns the rules to process.
     * @param rules a {@link RuleContainer} containing other rules to process
     */
    public void setRules(RuleContainer rules)
    {
        _rules = rules;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add a Rule
     * @param rule The rule to add to the end of the rules array
     */
    public void addRule(Rule rule)
    {
        _rules.addRule(rule);
    }
   

    /* ------------------------------------------------------------ */
    /**
     * @return the rewriteRequestURI If true, this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public boolean isRewriteRequestURI()
    {
        return _rules.isRewriteRequestURI();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewriteRequestURI true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getRequestURI()}.
     */
    public void setRewriteRequestURI(boolean rewriteRequestURI)
    {
        _rules.setRewriteRequestURI(rewriteRequestURI);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public boolean isRewritePathInfo()
    {
        return _rules.isRewritePathInfo();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param rewritePathInfo true if this handler will rewrite the value
     * returned by {@link HttpServletRequest#getPathInfo()}.
     */
    public void setRewritePathInfo(boolean rewritePathInfo)
    {
        _rules.setRewritePathInfo(rewritePathInfo);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the originalPathAttribte. If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public String getOriginalPathAttribute()
    {
        return _rules.getOriginalPathAttribute();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param originalPathAttribte If non null, this string will be used
     * as the attribute name to store the original request path.
     */
    public void setOriginalPathAttribute(String originalPathAttribute)
    {
        _rules.setOriginalPathAttribute(originalPathAttribute);
    }


    /* ------------------------------------------------------------ */
    /**
     * @deprecated 
     */
    public PathMap getRewrite()
    {
        return _rules.getRewrite();
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void setRewrite(PathMap rewrite)
    {
        _rules.setRewrite(rewrite);
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void addRewriteRule(String pattern, String prefix)
    {
        _rules.addRewriteRule(pattern,prefix);
    }
    
    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.eclipse.jetty.server.handler.HandlerWrapper#handle(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        if (isStarted())
        { 
            String returned = _rules.matchAndApply(target, request, response);
            target = (returned == null) ? target : returned;
            
            if (!_rules.isHandled())
            {
                super.handle(target, request, response);
            }
        }
    }
    
}
