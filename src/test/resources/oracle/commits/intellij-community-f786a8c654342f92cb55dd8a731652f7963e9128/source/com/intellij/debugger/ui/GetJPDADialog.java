/*
 * @author: Eugene Zhuravlev
 * Date: Oct 16, 2002
 * Time: 5:31:46 PM
 */
package com.intellij.debugger.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.debugger.DebuggerBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.jetbrains.annotations.NonNls;

public class GetJPDADialog extends DialogWrapper {
  private static final @NonNls String JPDA_URL = "http://java.sun.com/products/jpda";

  public GetJPDADialog() {
    super(false);
    setTitle(DebuggerBundle.message("get.jpda.dialog.title"));
    setResizable(false);
    init();
  }

  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }

  protected JComponent createCenterPanel() {
    final JPanel _panel1 = new JPanel(new BorderLayout());

    JPanel _panel2 = new JPanel(new BorderLayout());
    _panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    //"Debug libraries are missig from JDK home.\nIn order for debugger to start, the libraries should be installed.\nPlease visit http://java.sun.com/products/jpda"
    JLabel label1 = new JLabel(DebuggerBundle.message("label.get.jpda.dialog.prompt"));
    //label1.setForeground(Color.black);
    JLabel label2 = new JLabel(JPDA_URL);
    label2.addMouseListener(
      new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          BrowserUtil.launchBrowser(JPDA_URL);
        }

      }
    );
    label2.setForeground(Color.blue.darker());
    label2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    _panel2.add(new JLabel(DebuggerBundle.message("label.get.jpda.dialog.error.description")), BorderLayout.NORTH);
    _panel2.add(label1, BorderLayout.WEST);
    _panel2.add(label2, BorderLayout.EAST);
    _panel1.add(_panel2, BorderLayout.NORTH);

    JPanel content = new JPanel(new GridLayout(2, 1, 10, 10));

    _panel1.add(content, BorderLayout.CENTER);
    return _panel1;
  }
}

