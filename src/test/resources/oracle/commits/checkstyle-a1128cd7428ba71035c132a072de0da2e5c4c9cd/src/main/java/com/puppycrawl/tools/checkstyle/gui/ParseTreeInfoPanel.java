////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import antlr.ANTLRException;

import com.puppycrawl.tools.checkstyle.TreeWalker;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;

/**
 * Displays information about a parse tree.
 * The user can change the file that is parsed and displayed
 * through a JFileChooser.
 *
 * @author Lars Kühne
 */
public class ParseTreeInfoPanel extends JPanel {
    /** For Serialisation that will never happen. */
    private static final long serialVersionUID = -4243405131202059043L;

    /** Parse tree model. */
    private final transient ParseTreeModel parseTreeModel;
    /** JTextArea component. */
    private final JTextArea textArea;
    /** Last directory. */
    private File lastDirectory;
    /** Current file. */
    private File currentFile;
    /** Reload action. */
    private final ReloadAction reloadAction;
    /** Lines to position map. */
    private final List<Integer> linesToPosition = new ArrayList<>();

    /**
     * Create a new ParseTreeInfoPanel instance.
     */
    public ParseTreeInfoPanel() {
        setLayout(new BorderLayout());

        parseTreeModel = new ParseTreeModel(null);
        final JTreeTable treeTable = new JTreeTable(parseTreeModel);
        final JScrollPane sp = new JScrollPane(treeTable);
        add(sp, BorderLayout.PAGE_START);

        final JButton fileSelectionButton =
            new JButton(new FileSelectionAction());

        reloadAction = new ReloadAction();
        reloadAction.setEnabled(false);
        final JButton reloadButton = new JButton(reloadAction);

        textArea = new JTextArea(20, 15);
        textArea.setEditable(false);
        treeTable.setEditor(textArea);
        treeTable.setLinePositionMap(linesToPosition);

        final JScrollPane sp2 = new JScrollPane(textArea);
        add(sp2, BorderLayout.CENTER);

        final JPanel p = new JPanel(new GridLayout(1, 2));
        add(p, BorderLayout.PAGE_END);
        p.add(fileSelectionButton);
        p.add(reloadButton);

        try {
            new FileDrop(sp, new FileDropListener(sp));
        }
        catch (final TooManyListenersException ignored) {
            showErrorDialog(null, "Cannot initialize Drag and Drop support");
        }

    }

    /**
     * Opens the input parse tree ast.
     * @param parseTree DetailAST tree.
     */
    public void openAst(DetailAST parseTree) {
        parseTreeModel.setParseTree(parseTree);
        reloadAction.setEnabled(true);

        // clear for each new file
        clearLinesToPosition();
        // starts line counting at 1
        addLineToPosition(0);
        // insert the contents of the file to the text area

        // clean the text area before inserting the lines of the new file
        if (!textArea.getText().isEmpty()) {
            textArea.replaceRange("", 0, textArea.getText().length());
        }

        // move back to the top of the file
        textArea.moveCaretPosition(0);
    }

