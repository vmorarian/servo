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

import com.netflix.servo.Tag;
import com.netflix.servo.TagList;

import java.util.regex.Pattern;

public final class RegexMetricFilter implements MetricFilter {

    private final String tagKey;
    private final Pattern pattern;
    private final boolean matchIfMissingTag;
    private final boolean invert;

    public RegexMetricFilter(
            String tagKey,
            Pattern pattern,
            boolean matchIfMissingTag,
            boolean invert) {
        this.tagKey = tagKey;
        this.pattern = pattern;
        this.matchIfMissingTag = matchIfMissingTag;
        this.invert = invert;
    }

    /** {@inheritDoc} */
    public boolean matches(String name, TagList tags) {
        String value = null;
        if (tagKey == null) {
            value = name;
        } else {
            Tag t = tags.getTag(tagKey);
            value = (t == null) ? null : t.getValue();
        }

        boolean match = matchIfMissingTag;
        if (value != null) {
            match = pattern.matcher(value).matches();
        }
        return match ^ invert;
    }
}
