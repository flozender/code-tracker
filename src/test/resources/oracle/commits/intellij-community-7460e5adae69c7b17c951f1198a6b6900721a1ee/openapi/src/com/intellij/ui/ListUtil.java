/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ListUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ui.ListUtil");

  public static abstract class Updatable {
    private final JButton myButton;
    private boolean myEnabled = true;

    public Updatable(JButton button) {
      myButton = button;
    }

    public void enable(boolean enable) {
      myEnabled = enable;
      update();
    }

    protected void setButtonEnabled(boolean enabled) {
      myButton.setEnabled(enabled && myEnabled);
    }

    protected abstract void update();
  }

  public static java.util.List removeSelectedItems(JList list) {
    return removeSelectedItems(list, null);
  }

  public static java.util.List removeIndices(JList list, int[] indices) {
    return removeIndices(list, indices, null);
  }

  public static <T> java.util.List<T> removeSelectedItems(JList list, Condition<T> condition) {
    int[] idxs = list.getSelectedIndices();
    return removeIndices(list, idxs, condition);
  }

  private static <T> java.util.List<T> removeIndices(JList list, int[] idxs, Condition<T> condition) {
    if (idxs.length == 0) {
      return new ArrayList<T>(0);
    }
    ListModel model = list.getModel();
    int firstSelectedIndex = idxs[0];
    ArrayList<T> removedItems = new ArrayList<T>();
    int deletedCount = 0;
    for (int idx = 0; idx < idxs.length; idx++) {
      int index = idxs[idx] - deletedCount;
      if (index < 0 || index >= model.getSize()) continue;
      T obj = (T)get(model, index);
      if (condition == null || condition.value(obj)) {
        removedItems.add(obj);
        remove(model, index);
        deletedCount++;
      }
    }
    if (model.getSize() == 0) {
      list.clearSelection();
    }
    else if (list.getSelectedValue() == null) {
      // if nothing remains selected, set selected row
      if (firstSelectedIndex >= model.getSize()){
        list.setSelectedIndex(model.getSize() - 1);
      }
      else{
        list.setSelectedIndex(firstSelectedIndex);
      }
    }
    return removedItems;
  }

  public static boolean canRemoveSelectedItems(JList list){
    return canRemoveSelectedItems(list, null);
  }

  public static boolean canRemoveSelectedItems(JList list, Condition applyable){
    ListModel model = list.getModel();
    int[] idxs = list.getSelectedIndices();
    if (idxs.length == 0) {
      return false;
    }

    for (int idx = 0; idx < idxs.length; idx++) {
      int index = idxs[idx];
      if (index < 0 || index >= model.getSize()) continue;
      Object obj = getExtensions(model).get(model, index);
      if (applyable == null || applyable.value(obj)) {
        return true;
      }
    }

    return false;
  }

  public static int moveSelectedItemsUp(JList list) {
    DefaultListModel model = (DefaultListModel)list.getModel();
    int[] indices = list.getSelectedIndices();
    if (!canMoveSelectedItemsUp(list)) return 0;
    for(int i = 0; i < indices.length; i++){
      int index = indices[i];
      Object temp = model.get(index);
      model.set(index, model.get(index - 1));
      model.set(index - 1, temp);
      list.removeSelectionInterval(index, index);
      list.addSelectionInterval(index - 1, index - 1);
    }
    Rectangle cellBounds = list.getCellBounds(indices[0] - 1, indices[indices.length - 1] - 1);
    if (cellBounds != null){
      list.scrollRectToVisible(cellBounds);
    }
    return indices.length;
  }

  public static boolean canMoveSelectedItemsUp(JList list) {
    int[] indices = list.getSelectedIndices();
    return indices.length > 0 && indices[0] > 0;
  }

  public static int moveSelectedItemsDown(JList list) {
    DefaultListModel model = (DefaultListModel)list.getModel();
    int[] indices = list.getSelectedIndices();
    if (!canMoveSelectedItemsDown(list)) return 0;
    for(int i = indices.length - 1; i >= 0 ; i--){
      int index = indices[i];
      Object temp = model.get(index);
      model.set(index, model.get(index + 1));
      model.set(index + 1, temp);
      list.removeSelectionInterval(index, index);
      list.addSelectionInterval(index + 1, index + 1);
    }
    Rectangle cellBounds = list.getCellBounds(indices[0] + 1, indices[indices.length - 1] + 1);
    if (cellBounds != null){
      list.scrollRectToVisible(cellBounds);
    }
    return indices.length;
  }

  public static boolean canMoveSelectedItemsDown(JList list) {
    ListModel model = list.getModel();
    int[] indices = list.getSelectedIndices();
    return indices.length > 0 && indices[indices.length - 1] < model.getSize() - 1;
  }

  public static Updatable addMoveUpListener(JButton button, final JList list) {
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        moveSelectedItemsUp(list);
        list.requestFocusInWindow();
      }
    });
    return disableWhenNoSelection(button, list);
  }


  public static Updatable addMoveDownListener(JButton button, final JList list) {
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        moveSelectedItemsDown(list);
        list.requestFocusInWindow();
      }
    });
    return disableWhenNoSelection(button, list);
  }

  public static Updatable addRemoveListener(final JButton button, final JList list) {
    return addRemoveListener(button, list, null);
  }

  public static Updatable addRemoveListener(final JButton button, final JList list, final RemoveNotification notification) {
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final java.util.List items = removeSelectedItems(list);
        if (notification != null)
          notification.itemsRemoved(items);
        list.requestFocusInWindow();
      }
    });
    class MyListSelectionListener extends Updatable implements ListSelectionListener {
      public MyListSelectionListener(JButton button) {
        super(button);
      }

      public void valueChanged(ListSelectionEvent e) {
        setButtonEnabled(canRemoveSelectedItems(list));
      }

      protected void update() {
        valueChanged(null);
      }
    }
    MyListSelectionListener listener = new MyListSelectionListener(button);
    list.getSelectionModel().addListSelectionListener(listener);
    listener.update();
    return listener;
  }

  private static Object get(ListModel model, int index) {
    return getExtensions(model).get(model, index);
  }

  private static void remove(ListModel model, int index) {
    getExtensions(model).remove(model, index);
  }

  public static Updatable disableWhenNoSelection(final JButton button, final JList list) {
    class MyListSelectionListener extends Updatable implements ListSelectionListener {
      public MyListSelectionListener(JButton button) {
        super(button);
      }

      public void valueChanged(ListSelectionEvent e) {
        setButtonEnabled((list.getSelectedIndex() != -1));
      }

      public void update() {
        valueChanged(null);
      }
    }
    MyListSelectionListener listener = new MyListSelectionListener(button);
    list.getSelectionModel().addListSelectionListener(listener);
    listener.update();
    return listener;
  }

  public static interface RemoveNotification<ItemType> {
    void itemsRemoved(List<ItemType> items);
  }

  private static ListModelExtension getExtensions(ListModel model) {
    if (model instanceof DefaultListModel) return DEFAULT_MODEL;
    if (model instanceof SortedListModel) return SORTED_MODEL;

    if (model == null) LOG.assertTrue(false);
    else LOG.assertTrue(false, "Unknown model class: " + model.getClass().getName());
    return null;
  }

  private static interface ListModelExtension<ModelType extends ListModel> {
    Object get(ModelType model, int index);
    void remove(ModelType model, int index);
  }

  private static ListModelExtension DEFAULT_MODEL = new ListModelExtension<DefaultListModel>() {
    public Object get(DefaultListModel model, int index) {
      return model.get(index);
    }

    public void remove(DefaultListModel model, int index) {
      model.remove(index);
    }
  };

  private static ListModelExtension SORTED_MODEL = new ListModelExtension<SortedListModel>() {
    public Object get(SortedListModel model, int index) {
      return model.get(index);
    }

    public void remove(SortedListModel model, int index) {
      model.remove(index);
    }
  };
}