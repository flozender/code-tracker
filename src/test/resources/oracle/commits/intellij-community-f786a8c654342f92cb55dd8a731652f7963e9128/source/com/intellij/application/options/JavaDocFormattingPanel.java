package com.intellij.application.options;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.openapi.application.ApplicationBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author max
 */
public class JavaDocFormattingPanel extends OptionTreeWithPreviewPanel {
  private JCheckBox myEnableCheckBox;

  private final JPanel myPanel = new JPanel(new BorderLayout());
  private static final String OTHER_GROUP = ApplicationBundle.message("group.javadoc.other");
  private static final String INVALID_TAGS_GROUP = ApplicationBundle.message("group.javadoc.invalid.tags");
  private static final String BLANK_LINES_GROUP = ApplicationBundle.message("group.javadoc.blank.lines");
  private static final String ALIGNMENT_GROUP = ApplicationBundle.message("group.javadoc.alignment");

  public JavaDocFormattingPanel(CodeStyleSettings settings) {
    super(settings);
    myEnableCheckBox = new JCheckBox(ApplicationBundle.message("checkbox.enable.javadoc.formatting"));
    myEnableCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        update();
      }
    });

    myPanel.add(BorderLayout.CENTER, getInternalPanel());
    myPanel.add(myEnableCheckBox, BorderLayout.NORTH);
  }

  public JComponent getPanel() {
    return myPanel;
  }

  private void update() {
    setEnabled(getPanel(), myEnableCheckBox.isSelected());
    myEnableCheckBox.setEnabled(true);
  }

  protected void initTables() {
    initBooleanField("JD_ALIGN_PARAM_COMMENTS", ApplicationBundle.message("checkbox.align.parameter.descriptions"), ALIGNMENT_GROUP);
    initBooleanField("JD_ALIGN_EXCEPTION_COMMENTS", ApplicationBundle.message("checkbox.align.thrown.exception.descriptions"), ALIGNMENT_GROUP);

    initBooleanField("JD_ADD_BLANK_AFTER_DESCRIPTION", ApplicationBundle.message("checkbox.after.description"), BLANK_LINES_GROUP);
    initBooleanField("JD_ADD_BLANK_AFTER_PARM_COMMENTS", ApplicationBundle.message("checkbox.after.parameter.descriptions"), BLANK_LINES_GROUP);
    initBooleanField("JD_ADD_BLANK_AFTER_RETURN", ApplicationBundle.message("checkbox.after.return.tag"), BLANK_LINES_GROUP);

    initBooleanField("JD_KEEP_INVALID_TAGS", ApplicationBundle.message("checkbox.keep.invalid.tags"), INVALID_TAGS_GROUP);
    initBooleanField("JD_KEEP_EMPTY_PARAMETER", ApplicationBundle.message("checkbox.keep.empty.param.tags"), INVALID_TAGS_GROUP);
    initBooleanField("JD_KEEP_EMPTY_RETURN", ApplicationBundle.message("checkbox.keep.empty.return.tags"), INVALID_TAGS_GROUP);
    initBooleanField("JD_KEEP_EMPTY_EXCEPTION", ApplicationBundle.message("checkbox.keep.empty.throws.tags"), INVALID_TAGS_GROUP);

    initBooleanField("JD_LEADING_ASTERISKS_ARE_ENABLED", ApplicationBundle.message("checkbox.enable.leading.asterisks"), OTHER_GROUP);
    initBooleanField("JD_USE_THROWS_NOT_EXCEPTION", ApplicationBundle.message("checkbox.use.throws.rather.than.exception"), OTHER_GROUP);
    initBooleanField("WRAP_COMMENTS", ApplicationBundle.message("checkbox.wrap.at.right.margin"), OTHER_GROUP);
    initBooleanField("JD_P_AT_EMPTY_LINES", ApplicationBundle.message("checkbox.generate.p.on.empty.lines"), OTHER_GROUP);
    initBooleanField("JD_KEEP_EMPTY_LINES", ApplicationBundle.message("checkbox.keep.empty.lines"), OTHER_GROUP);
    initBooleanField("JD_DO_NOT_WRAP_ONE_LINE_COMMENTS", ApplicationBundle.message("checkbox.do.not.wrap.one.line.comments"), OTHER_GROUP );
  }

  protected int getRightMargin() {
    return 47;
  }

  protected String getPreviewText() {                    //| Margin is here
    return "package sample;\n" +
           "public class Sample {\n" +
           "  /**\n" +
           "   * This is a method description that is long enough to exceed right margin.\n" +
           "   *\n" +
           "   * Another paragraph of the description placed after blank line.\n" +
           "   * @param i short named parameter description\n" +
           "   * @param longParameterName long named parameter description\n" +
           "   * @param missingDescription\n" +
           "   * @return return description.\n" +
           "   * @throws XXXException description.\n" +
           "   * @throws YException description.\n" +
           "   * @throws ZException\n" +
           "   *\n" +
           "   * @invalidTag" +
           "   */\n" +
           "  public abstract String sampleMethod(int i, int longParameterName, int missingDescription) throws XXXException, YException, ZException;\n" +
           "\n"+
           "  /** One-line comment */\n" +
           "  public abstract String sampleMethod2();\n";
  }


private static void setEnabled(JComponent c, boolean enabled) {
  c.setEnabled(enabled);
  Component[] children = c.getComponents();
  for (Component child : children) {
    if (child instanceof JComponent) {
      setEnabled((JComponent)child, enabled);
    }
  }
}

  public void apply(CodeStyleSettings settings) {
    super.apply(settings);
    settings.ENABLE_JAVADOC_FORMATTING = myEnableCheckBox.isSelected();
  }

  protected void resetImpl(final CodeStyleSettings settings) {
    super.resetImpl(settings);
    myEnableCheckBox.setSelected(settings.ENABLE_JAVADOC_FORMATTING);
    update();
  }

  public boolean isModified(CodeStyleSettings settings) {
    return super.isModified(settings) || myEnableCheckBox.isSelected() != settings.ENABLE_JAVADOC_FORMATTING;
  }
}
