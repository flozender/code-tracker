package com.intellij.compiler.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;

/**
 * @author nik
 */
public class BuildArtifactAction extends AnAction {
  private final Project myProject;
  private Artifact myArtifact;

  public BuildArtifactAction(Project project, Artifact artifact) {
    super(artifact.getName(), "Build Artifact '" + artifact.getName() + "'", artifact.getArtifactType().getIcon());
    myProject = project;
    myArtifact = artifact;
  }

  public void actionPerformed(AnActionEvent e) {
    final String outputPath = myArtifact.getOutputPath();
    if (StringUtil.isEmpty(outputPath)) {
      final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      descriptor.setTitle(CompilerBundle.message("dialog.title.output.directory.for.artifact"));
      descriptor.setDescription(CompilerBundle.message("chooser.description.select.output.directory.for.0.artifact", myArtifact.getName()));
      final FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, myProject);
      final VirtualFile[] files = chooser.choose(null, myProject);
      if (files.length != 1) return;
      final ModifiableArtifactModel model = ArtifactManager.getInstance(myProject).createModifiableModel();
      model.getOrCreateModifiableArtifact(myArtifact).setOutputPath(files[0].getPath());
      new WriteAction() {
        protected void run(final Result result) {
          model.commit();
        }
      }.execute();
    }
    CompilerManager.getInstance(myProject).make(ArtifactCompileScope.create(myProject, myArtifact), null);
  }
}
