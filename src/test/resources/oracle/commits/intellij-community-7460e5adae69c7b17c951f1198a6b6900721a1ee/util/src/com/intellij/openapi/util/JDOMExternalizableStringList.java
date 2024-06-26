/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.openapi.util;

import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Element;
import sun.reflect.Reflection;

import java.util.ArrayList;
import java.util.Iterator;

public class JDOMExternalizableStringList extends ArrayList<String> implements JDOMExternalizable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.util.JDOMExternalizableStringList");

  private static final String ATTR_LIST = "list";
  private static final String ATTR_LISTSIZE = "size";
  private static final String ATTR_ITEM = "item";
  private static final String ATTR_INDEX = "index";
  private static final String ATTR_CLASS = "class";
  private static final String ATTR_VALUE = "itemvalue";

  public void readExternal(Element element) throws InvalidDataException {
    clear();

    for (Iterator i = element.getChildren().iterator(); i.hasNext();) {
      Element e = (Element)i.next();
      if (ATTR_LIST.equals(e.getName())) {
        Element listElement = e;
        String sizeString = listElement.getAttributeValue(ATTR_LISTSIZE);
        int listSize;
        try {
          listSize = Integer.parseInt(sizeString);
        }
        catch (NumberFormatException ex) {
          throw new InvalidDataException("Size " + sizeString + " found. Must be integer!");
        }
        for (int j = 0; j < listSize; j++) {
          this.add(null);
        }
        final ClassLoader classLoader = Reflection.getCallerClass(2).getClassLoader();
        for (Iterator listIterator = listElement.getChildren().iterator(); listIterator.hasNext();) {
          Element listItemElement = (Element)listIterator.next();
          if (!ATTR_ITEM.equals(listItemElement.getName())) {
            throw new InvalidDataException(
              "Unable to read list item. Unknown element found: " + listItemElement.getName());
          }
          String listItem;
          String itemIndexString = listItemElement.getAttributeValue(ATTR_INDEX);
          String itemClassString = listItemElement.getAttributeValue(ATTR_CLASS);
          Class itemClass;
          try {
            itemClass = Class.forName(itemClassString, true, classLoader);
          }
          catch (ClassNotFoundException ex) {
            throw new InvalidDataException(
              "Unable to read list item: unable to load class: " + itemClassString + " \n" + ex.getMessage());
          }

          listItem = listItemElement.getAttributeValue(ATTR_VALUE);

          LOG.assertTrue(String.class.equals(itemClass));

          this.set(Integer.parseInt(itemIndexString), listItem);
        }
      }
    }
  }

  public void writeExternal(Element element) throws WriteExternalException {
    int listSize = size();
    Element listElement = new Element(ATTR_LIST);
    listElement.setAttribute(ATTR_LISTSIZE, Integer.toString(listSize));
    element.addContent(listElement);
    for (int i = 0; i < listSize; i++) {
      String listItem = this.get(i);
      if (listItem != null) {
        Element itemElement = new Element(ATTR_ITEM);
        itemElement.setAttribute(ATTR_INDEX, Integer.toString(i));
        itemElement.setAttribute(ATTR_CLASS, listItem.getClass().getName());
        itemElement.setAttribute(ATTR_VALUE, listItem);
        listElement.addContent(itemElement);
      }
    }
  }
}
