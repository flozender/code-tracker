package com.intellij.ide.todo.configurable;

import com.intellij.psi.search.TodoPattern;
import com.intellij.util.ui.ItemRemovable;
import com.intellij.ide.IdeBundle;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

final class PatternsTableModel extends AbstractTableModel implements ItemRemovable{
  private final String[] ourColumnNames=new String[]{
    IdeBundle.message("column.todo.patterns.icon"),
    IdeBundle.message("column.todo.patterns.case.sensitive"),
    IdeBundle.message("column.todo.patterns.pattern")
  };
  private final Class[] ourColumnClasses=new Class[]{Icon.class,Boolean.class,String.class};

  private List<TodoPattern> myPatterns;

  public PatternsTableModel(List<TodoPattern> patterns){
    myPatterns=patterns;
  }

  public String getColumnName(int column){
    return ourColumnNames[column];
  }

  public Class getColumnClass(int column){
    return ourColumnClasses[column];
  }

  public int getColumnCount(){
    return 3;
  }

  public int getRowCount(){
    return myPatterns.size();
  }

  public Object getValueAt(int row,int column){
    TodoPattern pattern=myPatterns.get(row);
    switch(column){
      case 0:{ // "Icon" column
        return pattern.getAttributes().getIcon();
      }case 1:{ // "Case Sensitive" column
        return pattern.isCaseSensitive()?Boolean.TRUE:Boolean.FALSE;
      }case 2:{ // "Pattern" column
        return pattern.getPatternString();
      }default:{
        throw new IllegalArgumentException();
      }
    }
  }

  public void setValueAt(Object value,int row,int column){
    TodoPattern pattern=myPatterns.get(row);
    switch(column){
      case 0:{
        pattern.getAttributes().setIcon((Icon)value);
        break;
      }case 1:{
        pattern.setCaseSensitive(((Boolean)value).booleanValue());
        break;
      }case 2:{
        pattern.setPatternString(((String)value).trim());
        break;
      }default:{
        throw new IllegalArgumentException();
      }
    }
  }

  public void removeRow(int index){
    myPatterns.remove(index);
    fireTableRowsDeleted(index,index);
  }
}