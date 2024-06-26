package net.sourceforge.pmd.lang.vm;

import net.sourceforge.pmd.lang.BaseLanguageModule;
import net.sourceforge.pmd.lang.vm.rule.VmRuleChainVisitor;

/**
 * Created by christoferdutz on 20.09.14.
 */
public class VmLanguageModule extends BaseLanguageModule {

    public static final String NAME = "VM";

    public VmLanguageModule() {
        super(NAME, null, "vm", VmRuleChainVisitor.class, "vm");
        addVersion("", new VmHandler(), true);
    }

}
