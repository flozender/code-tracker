package net.sourceforge.pmd.lang.ecmascript;

import net.sourceforge.pmd.lang.BaseLanguageModule;
import net.sourceforge.pmd.lang.ecmascript.rule.EcmascriptRuleChainVisitor;

/**
 * Created by christoferdutz on 20.09.14.
 */
public class EcmascriptLanguageModule extends BaseLanguageModule {

    public static final String NAME = "Ecmascript";

    public EcmascriptLanguageModule() {
        super(NAME, null, "ecmascript", EcmascriptRuleChainVisitor.class, "js");
        addVersion("3", new Ecmascript3Handler(), true);
    }

}
