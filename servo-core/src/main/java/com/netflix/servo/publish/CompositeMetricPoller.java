/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
 * %%
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
 * #L%
 */
package com.netflix.servo.publish;

import static com.netflix.servo.annotations.DataSourceType.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.netflix.servo.BasicTag;
import com.netflix.servo.BasicTagList;
import com.netflix.servo.TagList;
import com.netflix.servo.util.Counters;

import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combines results from a list of metric pollers. This clas
 */
public class CompositeMetricPoller implements MetricPoller {

    private static final List<Metric> EMPTY = ImmutableList.of();

    private static final String POLLER_KEY = "PollerName";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String,MetricPoller> pollers;
    private final ExecutorService executor;
    private final long timeout;

    /**
     * Creates a new instance for a set of pollers.
     *
     * @param pollers   a set of pollers to collect data from, the map key for a
     *                  poller is used as a name identify a particular poller
     *                  for logging and error messages
     * @param executor  an executor to use for executing the poll methods
     * @param timeout   timeout in milliseconds used when getting the value
     *                  from the future
     */
    public CompositeMetricPoller(
            Map<String,MetricPoller> pollers,
            ExecutorService executor,
            long timeout) {
        this.pollers = ImmutableMap.copyOf(pollers);
        this.executor = executor;
        this.timeout = timeout;
    }

    private void increment(Throwable t, String name) {
        TagList tags = BasicTagList.copyOf(new BasicTag(POLLER_KEY, name));
        Counters.increment(t.getClass().getSimpleName() + "Count", tags);
    }

    private List<Metric> getMetrics(String name, Future<List<Metric>> future) {
        List<Metric> metrics = EMPTY;
        try {
            metrics = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            increment(e, name);
            logger.warn("uncaught exception from poll method for {}", e, name);
        } catch (TimeoutException e) {
            increment(e, name);
            logger.warn("timeout executing poll method for {}", e, name);
        } catch (InterruptedException e) {
            increment(e, name);
            logger.warn("interrupted while doing get for {}", e, name);
        }
        return metrics;
    }

    public List<Metric> poll(MetricFilter filter) {
        Map<String,Future<List<Metric>>> futures = Maps.newHashMap();
        for (Map.Entry<String,MetricPoller> e : pollers.entrySet()) {
            PollCallable task = new PollCallable(e.getValue(), filter);
            futures.put(e.getKey(), executor.submit(task));
        }

        List<Metric> allMetrics = Lists.newArrayList();
        for (Map.Entry<String,Future<List<Metric>>> e : futures.entrySet()) {
            allMetrics.addAll(getMetrics(e.getKey(), e.getValue()));
        }
        return allMetrics;
    }
}
