/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.servlet.view.jasperreports;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import net.sf.jasperreports.engine.design.JRCompiler;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver} implementation that
 * resolves instances of {@link AbstractJasperReportsView} by translating
 * the supplied view name into the URL of the report file.
 * 
 * @author Rob Harrop
 * @since 1.2.6
 */
public class JasperReportsViewResolver extends UrlBasedViewResolver {

	private String reportDataKey;

	private Properties subReportUrls;

	private String[] subReportDataKeys;

	private Properties headers;

	private Map exporterParameters = new HashMap();

	private DataSource jdbcDataSource;

	private JRCompiler reportCompiler;


	/**
	 * Requires the view class to be a subclass of {@link AbstractJasperReportsView}.
	 */
	@Override
	protected Class requiredViewClass() {
		return AbstractJasperReportsView.class;
	}

	/**
	 * Set the <code>reportDataKey</code> the view class should use.
	 * @see AbstractJasperReportsView#setReportDataKey
	 */
	public void setReportDataKey(String reportDataKey) {
		this.reportDataKey = reportDataKey;
	}

	/**
	 * Set the <code>subReportUrls</code> the view class should use.
	 * @see AbstractJasperReportsView#setSubReportUrls
	 */
	public void setSubReportUrls(Properties subReportUrls) {
		this.subReportUrls = subReportUrls;
	}

	/**
	 * Set the <code>subReportDataKeys</code> the view class should use.
	 * @see AbstractJasperReportsView#setSubReportDataKeys
	 */
	public void setSubReportDataKeys(String[] subReportDataKeys) {
		this.subReportDataKeys = subReportDataKeys;
	}

	/**
	 * Set the <code>headers</code> the view class should use.
	 * @see AbstractJasperReportsView#setHeaders
	 */
	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * Set the <code>exporterParameters</code> the view class should use.
	 * @see AbstractJasperReportsView#setExporterParameters
	 */
	public void setExporterParameters(Map exporterParameters) {
		this.exporterParameters = exporterParameters;
	}

	/**
	 * Set the {@link DataSource} the view class should use.
	 * @see AbstractJasperReportsView#setJdbcDataSource
	 */
	public void setJdbcDataSource(DataSource jdbcDataSource) {
		this.jdbcDataSource = jdbcDataSource;
	}

	/**
	 * Set the {@link JRCompiler} the view class should use.
	 * @see AbstractJasperReportsView#setReportCompiler
	 */
	public void setReportCompiler(JRCompiler reportCompiler) {
		this.reportCompiler = reportCompiler;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractJasperReportsView view = (AbstractJasperReportsView) super.buildView(viewName);

		view.setReportDataKey(this.reportDataKey);
		view.setSubReportUrls(this.subReportUrls);
		view.setSubReportDataKeys(this.subReportDataKeys);
		view.setHeaders(this.headers);
		view.setExporterParameters(this.exporterParameters);
		view.setJdbcDataSource(this.jdbcDataSource);

		if (this.reportCompiler != null) {
			view.setReportCompiler(this.reportCompiler);
		}

		return view;
	}

}
