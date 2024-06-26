package com.intellij.codeInspection.duplicatePropertyInspection;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.DescriptorComposer;
import com.intellij.codeInspection.ex.DescriptorProviderInspection;
import com.intellij.codeInspection.ex.HTMLComposerImpl;
import com.intellij.codeInspection.ex.JobDescriptor;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.concurrency.JobUtil;
import com.intellij.lang.properties.PropertiesBundle;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.impl.search.LowLevelSearchUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.StringSearcher;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class DuplicatePropertyInspection extends DescriptorProviderInspection {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.DuplicatePropertyInspection");

  public boolean CURRENT_FILE = true;
  public boolean MODULE_WITH_DEPENDENCIES = false;

  public boolean CHECK_DUPLICATE_VALUES = true;
  public boolean CHECK_DUPLICATE_KEYS = true;
  public boolean CHECK_DUPLICATE_KEYS_WITH_DIFFERENT_VALUES = true;

  private JRadioButton myFileScope;
  private JRadioButton myModuleScope;
  private JRadioButton myProjectScope;
  private JCheckBox myDuplicateValues;
  private JCheckBox myDuplicateKeys;
  private JCheckBox myDuplicateBoth;
  private JPanel myWholePanel;

  public void runInspection(AnalysisScope scope, final InspectionManager manager) {
    scope.accept(new PsiRecursiveElementVisitor() {
      @Override public void visitFile(PsiFile file) {
        checkFile(file, manager);
      }
    });
  }

  public HTMLComposerImpl getComposer() {
    return new DescriptorComposer(this) {
      protected void composeDescription(final CommonProblemDescriptor description, int i, StringBuffer buf, final RefEntity refElement) {
        @NonNls String descriptionTemplate = description.getDescriptionTemplate();
        descriptionTemplate = descriptionTemplate.replaceAll("#end", " ");
        buf.append(descriptionTemplate);
      }
    };
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static void surroundWithHref(StringBuffer anchor, PsiElement element, final boolean isValue) {
    if (element != null) {
      final PsiElement parent = element.getParent();
      PsiElement elementToLink = isValue ? parent.getFirstChild() : parent.getLastChild();
      if (elementToLink != null) {
        HTMLComposer.appendAfterHeaderIndention(anchor);
        HTMLComposer.appendAfterHeaderIndention(anchor);
        anchor.append("<a HREF=\"");
        try {
          final PsiFile file = element.getContainingFile();
          if (file != null) {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
              anchor.append(new URL(virtualFile.getUrl() + "#" + elementToLink.getTextRange().getStartOffset()));
            }
          }
        }
        catch (MalformedURLException e) {
          LOG.error(e);
        }
        anchor.append("\">");
        anchor.append(elementToLink.getText().replaceAll("\\$", "\\\\\\$"));
        anchor.append("</a>");
        compoundLineLink(anchor, element);
        anchor.append("<br>");
      }
    }
    else {
      anchor.append("<font style=\"font-family:verdana; font-weight:bold; color:#FF0000\";>");
      anchor.append(InspectionsBundle.message("inspection.export.results.invalidated.item"));
      anchor.append("</font>");
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static void compoundLineLink(StringBuffer lineAnchor, PsiElement psiElement) {
    final PsiFile file = psiElement.getContainingFile();
    if (file != null) {
      final VirtualFile vFile = file.getVirtualFile();
      if (vFile != null) {
        Document doc = FileDocumentManager.getInstance().getDocument(vFile);
        final int lineNumber = doc.getLineNumber(psiElement.getTextOffset()) + 1;
        lineAnchor.append(" ").append(InspectionsBundle.message("inspection.export.results.at.line")).append(" ");
        lineAnchor.append("<a HREF=\"");
        try {
          int offset = doc.getLineStartOffset(lineNumber - 1);
          offset = CharArrayUtil.shiftForward(doc.getCharsSequence(), offset, " \t");
          lineAnchor.append(new URL(vFile.getUrl() + "#" + offset));
        }
        catch (MalformedURLException e) {
          LOG.error(e);
        }
        lineAnchor.append("\">");
        lineAnchor.append(Integer.toString(lineNumber));
        lineAnchor.append("</a>");
      }
    }
  }

  @NotNull
  public JobDescriptor[] getJobDescriptors() {
    return JobDescriptor.EMPTY_ARRAY;
  }

  private void checkFile(final PsiFile file, final InspectionManager manager) {
    if (!(file instanceof PropertiesFile)) return;
    if (!getContext().isToCheckMember(file, this)) return;
    final PsiSearchHelper searchHelper = file.getManager().getSearchHelper();
    final PropertiesFile propertiesFile = (PropertiesFile)file;
    final List<Property> properties = propertiesFile.getProperties();
    Module module = ModuleUtil.findModuleForPsiElement(file);
    if (module == null) return;
    final GlobalSearchScope scope = CURRENT_FILE
                                    ? (GlobalSearchScope)GlobalSearchScope.fileScope(file)
                                    : MODULE_WITH_DEPENDENCIES
                                      ? GlobalSearchScope.moduleWithDependenciesScope(module)
                                      : GlobalSearchScope.projectScope(file.getProject());
    final Map<String, Set<PsiFile>> processedValueToFiles = Collections.synchronizedMap(new HashMap<String, Set<PsiFile>>());
    final Map<String, Set<PsiFile>> processedKeyToFiles = Collections.synchronizedMap(new HashMap<String, Set<PsiFile>>());
    final ProgressIndicator original = ProgressManager.getInstance().getProgressIndicator();
    final ProgressIndicator progress = original == null ? null : new ProgressWrapper(original);
    ProgressManager.getInstance().runProcess(new Runnable() {
      public void run() {
        JobUtil.invokeConcurrentlyForAll(properties, new Processor<Property>() {
          public boolean process(final Property property) {
            if (original != null) {
              original.checkCanceled();
              original.setText2(PropertiesBundle.message("searching.for.property.key.progress.text", property.getUnescapedKey()));
            }
            processTextUsages(processedValueToFiles, property.getValue(), processedKeyToFiles, searchHelper, scope);
            processTextUsages(processedKeyToFiles, property.getUnescapedKey(), processedValueToFiles, searchHelper, scope);
            return true;
          }
        }, "Searching properties usages");

        List<ProblemDescriptor> problemDescriptors = new ArrayList<ProblemDescriptor>();
        Map<String, Set<String>> keyToDifferentValues = new HashMap<String, Set<String>>();
        if (CHECK_DUPLICATE_KEYS || CHECK_DUPLICATE_KEYS_WITH_DIFFERENT_VALUES) {
          prepareDuplicateKeysByFile(processedKeyToFiles, manager, keyToDifferentValues, problemDescriptors, file, original);
        }
        if (CHECK_DUPLICATE_VALUES) prepareDuplicateValuesByFile(processedValueToFiles, manager, problemDescriptors, file, original);
        if (CHECK_DUPLICATE_KEYS_WITH_DIFFERENT_VALUES) {
          processDuplicateKeysWithDifferentValues(keyToDifferentValues, processedKeyToFiles, problemDescriptors, manager, file, original);
        }
        if (!problemDescriptors.isEmpty()) {
          addProblemElement(getRefManager().getReference(file),
                            problemDescriptors.toArray(new ProblemDescriptor[problemDescriptors.size()]));
        }
      }
    }, progress);
  }

  private static void processTextUsages(final Map<String, Set<PsiFile>> processedTextToFiles,
                                        final String text,
                                        final Map<String, Set<PsiFile>> processedFoundTextToFiles,
                                        final PsiSearchHelper searchHelper,
                                        final GlobalSearchScope scope) {
    if (!processedTextToFiles.containsKey(text)) {
      if (processedFoundTextToFiles.containsKey(text)) {
        final Set<PsiFile> filesWithValue = processedFoundTextToFiles.get(text);
        processedTextToFiles.put(text, filesWithValue);
      }
      else {
        final Set<PsiFile> resultFiles = new HashSet<PsiFile>();
        findFilesWithText(text, searchHelper, scope, resultFiles);
        if (resultFiles.isEmpty()) return;
        processedTextToFiles.put(text, resultFiles);
      }
    }
  }


  private static void prepareDuplicateValuesByFile(final Map<String, Set<PsiFile>> valueToFiles,
                                                   final InspectionManager manager,
                                                   final List<ProblemDescriptor> problemDescriptors,
                                                   final PsiFile psiFile,
                                                   final ProgressIndicator progress) {
    for (String value : valueToFiles.keySet()) {
      if (progress != null){
        progress.setText2(InspectionsBundle.message("duplicate.property.value.progress.indicator.text", value));
        if (progress.isCanceled()) throw new ProcessCanceledException();
      }
      StringSearcher searcher = new StringSearcher(value);
      StringBuffer message = new StringBuffer();
      int duplicatesCount = 0;
      Set<PsiFile> psiFilesWithDuplicates = valueToFiles.get(value);
      for (PsiFile file : psiFilesWithDuplicates) {
        CharSequence text = file.getViewProvider().getContents();
        for (int offset = LowLevelSearchUtil.searchWord(text, 0, text.length(), searcher);
             offset >= 0;
             offset = LowLevelSearchUtil.searchWord(text, offset + searcher.getPattern().length(), text.length(), searcher)
          ) {
          PsiElement element = file.findElementAt(offset);
          if (element != null && element.getParent() instanceof Property) {
            final Property property = (Property)element.getParent();
            if (Comparing.equal(property.getValue(), value) && element.getStartOffsetInParent() != 0) {
              if (duplicatesCount == 0){
                message.append(InspectionsBundle.message("duplicate.property.value.problem.descriptor", property.getValue()));
              }
              surroundWithHref(message, element, true);
              duplicatesCount ++;
            }
          }
        }
      }
      if (duplicatesCount > 1) {
        problemDescriptors.add(manager.createProblemDescriptor(psiFile, message.toString(),
                                                               (LocalQuickFix[])null, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      }
    }


  }

  private void prepareDuplicateKeysByFile(final Map<String, Set<PsiFile>> keyToFiles,
                                          final InspectionManager manager,
                                          final Map<String, Set<String>> keyToValues,
                                          final List<ProblemDescriptor> problemDescriptors,
                                          final PsiFile psiFile,
                                          final ProgressIndicator progress) {
    for (String key : keyToFiles.keySet()) {
      if (progress!= null){
        progress.setText2(InspectionsBundle.message("duplicate.property.key.progress.indicator.text", key));
        if (progress.isCanceled()) throw new ProcessCanceledException();
      }
      final StringBuffer message = new StringBuffer();
      int duplicatesCount = 0;
      Set<PsiFile> psiFilesWithDuplicates = keyToFiles.get(key);
      for (PsiFile file : psiFilesWithDuplicates) {
        if (!(file instanceof PropertiesFile)) continue;
        PropertiesFile propertiesFile = (PropertiesFile)file;
        final List<Property> propertiesByKey = propertiesFile.findPropertiesByKey(key);
        for (Property property : propertiesByKey) {
          if (duplicatesCount == 0){
            message.append(InspectionsBundle.message("duplicate.property.key.problem.descriptor", key));
          }
          surroundWithHref(message, property.getFirstChild(), false);
          duplicatesCount ++;
          //prepare for filter same keys different values
          Set<String> values = keyToValues.get(key);
          if (values == null){
            values = new HashSet<String>();
            keyToValues.put(key, values);
          }
          values.add(property.getValue());
        }
      }
      if (duplicatesCount > 1 && CHECK_DUPLICATE_KEYS) {
        problemDescriptors.add(manager.createProblemDescriptor(psiFile, message.toString(),
                                                               (LocalQuickFix[])null, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      }
    }

  }


  private static void processDuplicateKeysWithDifferentValues(final Map<String, Set<String>> keyToDifferentValues,
                                                              final Map<String, Set<PsiFile>> keyToFiles,
                                                              final List<ProblemDescriptor> problemDescriptors,
                                                              final InspectionManager manager,
                                                              final PsiFile psiFile,
                                                              final ProgressIndicator progress) {
    for (String key : keyToDifferentValues.keySet()) {
      if (progress != null) {
        progress.setText2(InspectionsBundle.message("duplicate.property.diff.key.progress.indicator.text", key));
        if (progress.isCanceled()) throw new ProcessCanceledException();
      }
      final Set<String> values = keyToDifferentValues.get(key);
      if (values == null || values.size() < 2){
        keyToFiles.remove(key);
      } else {
        StringBuffer message = new StringBuffer();
        final Set<PsiFile> psiFiles = keyToFiles.get(key);
        boolean firstUsage = true;
        for (PsiFile file : psiFiles) {
          if (!(file instanceof PropertiesFile)) continue;
          PropertiesFile propertiesFile = (PropertiesFile)file;
          final List<Property> propertiesByKey = propertiesFile.findPropertiesByKey(key);
          for (Property property : propertiesByKey) {
            if (firstUsage){
              message.append(InspectionsBundle.message("duplicate.property.diff.key.problem.descriptor", key));
              firstUsage = false;
            }
            surroundWithHref(message, property.getFirstChild(), false);
          }
        }
        problemDescriptors.add(manager.createProblemDescriptor(psiFile, message.toString(),
                                                               (LocalQuickFix[])null, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      }
    }
  }

  private static void findFilesWithText(String stringToFind,
                                        PsiSearchHelper searchHelper,
                                        GlobalSearchScope scope,
                                        final Set<PsiFile> resultFiles) {
    final List<String> words = StringUtil.getWordsIn(stringToFind);
    if (words.isEmpty()) return;
    Collections.sort(words, new Comparator<String>() {
      public int compare(final String o1, final String o2) {
        return o2.length() - o1.length();
      }
    });
    for (String word : words) {
      final Set<PsiFile> files = new THashSet<PsiFile>();
      searchHelper.processAllFilesWithWord(word, scope, new CommonProcessors.CollectProcessor<PsiFile>(files), true);
      if (resultFiles.isEmpty()) {
        resultFiles.addAll(files);
      }
      else {
        resultFiles.retainAll(files);
      }
      if (resultFiles.isEmpty()) return;
    }
  }

  @NotNull
  public String getDisplayName() {
    return InspectionsBundle.message("duplicate.property.display.name");
  }

  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.INTERNATIONALIZATION_GROUP_NAME;
  }

  @NotNull
  public String getShortName() {
    return "DuplicatePropertyInspection";
  }

  public boolean isEnabledByDefault() {
    return false;
  }

  public JComponent createOptionsPanel() {
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myFileScope);
    buttonGroup.add(myModuleScope);
    buttonGroup.add(myProjectScope);

    myFileScope.setSelected(CURRENT_FILE);
    myModuleScope.setSelected(MODULE_WITH_DEPENDENCIES);
    myProjectScope.setSelected(!(CURRENT_FILE || MODULE_WITH_DEPENDENCIES));

    myFileScope.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CURRENT_FILE = myFileScope.isSelected();
      }
    });
    myModuleScope.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        MODULE_WITH_DEPENDENCIES = myModuleScope.isSelected();
        if (MODULE_WITH_DEPENDENCIES){
          CURRENT_FILE = false;
        }
      }
    });
    myProjectScope.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (myProjectScope.isSelected()){
          CURRENT_FILE = false;
          MODULE_WITH_DEPENDENCIES = false;
        }
      }
    });

    myDuplicateKeys.setSelected(CHECK_DUPLICATE_KEYS);
    myDuplicateValues.setSelected(CHECK_DUPLICATE_VALUES);
    myDuplicateBoth.setSelected(CHECK_DUPLICATE_KEYS_WITH_DIFFERENT_VALUES);

    myDuplicateKeys.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CHECK_DUPLICATE_KEYS = myDuplicateKeys.isSelected();
      }
    });
    myDuplicateValues.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CHECK_DUPLICATE_VALUES = myDuplicateValues.isSelected();
      }
    });
    myDuplicateBoth.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CHECK_DUPLICATE_KEYS_WITH_DIFFERENT_VALUES = myDuplicateBoth.isSelected();
      }
    });
    return myWholePanel;
  }
}
