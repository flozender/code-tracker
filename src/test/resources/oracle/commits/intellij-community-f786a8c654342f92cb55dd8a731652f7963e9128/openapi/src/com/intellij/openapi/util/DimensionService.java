/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.WindowManager;
import gnu.trove.TObjectIntHashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class represents map between strings and rectangles. It's intended to store
 * sizes of window, dialogs, etc.
 */
public class DimensionService implements JDOMExternalizable, ApplicationComponent {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.util.DimensionService");

  private final Map<String,Point> myKey2Location;
  private final Map<String,Dimension> myKey2Size;
  private final TObjectIntHashMap<String> myKey2ExtendedState;
  @NonNls private static final String EXTENDED_STATE = "extendedState";
  @NonNls private static final String KEY = "key";
  @NonNls private static final String STATE = "state";
  @NonNls private static final String ELEMENT_LOCATION = "location";
  @NonNls private static final String ELEMENT_SIZE = "size";
  @NonNls private static final String ATTRIBUTE_X = "x";
  @NonNls private static final String ATTRIBUTE_Y = "y";
  @NonNls private static final String ATTRIBUTE_WIDTH = "width";
  @NonNls private static final String ATTRIBUTE_HEIGHT = "height";

  public static DimensionService getInstance() {
    return ApplicationManager.getApplication().getComponent(DimensionService.class);
  }

  /** Invoked by reflection */
  private DimensionService(){
    myKey2Location=new HashMap<String, Point>();
    myKey2Size=new HashMap<String, Dimension>();
    myKey2ExtendedState = new TObjectIntHashMap<String>();
  }

  public void initComponent() {}

  public void disposeComponent() {}

  /**
   * @return point stored under the specified <code>key</code>. The method returns
   * <code>null</code> if there is no stored value under the <code>key</code>. If point
   * is outside of current screen bounds then the method returns <code>null</code>. It
   * properly works in multimonitor configuration.
   * @exception java.lang.IllegalArgumentException if <code>key</code> is <code>null</code>.
   */
  public synchronized Point getLocation(String key) {
    if(key==null){
      throw new IllegalArgumentException("key cannot be null");
    }
    Point point=myKey2Location.get(key);
    if(point!=null){
      WindowManager windowManager=WindowManager.getInstance();
      if(!windowManager.isInsideScreenBounds(point.x,point.y)){
        point=null;
      }
    }
    if(point!=null){
      return (Point)point.clone();
    }else{
      return null;
    }
  }

  /**
   * Store specified <code>point</code> under the <code>key</code>. If <code>point</code> is
   * <code>null</code> then the value stored under <code>key</code> will be removed.
   * @exception java.lang.IllegalArgumentException if <code>key</code> is <code>null</code>.
   */
  public synchronized void setLocation(String key, Point point){
    if(key==null){
      throw new IllegalArgumentException("key cannot be null");
    }
    if (point != null) {
      myKey2Location.put(key, (Point)point.clone());
    }else {
      myKey2Location.remove(key);
    }
  }

  /**
   * @return point stored under the specified <code>key</code>. The method returns
   * <code>null</code> if there is no stored value under the <code>key</code>.
   * @exception java.lang.IllegalArgumentException if <code>key</code> is <code>null</code>.
   */
  public synchronized Dimension getSize(@NonNls String key) {
    if(key==null){
      throw new IllegalArgumentException("key cannot be null");
    }
    Dimension size=myKey2Size.get(key);
    if(size!=null){
      return (Dimension)size.clone();
    }else{
      return null;
    }
  }

  /**
   * Store specified <code>size</code> under the <code>key</code>. If <code>size</code> is
   * <code>null</code> then the value stored under <code>key</code> will be removed.
   * @exception java.lang.IllegalArgumentException if <code>key</code> is <code>null</code>.
   */
  public synchronized void setSize(@NonNls String key, Dimension size){
    if(key==null){
      throw new IllegalArgumentException("key cannot be null");
    }
    if (size != null) {
      myKey2Size.put(key, (Dimension)size.clone());
    }else {
      myKey2Size.remove(key);
    }
  }

  public synchronized void readExternal(Element element) throws InvalidDataException {
    for (Iterator i = element.getChildren().iterator(); i.hasNext();) {
      Element e = (Element)i.next();
      if(ELEMENT_LOCATION.equals(e.getName())){
        try{
          myKey2Location.put(
            e.getAttributeValue(KEY),
            new Point(
              Integer.parseInt(e.getAttributeValue(ATTRIBUTE_X)),
              Integer.parseInt(e.getAttributeValue(ATTRIBUTE_Y))
            )
          );
        }catch (NumberFormatException ignored){}
      }
      else if(ELEMENT_SIZE.equals(e.getName())){
        try{
          myKey2Size.put(
            e.getAttributeValue(KEY),
            new Dimension(
              Integer.parseInt(e.getAttributeValue(ATTRIBUTE_WIDTH)),
              Integer.parseInt(e.getAttributeValue(ATTRIBUTE_HEIGHT))
            )
          );
        }catch (NumberFormatException ignored){}
      }
      else if(EXTENDED_STATE.equals(e.getName())) {
        try {
          myKey2ExtendedState.put(e.getAttributeValue(KEY), Integer.parseInt(e.getAttributeValue(STATE)));
        } catch(NumberFormatException ignored){}
      }
    }
  }

  public synchronized void writeExternal(Element element) throws WriteExternalException {
    // Save locations
    for(Iterator<String> i=myKey2Location.keySet().iterator();i.hasNext();){
      String key=i.next();
      Point point=myKey2Location.get(key);
      LOG.assertTrue(point!=null);
      Element e=new Element(ELEMENT_LOCATION);
      e.setAttribute(KEY,key);
      e.setAttribute(ATTRIBUTE_X, String.valueOf(point.x));
      e.setAttribute(ATTRIBUTE_Y, String.valueOf(point.y));
      element.addContent(e);
    }
    // Save sizes
    for (Iterator<String> i = myKey2Size.keySet().iterator(); i.hasNext();) {
      String key = i.next();
      Dimension size = myKey2Size.get(key);
      LOG.assertTrue(size!=null);
      Element e = new Element(ELEMENT_SIZE);
      e.setAttribute(KEY,key);
      e.setAttribute(ATTRIBUTE_WIDTH, String.valueOf(size.width));
      e.setAttribute(ATTRIBUTE_HEIGHT, String.valueOf(size.height));
      element.addContent(e);
    }
    // Save extended states
    for (int i = 0; i < myKey2ExtendedState.keys().length; i++) {
      String key = (String)myKey2ExtendedState.keys()[i];
      Element e = new Element(EXTENDED_STATE);
      e.setAttribute(KEY, key);
      e.setAttribute(STATE, String.valueOf(myKey2ExtendedState.get(key)));
      element.addContent(e);
    }
  }

  public String getComponentName() {
    return "DimensionService";
  }

  public void setExtendedState(String key, int extendedState) {
    myKey2ExtendedState.put(key, extendedState);
  }

  public int getExtendedState(String key) {
    if (!myKey2ExtendedState.containsKey(key)) return -1;
    return myKey2ExtendedState.get(key);
  }
}
