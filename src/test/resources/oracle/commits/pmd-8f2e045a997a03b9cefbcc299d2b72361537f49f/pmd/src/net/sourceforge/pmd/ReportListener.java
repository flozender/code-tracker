package net.sourceforge.pmd;

import net.sourceforge.pmd.stat.Metric;

public interface ReportListener {
    void ruleViolationAdded(RuleViolation ruleViolation);

    void metricAdded(Metric metric);
}
