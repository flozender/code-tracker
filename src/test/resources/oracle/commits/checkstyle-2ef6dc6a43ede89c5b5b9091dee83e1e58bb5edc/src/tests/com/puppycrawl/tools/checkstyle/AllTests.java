package com.puppycrawl.tools.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractViolationReporterTest;
import com.puppycrawl.tools.checkstyle.api.DetailASTTest;
import com.puppycrawl.tools.checkstyle.checks.AvoidInlineConditionalsCheckTest;
import com.puppycrawl.tools.checkstyle.checks.AvoidStarImportTest;
import com.puppycrawl.tools.checkstyle.checks.ArrayTrailingCommaCheckTest;
import com.puppycrawl.tools.checkstyle.checks.ConstantNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.DesignForExtensionCheckTest;
import com.puppycrawl.tools.checkstyle.checks.DoubleCheckedLockingCheckTest;
import com.puppycrawl.tools.checkstyle.checks.EmptyBlockCheckTest;
import com.puppycrawl.tools.checkstyle.checks.EmptyForIteratorPadCheckTest;
import com.puppycrawl.tools.checkstyle.checks.EmptyStatementCheckTest;
import com.puppycrawl.tools.checkstyle.checks.EqualsHashCodeCheckTest;
import com.puppycrawl.tools.checkstyle.checks.FileLengthCheckTest;
import com.puppycrawl.tools.checkstyle.checks.FileSetCheckLifecycleTest;
import com.puppycrawl.tools.checkstyle.checks.FinalClassCheckTest;
import com.puppycrawl.tools.checkstyle.checks.GenericIllegalRegexpCheckTest;
import com.puppycrawl.tools.checkstyle.checks.HeaderCheckTest;
import com.puppycrawl.tools.checkstyle.checks.HiddenFieldCheckTest;
import com.puppycrawl.tools.checkstyle.checks.IllegalImportCheckTest;
import com.puppycrawl.tools.checkstyle.checks.IllegalInstantiationCheckTest;
import com.puppycrawl.tools.checkstyle.checks.IndentationCheckTest;
import com.puppycrawl.tools.checkstyle.checks.InnerAssignmentCheckTest;
import com.puppycrawl.tools.checkstyle.checks.JavadocMethodCheckTest;
import com.puppycrawl.tools.checkstyle.checks.LeftCurlyCheckTest;
import com.puppycrawl.tools.checkstyle.checks.LineLengthCheckTest;
import com.puppycrawl.tools.checkstyle.checks.LocalFinalVariableNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.LocalVariableNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.MagicNumberCheckTest;
import com.puppycrawl.tools.checkstyle.checks.MemberNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.MethodLengthCheckTest;
import com.puppycrawl.tools.checkstyle.checks.MethodNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.MissingSwitchDefaultCheckTest;
import com.puppycrawl.tools.checkstyle.checks.ModifierOrderCheckTest;
import com.puppycrawl.tools.checkstyle.checks.NeedBracesCheckTest;
import com.puppycrawl.tools.checkstyle.checks.NewlineAtEndOfFileCheckTest;
import com.puppycrawl.tools.checkstyle.checks.NoWhitespaceAfterCheckTest;
import com.puppycrawl.tools.checkstyle.checks.NoWhitespaceBeforeCheckTest;
import com.puppycrawl.tools.checkstyle.checks.OperatorWrapCheckTest;
import com.puppycrawl.tools.checkstyle.checks.PackageHtmlCheckTest;
import com.puppycrawl.tools.checkstyle.checks.PackageNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.ParameterNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.ParameterNumberCheckTest;
import com.puppycrawl.tools.checkstyle.checks.ParenPadCheckTest;
import com.puppycrawl.tools.checkstyle.checks.RedundantImportCheckTest;
import com.puppycrawl.tools.checkstyle.checks.RedundantModifierTest;
import com.puppycrawl.tools.checkstyle.checks.RedundantThrowsCheckTest;
import com.puppycrawl.tools.checkstyle.checks.RightCurlyCheckTest;
import com.puppycrawl.tools.checkstyle.checks.SimplifyBooleanExpressionCheckTest;
import com.puppycrawl.tools.checkstyle.checks.SimplifyBooleanReturnCheckTest;
import com.puppycrawl.tools.checkstyle.checks.StaticVariableNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.TabCharacterCheckTest;
import com.puppycrawl.tools.checkstyle.checks.TodoCommentCheckTest;
import com.puppycrawl.tools.checkstyle.checks.TranslationCheckTest;
import com.puppycrawl.tools.checkstyle.checks.TypeNameCheckTest;
import com.puppycrawl.tools.checkstyle.checks.UncommentedMainCheckTest;
import com.puppycrawl.tools.checkstyle.checks.UnusedImportsCheckTest;
import com.puppycrawl.tools.checkstyle.checks.UpperEllCheckTest;
import com.puppycrawl.tools.checkstyle.checks.VisibilityModifierCheckTest;
import com.puppycrawl.tools.checkstyle.checks.WhitespaceAfterCheckTest;
import com.puppycrawl.tools.checkstyle.checks.WhitespaceAroundTest;
import com.puppycrawl.tools.checkstyle.checks.ArrayTypeStyleCheckTest;
import com.puppycrawl.tools.checkstyle.checks.FinalParametersCheckTest;
import com.puppycrawl.tools.checkstyle.checks.HideUtilityClassConstructorCheckTest;
import com.puppycrawl.tools.checkstyle.checks.AvoidNestedBlocksCheckTest;
import com.puppycrawl.tools.checkstyle.checks.InterfaceIsTypeCheckTest;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTypeCheckTest;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocVariableCheckTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for all tests.
 * @author Rick Giles
 * @version 22-Nov-2002
 */
