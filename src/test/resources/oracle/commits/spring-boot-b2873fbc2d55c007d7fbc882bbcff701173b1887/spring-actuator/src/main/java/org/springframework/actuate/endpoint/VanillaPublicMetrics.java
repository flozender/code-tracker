/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.actuate.endpoint;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.actuate.metrics.Metric;
import org.springframework.actuate.metrics.MetricRepository;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PublicMetrics} that exposes all metrics from the
 * {@link MetricRepository} along with memory information.
 * 
 * @author Dave Syer
 */
public class VanillaPublicMetrics implements PublicMetrics {

	private MetricRepository metricRepository;

	public VanillaPublicMetrics(MetricRepository metricRepository) {
		Assert.notNull(metricRepository, "MetricRepository must not be null");
		this.metricRepository = metricRepository;
	}

	@Override
	public Collection<Metric> metrics() {
		Collection<Metric> result = new LinkedHashSet<Metric>(
				this.metricRepository.findAll());
		result.add(new Metric("mem", new Long(Runtime.getRuntime().totalMemory()) / 1024));
		result.add(new Metric("mem.free",
				new Long(Runtime.getRuntime().freeMemory()) / 1024));
		result.add(new Metric("processors", Runtime.getRuntime().availableProcessors()));
		return result;
	}

}
