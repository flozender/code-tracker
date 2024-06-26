package com.intellij.ide.todo.configurable;

import com.intellij.application.options.colors.ColorAndFontDescription;
import com.intellij.application.options.colors.ColorAndFontDescriptionPanel;
import com.intellij.application.options.colors.TextAttributesDescription;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.search.TodoAttributes;
import com.intellij.psi.search.TodoPattern;
import com.intellij.ide.IdeBundle;

import javax.swing.*;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
class PatternDialog extends DialogWrapper{
  private TodoPattern myPattern;

  private JComboBox myIconComboBox;
  private JCheckBox myCaseSensitiveCheckBox;
  private JTextField myPatternStringField;
  private ColorAndFontDescriptionPanel myColorAndFontDescriptionPanel;
  private ColorAndFontDescription myColorAndFontDescription;

  public PatternDialog(Component parent,TodoPattern pattern){
    super(parent,true);
    myPattern=pattern;
    myIconComboBox=new JComboBox(
      new Icon[]{TodoAttributes.DEFAULT_ICON,TodoAttributes.QUESTION_ICON,TodoAttributes.IMPORTANT_ICON}
    );
    myIconComboBox.setSelectedItem(pattern.getAttributes().getIcon());
    myIconComboBox.setRenderer(new TodoTypeListCellRenderer());
    myCaseSensitiveCheckBox=new JCheckBox(IdeBundle.message("checkbox.case.sensitive"),pattern.isCaseSensitive());
    myPatternStringField=new JTextField(pattern.getPatternString());

    myColorAndFontDescriptionPanel = new ColorAndFontDescriptionPanel();

    TextAttributes attributes = myPattern.getAttributes().getTextAttributes();

    myColorAndFontDescription = new TextAttributesDescription(null, null, attributes, null, EditorColorsManager.getInstance().getGlobalScheme()) {
      public void apply(EditorColorsScheme scheme) {

      }

      public boolean isErrorStripeEnabled() {
        return true;
      }
    };

    myColorAndFontDescriptionPanel.reset(myColorAndFontDescription);

    init();
  }

  public JComponent getPreferredFocusedComponent(){
    return myPatternStringField;
  }

  protected void doOKAction(){
    myPattern.setPatternString(myPatternStringField.getText().trim());
    myPattern.setCaseSensitive(myCaseSensitiveCheckBox.isSelected());
    myPattern.getAttributes().setIcon((Icon)myIconComboBox.getSelectedItem());

    myColorAndFontDescriptionPanel.apply(myColorAndFontDescription, null);
    super.doOKAction();
  }

  protected JComponent createCenterPanel(){
    JPanel panel=new JPanel(new GridBagLayout());

    GridBagConstraints gb = new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,new Insets(0,0,5,10),0,0);

    JLabel patternLabel=new JLabel(IdeBundle.message("label.todo.pattern"));
    panel.add(patternLabel, gb);
    Dimension oldPreferredSize=myPatternStringField.getPreferredSize();
    myPatternStringField.setPreferredSize(new Dimension(300,oldPreferredSize.height));
    gb.gridx = 1;
    gb.gridwidth = GridBagConstraints.REMAINDER;
    gb.weightx = 1;
    panel.add(myPatternStringField,gb);

    JLabel iconLabel=new JLabel(IdeBundle.message("label.todo.icon"));
    gb.gridy++;
    gb.gridx = 0;
    gb.gridwidth = 1;
    gb.weightx = 0;
    panel.add(iconLabel, gb);

    gb.gridx = 1;
    gb.fill = GridBagConstraints.NONE;
    gb.gridwidth = GridBagConstraints.REMAINDER;
    gb.weightx = 0;
    panel.add(myIconComboBox, gb);

    gb.gridy++;
    gb.gridx = 0;
    gb.fill = GridBagConstraints.HORIZONTAL;
    gb.gridwidth = GridBagConstraints.REMAINDER;
    gb.weightx = 1;
    panel.add(myCaseSensitiveCheckBox, gb);

    gb.gridy++;
    gb.gridx = 0;
    gb.gridwidth = GridBagConstraints.REMAINDER;
    gb.weightx = 1;
    panel.add(myColorAndFontDescriptionPanel, gb);
    return panel;
  }
}
