/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.orderbook.core.options;

/**
 * An enumeration of possible values for configuring the invalid quote mode.
 * Invalid quote is a quote that has a level more than available level, invalid price or invalid size,
 * invalid number of orders or unknown exchange.
 * <p>
 * Note: UnreachableDepthMode depends on max deep limit
 * Supported for L2
 *
 * @author Andrii_Ostapenko1
 * @see BindOrderBookOptionsBuilder#maxDepth(int)
 */
// TODO ADD UNIT TEST!!
public class ValidationOptions {

    public static final ValidationOptions ALL_ENABLED = ValidationOptions.builder()
            .validateQuoteInsert()
            .validateQuoteUpdate()
            .build();

    private boolean quoteUpdate = false;

    private boolean quoteInsert = false;

    public boolean isQuoteUpdate() {
        return quoteUpdate;
    }

    public boolean isQuoteInsert() {
        return quoteInsert;
    }

    public static ValidationOptionsBuilder builder() {
        return new ValidationOptionsBuilder();
    }

    public static class ValidationOptionsBuilder {
        private final ValidationOptions mode = new ValidationOptions();

        public ValidationOptionsBuilder skipInvalidQuoteInsert() {
            mode.quoteInsert = false;
            return this;
        }

        public ValidationOptionsBuilder skipInvalidQuoteUpdate() {
            mode.quoteUpdate = false;
            return this;
        }

        public ValidationOptionsBuilder validateQuoteInsert() {
            mode.quoteInsert = true;
            return this;
        }

        public ValidationOptionsBuilder validateQuoteUpdate() {
            mode.quoteUpdate = true;
            return this;
        }

        public ValidationOptions build() {
            return mode;
        }
    }
}