public class AllTests {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllTests.class);
    }

    public static Test suite() {
        TestSuite suite =
            new TestSuite("Test for com.puppycrawl.tools.checkstyle");
        //$JUnit-BEGIN$
        suite.addTest(new TestSuite(DetailASTTest.class));
        suite.addTest(new TestSuite(AbstractViolationReporterTest.class));
        suite.addTest(new TestSuite(ArrayTypeStyleCheckTest.class));
        suite.addTest(new TestSuite(AvoidInlineConditionalsCheckTest.class));
        suite.addTest(new TestSuite(AvoidNestedBlocksCheckTest.class));
        suite.addTest(new TestSuite(AvoidStarImportTest.class));
        suite.addTest(new TestSuite(ArrayTrailingCommaCheckTest.class));
        suite.addTest(new TestSuite(ConfigurationLoaderTest.class));
        suite.addTest(new TestSuite(ConstantNameCheckTest.class));
        suite.addTest(new TestSuite(DesignForExtensionCheckTest.class));
        suite.addTest(new TestSuite(DoubleCheckedLockingCheckTest.class));
        suite.addTest(new TestSuite(EmptyBlockCheckTest.class));
        suite.addTest(new TestSuite(EmptyForIteratorPadCheckTest.class));
        suite.addTest(new TestSuite(EmptyStatementCheckTest.class));
        suite.addTest(new TestSuite(EqualsHashCodeCheckTest.class));
        suite.addTest(new TestSuite(FileLengthCheckTest.class));
        suite.addTest(new TestSuite(FileSetCheckLifecycleTest.class));
        suite.addTest(new TestSuite(FinalParametersCheckTest.class));
        suite.addTest(new TestSuite(GenericIllegalRegexpCheckTest.class));
        suite.addTest(new TestSuite(HeaderCheckTest.class));
        suite.addTest(new TestSuite(HiddenFieldCheckTest.class));
        suite.addTest(new TestSuite(HideUtilityClassConstructorCheckTest.class));
        suite.addTest(new TestSuite(IllegalImportCheckTest.class));
        suite.addTest(new TestSuite(IllegalInstantiationCheckTest.class));
        suite.addTest(new TestSuite(IndentationCheckTest.class));
        suite.addTest(new TestSuite(InnerAssignmentCheckTest.class));
        suite.addTest(new TestSuite(InterfaceIsTypeCheckTest.class));
        suite.addTest(new TestSuite(JavadocMethodCheckTest.class));
        suite.addTest(new TestSuite(JavadocTypeCheckTest.class));
        suite.addTest(new TestSuite(JavadocVariableCheckTest.class));
        suite.addTest(new TestSuite(LineLengthCheckTest.class));
        suite.addTest(new TestSuite(LocalFinalVariableNameCheckTest.class));
        suite.addTest(new TestSuite(LocalVariableNameCheckTest.class));
        suite.addTest(new TestSuite(MagicNumberCheckTest.class));
        suite.addTest(new TestSuite(MemberNameCheckTest.class));
        suite.addTest(new TestSuite(MethodLengthCheckTest.class));
        suite.addTest(new TestSuite(MethodNameCheckTest.class));
        suite.addTest(new TestSuite(ModifierOrderCheckTest.class));
        suite.addTest(new TestSuite(NeedBracesCheckTest.class));
        suite.addTest(new TestSuite(NewlineAtEndOfFileCheckTest.class));
        suite.addTest(new TestSuite(NoWhitespaceAfterCheckTest.class));
        suite.addTest(new TestSuite(NoWhitespaceBeforeCheckTest.class));
        suite.addTest(new TestSuite(OperatorWrapCheckTest.class));
        suite.addTest(new TestSuite(OptionTest.class));
        suite.addTest(new TestSuite(PackageHtmlCheckTest.class));
        suite.addTest(new TestSuite(PackageNameCheckTest.class));
        suite.addTest(new TestSuite(PackageNamesLoaderTest.class));
        suite.addTest(new TestSuite(PackageObjectFactoryTest.class));
        suite.addTest(new TestSuite(ParameterNameCheckTest.class));
        suite.addTest(new TestSuite(ParameterNumberCheckTest.class));
        suite.addTest(new TestSuite(ParenPadCheckTest.class));
        suite.addTest(new TestSuite(RedundantImportCheckTest.class));
        suite.addTest(new TestSuite(RedundantModifierTest.class));
        suite.addTest(new TestSuite(RedundantThrowsCheckTest.class));
        suite.addTest(new TestSuite(RightCurlyCheckTest.class));
        suite.addTest(new TestSuite(SimplifyBooleanExpressionCheckTest.class));
        suite.addTest(new TestSuite(SimplifyBooleanReturnCheckTest.class));
        suite.addTest(new TestSuite(StaticVariableNameCheckTest.class));
        suite.addTest(new TestSuite(StringArrayReaderTest.class));
        suite.addTest(new TestSuite(TabCharacterCheckTest.class));
        suite.addTest(new TestSuite(TodoCommentCheckTest.class));
        suite.addTest(new TestSuite(TranslationCheckTest.class));
        suite.addTest(new TestSuite(LeftCurlyCheckTest.class));
        suite.addTest(new TestSuite(TypeNameCheckTest.class));
        suite.addTest(new TestSuite(UncommentedMainCheckTest.class));
        suite.addTest(new TestSuite(UnusedImportsCheckTest.class));
        suite.addTest(new TestSuite(UpperEllCheckTest.class));
        suite.addTest(new TestSuite(UtilsTest.class));
        suite.addTest(new TestSuite(VisibilityModifierCheckTest.class));
        suite.addTest(new TestSuite(WhitespaceAfterCheckTest.class));
        suite.addTest(new TestSuite(WhitespaceAroundTest.class));
        suite.addTest(new TestSuite(XMLLoggerTest.class));
        suite.addTest(new TestSuite(FinalClassCheckTest.class));
        suite.addTest(new TestSuite(MissingSwitchDefaultCheckTest.class));
        //$JUnit-END$
        return suite;
    }
}
