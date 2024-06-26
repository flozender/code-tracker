package com.intellij.codeEditor.printing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.TabbedPaneWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class PrintDialog extends DialogWrapper {
  private JRadioButton myRbCurrentFile = null;
  private JRadioButton myRbSelectedText = null;
  private JRadioButton myRbCurrentPackage = null;
  private JCheckBox myCbIncludeSubpackages = null;

  private JComboBox myPaperSizeCombo = null;

  private JCheckBox myCbColorPrinting = null;
  private JCheckBox myCbSyntaxPrinting = null;
  private JCheckBox myCbPrintAsGraphics = null;

  private JRadioButton myRbPortrait = null;
  private JRadioButton myRbLandscape = null;

  private JComboBox myFontNameCombo = null;
  private JComboBox myFontSizeCombo = null;

  private JCheckBox myCbLineNumbers = null;

  private JRadioButton myRbNoWrap = null;
  private JRadioButton myRbWrapAtWordBreaks = null;

  private JTextField myTopMarginField = null;
  private JTextField myBottomMarginField = null;
  private JTextField myLeftMarginField = null;
  private JTextField myRightMarginField = null;

  private JCheckBox myCbDrawBorder = null;

  private JTextField myLineTextField1 = null;
  private JComboBox myLinePlacementCombo1 = null;
  private JComboBox myLineAlignmentCombo1 = null;
  private JTextField myLineTextField2 = null;
  private JComboBox myLinePlacementCombo2 = null;
  private JComboBox myLineAlignmentCombo2 = null;
  private JComboBox myFooterFontSizeCombo = null;
  private JComboBox myFooterFontNameCombo = null;
  private String myFileName = null;
  private String myDirectoryName = null;
  private boolean isSelectedTextEnabled;


  public PrintDialog(String fileName, String directoryName, boolean isSelectedTextEnabled, Project project) {
    super(project, true);
    setOKButtonText("Print");
    myFileName = fileName;
    myDirectoryName = directoryName;
    this.isSelectedTextEnabled = isSelectedTextEnabled;
    setTitle("Print");
    init();
  }


  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(4,8,8,4));
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;

    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = new Insets(0,0,0,0);

    myRbCurrentFile = new JRadioButton("File " + (myFileName != null ? myFileName : ""));
    panel.add(myRbCurrentFile, gbConstraints);

    myRbSelectedText = new JRadioButton("Selected text");
    gbConstraints.gridy++;
    gbConstraints.insets = new Insets(0,0,0,0);
    panel.add(myRbSelectedText, gbConstraints);

    myRbCurrentPackage = new JRadioButton("All files in directory "+ (myDirectoryName != null ? myDirectoryName : ""));
    gbConstraints.gridy++;
    gbConstraints.insets = new Insets(0,0,0,0);
    panel.add(myRbCurrentPackage, gbConstraints);

    myCbIncludeSubpackages = new JCheckBox("Include subdirectories ");
    gbConstraints.gridy++;
    gbConstraints.insets = new Insets(0,20,0,0);
    panel.add(myCbIncludeSubpackages, gbConstraints);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbCurrentFile);
    buttonGroup.add(myRbSelectedText);
    buttonGroup.add(myRbCurrentPackage);

    ActionListener actionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myCbIncludeSubpackages.setEnabled(myRbCurrentPackage.isSelected());
      }
    };

    myRbCurrentFile.addActionListener(actionListener);
    myRbSelectedText.addActionListener(actionListener);
    myRbCurrentPackage.addActionListener(actionListener);

    return panel;
  }

  protected JComponent createCenterPanel() {
    TabbedPaneWrapper tabbedPaneWrapper = new TabbedPaneWrapper();
    tabbedPaneWrapper.addTab("Settings", createPrintSettingsPanel());
    tabbedPaneWrapper.addTab("Header and Footer", createHeaderAndFooterPanel());
    tabbedPaneWrapper.addTab("Advanced", createAdvancedPanel());
    return tabbedPaneWrapper.getComponent();
  }

  private JPanel createPrintSettingsPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    panel.setBorder(BorderFactory.createEmptyBorder(8,8,4,4));
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.weighty = 0;
    gbConstraints.insets = new Insets(0, 8, 6, 4);
    gbConstraints.fill = GridBagConstraints.BOTH;

    JLabel paperSizeLabel = new MyLabel("Paper size");
    panel.add(paperSizeLabel, gbConstraints);
    myPaperSizeCombo = createPageSizesCombo();
    gbConstraints.gridx = 1;
    gbConstraints.gridwidth = 2;
    panel.add(myPaperSizeCombo, gbConstraints);

    JLabel fontLabel = new MyLabel("Font");
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridy++;
    panel.add(fontLabel, gbConstraints);

    myFontNameCombo = createFontNamesComboBox();
    gbConstraints.gridx = 1;
    panel.add(myFontNameCombo, gbConstraints);

    myFontSizeCombo = createFontSizesComboBox();
    gbConstraints.gridx = 2;
    panel.add(myFontSizeCombo, gbConstraints);

    myCbLineNumbers = new JCheckBox("Show line numbers");
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 3;
    gbConstraints.gridy++;
    panel.add(myCbLineNumbers, gbConstraints);

    myCbDrawBorder = new JCheckBox("Draw border");
    gbConstraints.gridy++;
    panel.add(myCbDrawBorder, gbConstraints);

    gbConstraints.insets = new Insets(0, 0, 6, 4);
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 3;
    gbConstraints.gridy++;
    panel.add(createStyleAndLayoutPanel(), gbConstraints);

    gbConstraints.gridy++;
    gbConstraints.weighty = 1;
    panel.add(new MyTailPanel(), gbConstraints);
    return panel;
  }

  private JPanel createAdvancedPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    panel.setBorder(BorderFactory.createEmptyBorder(8,8,4,4));
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.weighty = 0;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = new Insets(0, 0, 6, 4);

    panel.add(createWrappingPanel(), gbConstraints);

    gbConstraints.gridy++;
    panel.add(createMarginsPanel(), gbConstraints);

    gbConstraints.gridy++;
    gbConstraints.weighty = 1;
    panel.add(new MyTailPanel(), gbConstraints);

    return panel;
  }

  private JPanel createStyleAndLayoutPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(createOrientationPanel());
    panel.add(createStylePanel());
    return panel;
  }

  private JPanel createOrientationPanel() {
    JPanel panel1 = new JPanel();
    panel1.setBorder(IdeBorderFactory.createTitledBorder("Orientation"));
    JPanel panel = panel1;
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    myRbPortrait = new JRadioButton("Portrait");
    panel.add(myRbPortrait, gbConstraints);

    myRbLandscape = new JRadioButton("Landscape");
    gbConstraints.gridy++;
    panel.add(myRbLandscape, gbConstraints);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbPortrait);
    buttonGroup.add(myRbLandscape);

    return panel;
  }

  private JPanel createStylePanel() {
    JPanel panel1 = new JPanel();
    panel1.setBorder(IdeBorderFactory.createTitledBorder("Style"));
    JPanel panel = panel1;
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    myCbColorPrinting = new JCheckBox("Color printing");
    panel.add(myCbColorPrinting, gbConstraints);

    myCbSyntaxPrinting = new JCheckBox("Syntax printing");
    gbConstraints.gridy++;
    panel.add(myCbSyntaxPrinting, gbConstraints);

    myCbPrintAsGraphics = new JCheckBox("Print as graphics");
    gbConstraints.gridy++;
    panel.add(myCbPrintAsGraphics, gbConstraints);

    return panel;
  }

  private JPanel createWrappingPanel() {
    JPanel panel1 = new JPanel();
    panel1.setBorder(IdeBorderFactory.createTitledBorder("Wrapping"));
    JPanel panel = panel1;
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    myRbNoWrap = new JRadioButton("No wrap");
    panel.add(myRbNoWrap, gbConstraints);

    myRbWrapAtWordBreaks = new JRadioButton("Wrap at word breaks");
    gbConstraints.gridy++;
    panel.add(myRbWrapAtWordBreaks, gbConstraints);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbNoWrap);
    buttonGroup.add(myRbWrapAtWordBreaks);

    return panel;
  }

  private JPanel createMarginsPanel() {
    JPanel panel1 = new JPanel();
    panel1.setBorder(IdeBorderFactory.createTitledBorder("Margins (inches)"));
    JPanel panel = panel1;
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;

    panel.add(new MyLabel("Top"), gbConstraints);
    myTopMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 1;
    panel.add(myTopMarginField, gbConstraints);

    gbConstraints.weightx = 1;
    gbConstraints.gridx = 2;
    panel.add(new MyLabel("   Bottom"), gbConstraints);
    myBottomMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 3;
    panel.add(myBottomMarginField, gbConstraints);

    gbConstraints.weightx = 1;
    gbConstraints.gridx = 0;
    gbConstraints.gridy++;
    panel.add(new MyLabel("Left"), gbConstraints);
    myLeftMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 1;
    panel.add(myLeftMarginField, gbConstraints);

    gbConstraints.weightx = 1;
    gbConstraints.gridx = 2;
    panel.add(new MyLabel("   Right"), gbConstraints);
    myRightMarginField = new MyTextField(6);
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 3;
    panel.add(myRightMarginField, gbConstraints);

    return panel;
  }

  private JPanel createHeaderAndFooterPanel() {
//    JPanel panel = createGroupPanel("Header");
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(8,8,4,4));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = new Insets(0, 0, 6, 4);

    gbConstraints.gridwidth = 3;
    myLineTextField1 = new MyTextField(30);
    myLinePlacementCombo1 = new JComboBox();
    myLineAlignmentCombo1 = new JComboBox();
    JPanel linePanel1 = createLinePanel("Line #1", myLineTextField1, myLinePlacementCombo1, myLineAlignmentCombo1);
    panel.add(linePanel1, gbConstraints);

    myLineTextField2 = new MyTextField(30);
    myLinePlacementCombo2 = new JComboBox();
    myLineAlignmentCombo2 = new JComboBox();
    JPanel linePanel2 = createLinePanel("Line #2", myLineTextField2, myLinePlacementCombo2, myLineAlignmentCombo2);
    gbConstraints.gridy++;
    panel.add(linePanel2, gbConstraints);

    gbConstraints.insets = new Insets(0, 8, 6, 4);
    gbConstraints.gridy++;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridx = 0;
    panel.add(new MyLabel("Font"), gbConstraints);
    myFooterFontNameCombo = createFontNamesComboBox();
    gbConstraints.gridx = 1;
    panel.add(myFooterFontNameCombo, gbConstraints);

    myFooterFontSizeCombo = createFontSizesComboBox();
    gbConstraints.gridx = 2;
    panel.add(myFooterFontSizeCombo, gbConstraints);

    return panel;
  }

  private JPanel createLinePanel(String name, JTextField lineTextField, JComboBox linePlacementCombo, JComboBox lineAlignmentCombo) {
    JPanel panel1 = new JPanel();
    panel1.setBorder(IdeBorderFactory.createTitledBorder(name));
    JPanel panel = panel1;
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.gridy = 0;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 1;
    gbConstraints.gridheight = 1;
    gbConstraints.weightx = 0;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.insets = new Insets(0, 0, 6, 0);

    panel.add(new MyLabel("Text line   "), gbConstraints);
    gbConstraints.gridx = 1;
    gbConstraints.gridwidth = 4;
    gbConstraints.weightx = 1;
    panel.add(lineTextField, gbConstraints);

    gbConstraints.gridwidth = 1;
    gbConstraints.gridy++;
    gbConstraints.gridx = 0;
    gbConstraints.weightx = 0;
    panel.add(new MyLabel("Placement   "), gbConstraints);
    linePlacementCombo.addItem(PrintSettings.HEADER);
    linePlacementCombo.addItem(PrintSettings.FOOTER);
    gbConstraints.gridx = 1;
    gbConstraints.weightx = 0;
    panel.add(linePlacementCombo, gbConstraints);

    gbConstraints.gridx = 2;
    gbConstraints.weightx = 1;
    panel.add(new MyTailPanel(), gbConstraints);

    gbConstraints.gridx = 3;
    gbConstraints.weightx = 0;
    panel.add(new MyLabel("Alignment   "), gbConstraints);
    lineAlignmentCombo.addItem(PrintSettings.LEFT);
    lineAlignmentCombo.addItem(PrintSettings.CENTER);
    lineAlignmentCombo.addItem(PrintSettings.RIGHT);
    gbConstraints.gridx = 4;
    gbConstraints.weightx = 0;
    panel.add(lineAlignmentCombo, gbConstraints);

    return panel;
  }

  private JComboBox createFontNamesComboBox() {
    JComboBox comboBox = new JComboBox();
    GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Font[] fonts = graphicsEnvironment.getAllFonts();
    for(int i = 0; i < fonts.length; i++) {
      Font font = fonts[i];
      comboBox.addItem(font.getName());
    }
    return comboBox;
  }

  private JComboBox createFontSizesComboBox() {
    JComboBox comboBox = new JComboBox();
    for(int i = 6; i < 40; i++) {
      comboBox.addItem(""+i);
    }
    return comboBox;
  }

  private JComboBox createPageSizesCombo() {
    JComboBox pageSizesCombo = new JComboBox();
    String[] names = PageSizes.getNames();
    for(int i = 0; i < names.length; i++) {
      pageSizesCombo.addItem(PageSizes.getItem(names[i]));
    }
    return pageSizesCombo;
  }

  private static class MyTailPanel extends JPanel {
    public MyTailPanel(){
      setFocusable(false);
    }

    public Dimension getMinimumSize() {
      return new Dimension(0,0);
    }
    public Dimension getPreferredSize() {
      return new Dimension(0,0);
    }
  }

  public void reset() {
    PrintSettings printSettings = PrintSettings.getInstance();

    myRbSelectedText.setEnabled(isSelectedTextEnabled);
    myRbSelectedText.setSelected(isSelectedTextEnabled);
    myRbCurrentFile.setEnabled(myFileName != null);
    myRbCurrentFile.setSelected(myFileName != null && !isSelectedTextEnabled);
    myRbCurrentPackage.setEnabled(myDirectoryName != null);
    myRbCurrentPackage.setSelected(myDirectoryName != null && !isSelectedTextEnabled && myFileName == null);

    myCbIncludeSubpackages.setSelected(printSettings.isIncludeSubdirectories());
    myCbIncludeSubpackages.setEnabled(myRbCurrentPackage.isSelected());

    Object selectedPageSize = PageSizes.getItem(printSettings.PAPER_SIZE);
    if(selectedPageSize != null) {
      myPaperSizeCombo.setSelectedItem(selectedPageSize);
    }
    myCbColorPrinting.setSelected(printSettings.COLOR_PRINTING);
    myCbSyntaxPrinting.setSelected(printSettings.SYNTAX_PRINTING);
    myCbPrintAsGraphics.setSelected(printSettings.PRINT_AS_GRAPHICS);

    if(printSettings.PORTRAIT_LAYOUT) {
      myRbPortrait.setSelected(true);
    }
    else {
      myRbLandscape.setSelected(true);
    }
    myFontNameCombo.setSelectedItem(printSettings.FONT_NAME);
    myFontSizeCombo.setSelectedItem(""+printSettings.FONT_SIZE);

    myCbLineNumbers.setSelected(printSettings.PRINT_LINE_NUMBERS);

    if(printSettings.WRAP) {
      myRbWrapAtWordBreaks.setSelected(true);
    }
    else {
      myRbNoWrap.setSelected(true);
    }

    myTopMarginField.setText(""+printSettings.TOP_MARGIN);
    myBottomMarginField.setText(""+printSettings.BOTTOM_MARGIN);
    myLeftMarginField.setText(""+printSettings.LEFT_MARGIN);
    myRightMarginField.setText(""+printSettings.RIGHT_MARGIN);

    myCbDrawBorder.setSelected(printSettings.DRAW_BORDER);


    myLineTextField1.setText(printSettings.FOOTER_HEADER_TEXT1);
    myLinePlacementCombo1.setSelectedItem(printSettings.FOOTER_HEADER_PLACEMENT1);
    myLineAlignmentCombo1.setSelectedItem(printSettings.FOOTER_HEADER_ALIGNMENT1);

    myLineTextField2.setText(printSettings.FOOTER_HEADER_TEXT2);
    myLinePlacementCombo2.setSelectedItem(printSettings.FOOTER_HEADER_PLACEMENT2);
    myLineAlignmentCombo2.setSelectedItem(printSettings.FOOTER_HEADER_ALIGNMENT2);

    myFooterFontSizeCombo.setSelectedItem(""+printSettings.FOOTER_HEADER_FONT_SIZE);
    myFooterFontNameCombo.setSelectedItem(printSettings.FOOTER_HEADER_FONT_NAME);
  }

  public void apply() {
    PrintSettings printSettings = PrintSettings.getInstance();

    if (myRbCurrentFile.isSelected()){
      printSettings.setPrintScope(PrintSettings.PRINT_FILE);
    }
    else if (myRbSelectedText.isSelected()){
      printSettings.setPrintScope(PrintSettings.PRINT_SELECTED_TEXT);
    }
    else if (myRbCurrentPackage.isSelected()){
      printSettings.setPrintScope(PrintSettings.PRINT_DIRECTORY);
    }
    printSettings.setIncludeSubdirectories(myCbIncludeSubpackages.isSelected());

    printSettings.PAPER_SIZE = PageSizes.getName(myPaperSizeCombo.getSelectedItem());
    printSettings.COLOR_PRINTING = myCbColorPrinting.isSelected();
    printSettings.SYNTAX_PRINTING = myCbSyntaxPrinting.isSelected();
    printSettings.PRINT_AS_GRAPHICS = myCbPrintAsGraphics.isSelected();

    printSettings.PORTRAIT_LAYOUT = myRbPortrait.isSelected();

    printSettings.FONT_NAME = (String)myFontNameCombo.getSelectedItem();

    try {
      String fontSizeStr = (String)myFontSizeCombo.getSelectedItem();
      printSettings.FONT_SIZE = Integer.parseInt(fontSizeStr);
    }
    catch(NumberFormatException e) {
    }

    printSettings.PRINT_LINE_NUMBERS = myCbLineNumbers.isSelected();

    printSettings.WRAP = myRbWrapAtWordBreaks.isSelected();


    try {
      printSettings.TOP_MARGIN = Float.parseFloat(myTopMarginField.getText());
    }
    catch(NumberFormatException e) {
    }

    try {
      printSettings.BOTTOM_MARGIN = Float.parseFloat(myBottomMarginField.getText());
    }
    catch(NumberFormatException e) {
    }

    try {
      printSettings.LEFT_MARGIN = Float.parseFloat(myLeftMarginField.getText());
    }
    catch(NumberFormatException e) {
    }

    try {
      printSettings.RIGHT_MARGIN = Float.parseFloat(myRightMarginField.getText());
    }
    catch(NumberFormatException e) {
    }
    printSettings.DRAW_BORDER = myCbDrawBorder.isSelected();

    printSettings.FOOTER_HEADER_TEXT1 = myLineTextField1.getText();
    printSettings.FOOTER_HEADER_ALIGNMENT1 = (String)myLineAlignmentCombo1.getSelectedItem();
    printSettings.FOOTER_HEADER_PLACEMENT1 = (String)myLinePlacementCombo1.getSelectedItem();

    printSettings.FOOTER_HEADER_TEXT2 = myLineTextField2.getText();
    printSettings.FOOTER_HEADER_ALIGNMENT2 = (String)myLineAlignmentCombo2.getSelectedItem();
    printSettings.FOOTER_HEADER_PLACEMENT2 = (String)myLinePlacementCombo2.getSelectedItem();

    try {
      printSettings.FOOTER_HEADER_FONT_SIZE = Integer.parseInt((String)myFooterFontSizeCombo.getSelectedItem());
    }
    catch(NumberFormatException e) {
    }

    printSettings.FOOTER_HEADER_FONT_NAME = (String)myFooterFontNameCombo.getSelectedItem();

  }

  protected Action[] createActions() {
    return new Action[]{getOKAction(),getCancelAction(), new ApplyAction()};
  }

  class ApplyAction extends AbstractAction{
    public ApplyAction(){
      putValue(Action.NAME,"A&pply");
    }

    public void actionPerformed(ActionEvent e){
      apply();
    }
  }


  private static class MyTextField extends JTextField {
    public MyTextField(int size) {
     super(size);
    }
    public Dimension getMinimumSize() {
      return super.getPreferredSize();
    }
  }

  private static class MyLabel extends JLabel {
    public MyLabel(String text) {
     super(text);
    }
    public Dimension getMinimumSize() {
      return super.getPreferredSize();
    }
  }


}