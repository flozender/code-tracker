/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.matchers.text;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.ContainsExtraTypeInfo;
import org.mockito.internal.reporting.PrintSettings;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unchecked")
public class MatchersPrinter {

    public String getArgumentsLine(List<ArgumentMatcher> matchers, PrintSettings printSettings) {
        ValuePrinter printer = new ValuePrinter();
        printer.appendList("(", ", ", ");", applyPrintSettings(matchers, printSettings));
        return printer.toString();
    }

    public String getArgumentsBlock(List<ArgumentMatcher> matchers, PrintSettings printSettings) {
        ValuePrinter printer = new ValuePrinter();
        printer.appendList("(\n    ", ",\n    ", "\n);", applyPrintSettings(matchers, printSettings));
        return printer.toString();
    }

    private Iterator applyPrintSettings(List<ArgumentMatcher> matchers, PrintSettings printSettings) {
        List out = new LinkedList();
        int i = 0;
        for (final ArgumentMatcher matcher : matchers) {
            if (matcher instanceof ContainsExtraTypeInfo && printSettings.extraTypeInfoFor(i)) {
                out.add(new FormattedText(((ContainsExtraTypeInfo) matcher).toStringWithType()));
            } else {
                out.add(new FormattedText(MatcherToString.toString(matcher)));
            }
            i++;
        }
        return out.iterator();
    }
}