    /**
     * Opens file and loads it into text area.
     * @param file File to open.
     * @param parent Component for displaying errors if file can't be open.
     */
    public void openFile(File file, final Component parent) {
        if (file != null) {
            try {
                Main.getFrame().setTitle("Checkstyle : " + file.getName());
                final FileText text = new FileText(file.getAbsoluteFile(),
                                                   getEncoding());
                final DetailAST parseTree = parseFile(text);
                parseTreeModel.setParseTree(parseTree);
                currentFile = file;
                lastDirectory = file.getParentFile();
                reloadAction.setEnabled(true);

                final String[] sourceLines = text.toLinesArray();

                // clear for each new file
                clearLinesToPosition();
                // starts line counting at 1
                addLineToPosition(0);

                //clean the text area before inserting the lines of the new file
                if (!textArea.getText().isEmpty()) {
                    textArea.replaceRange("", 0, textArea.getText()
                            .length());
                }

                // insert the contents of the file to the text area
                for (final String element : sourceLines) {
                    addLineToPosition(textArea.getText().length());
                    textArea.append(element + System.lineSeparator());
                }

                // move back to the top of the file
                textArea.moveCaretPosition(0);
            }
            catch (final IOException | ANTLRException ex) {
                showErrorDialog(
                        parent,
                        "Could not parse" + file + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Parses a file and returns the parse tree.
     * @param text the file to parse
     * @return the root node of the parse tree
     * @throws ANTLRException if the file is not a Java source
     */
    private static DetailAST parseFile(FileText text)
        throws ANTLRException {
        final FileContents contents = new FileContents(text);
        return TreeWalker.parse(contents);
    }

    /**
     * Returns the configured file encoding.
     * This can be set using the {@code file.encoding} system property.
     * It defaults to UTF-8.
     * @return the configured file encoding
     */
    private static String getEncoding() {
        return System.getProperty("file.encoding", "UTF-8");
    }

    /**
     * Opens dialog with error.
     * @param parent Component for displaying errors.
     * @param msg Error message to display.
     */
    private static void showErrorDialog(final Component parent, final String msg) {
        final Runnable showError = new FrameShower(parent, msg);
        SwingUtilities.invokeLater(showError);
    }

    /**
     * Adds a new value into lines to position map.
     * @param value Value to be added into position map.
     */
    private void addLineToPosition(int value) {
        linesToPosition.add(value);
    }

    /** Clears lines to position map. */
    private void clearLinesToPosition() {
        linesToPosition.clear();
    }

    /**
     * Http://findbugs.sourceforge.net/bugDescriptions.html#SW_SWING_METHODS_INVOKED_IN_SWING_THREAD
     */
    private static class FrameShower implements Runnable {
        /**
         * Frame
         */
        private final Component parent;

        /**
         * Frame
         */
        private final String msg;

        /**
         * @param parent Frame's component.
         * @param msg Message to show.
         */
        FrameShower(Component parent, final String msg) {
            this.parent = parent;
            this.msg = msg;
        }

        /**
         * Display a frame
         */
        @Override
        public void run() {
            JOptionPane.showMessageDialog(parent, msg);
        }
    }

    private static class JavaFileFilter extends FileFilter {
        @Override
        public boolean accept(File file) {
            if (file == null) {
                return false;
            }
            return file.isDirectory() || file.getName().endsWith(".java");
        }

        @Override
        public String getDescription() {
            return "Java Source Code";
        }
    }

    private class FileSelectionAction extends AbstractAction {
        /**
         *
         */
        private static final long serialVersionUID = -1926935338069418119L;

        /** Defalut constructor to setup current action. */
        FileSelectionAction() {
            super("Select Java File");
            putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser(lastDirectory);
            final FileFilter filter = new JavaFileFilter();
            fc.setFileFilter(filter);
            final Component parent =
                SwingUtilities.getRoot(ParseTreeInfoPanel.this);
            fc.showDialog(parent, "Open");
            final File file = fc.getSelectedFile();
            openFile(file, parent);

        }
    }

    private class ReloadAction extends AbstractAction {
        /**
         *
         */
        private static final long serialVersionUID = -1021880396046355863L;

        /** Defalut constructor to setup current action. */
        ReloadAction() {
            super("Reload Java File");
            putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Component parent =
                SwingUtilities.getRoot(ParseTreeInfoPanel.this);
            openFile(currentFile, parent);
        }
    }

    private class FileDropListener implements FileDrop.Listener {
        /** Scroll pane. */
        private final JScrollPane scrollPane;

        /**
         * @param scrollPane Scroll pane.
         */
        FileDropListener(JScrollPane scrollPane) {
            this.scrollPane = scrollPane;
        }

        @Override
        public void filesDropped(File... files) {
            if (files != null && files.length > 0) {
                final File file = files[0];
                openFile(file, scrollPane);
            }
        }
    }
}
