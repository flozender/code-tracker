/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.keymap;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.util.registry.RegistryValueListener;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class KeymapUtil {

  @NonNls private static final String APPLE_LAF_AQUA_LOOK_AND_FEEL_CLASS_NAME = "apple.laf.AquaLookAndFeel";
  @NonNls private static final String GET_KEY_MODIFIERS_TEXT_METHOD = "getKeyModifiersText";
  @NonNls private static final String CANCEL_KEY_TEXT = "Cancel";
  @NonNls private static final String BREAK_KEY_TEXT = "Break";
  @NonNls private static final String SHIFT = "shift";
  @NonNls private static final String CONTROL = "control";
  @NonNls private static final String CTRL = "ctrl";
  @NonNls private static final String META = "meta";
  @NonNls private static final String ALT = "alt";
  @NonNls private static final String ALT_GRAPH = "altGraph";
  @NonNls private static final String DOUBLE_CLICK = "doubleClick";

  private static final Set<Integer> ourTooltipKeys = new HashSet<Integer>();
  private static final Set<Integer> ourOtherTooltipKeys = new HashSet<Integer>();
  private static RegistryValue ourTooltipKeysProperty;

  private KeymapUtil() {
  }

  public static String getShortcutText(Shortcut shortcut) {
    String s = "";

    if (shortcut instanceof KeyboardShortcut) {
      KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;

      String acceleratorText = getKeystrokeText(keyboardShortcut.getFirstKeyStroke());
      if (!acceleratorText.isEmpty()) {
        s = acceleratorText;
      }

      acceleratorText = getKeystrokeText(keyboardShortcut.getSecondKeyStroke());
      if (!acceleratorText.isEmpty()) {
        s += ", " + acceleratorText;
      }
    }
    else if (shortcut instanceof MouseShortcut) {
      MouseShortcut mouseShortcut = (MouseShortcut)shortcut;
      s = getMouseShortcutText(mouseShortcut.getButton(), mouseShortcut.getModifiers(), mouseShortcut.getClickCount());
    }
    else if (shortcut instanceof KeyboardModifierGestureShortcut) {
      final KeyboardModifierGestureShortcut gestureShortcut = (KeyboardModifierGestureShortcut)shortcut;
      s = gestureShortcut.getType() == KeyboardGestureAction.ModifierType.dblClick ? "Press, release and hold " : "Hold ";
      s += getKeystrokeText(gestureShortcut.getStroke());
    }
    else {
      throw new IllegalArgumentException("unknown shortcut class: " + shortcut.getClass().getCanonicalName());
    }
    return s;
  }

  public static Icon getShortcutIcon(Shortcut shortcut) {
    if (shortcut instanceof KeyboardShortcut) {
      return AllIcons.General.KeyboardShortcut;
    }
    else if (shortcut instanceof MouseShortcut) {
      return AllIcons.General.MouseShortcut;
    }
    else {
      throw new IllegalArgumentException("unknown shortcut class: " + shortcut);
    }
  }

  /**
   * @param button        target mouse button
   * @param modifiers     modifiers used within the target click
   * @param clickCount    target clicks count
   * @return string representation of passed mouse shortcut.
   */
  public static String getMouseShortcutText(int button, @JdkConstants.InputEventMask int modifiers, int clickCount) {
    if (clickCount < 3) {
      return KeyMapBundle.message("mouse." + (clickCount == 1? "" : "double.") + "click.shortcut.text", getModifiersText(mapNewModifiers(modifiers)), button);
    } else {
      throw new IllegalStateException("unknown clickCount: " + clickCount);
    }
  }

  @JdkConstants.InputEventMask
  private static int mapNewModifiers(@JdkConstants.InputEventMask int modifiers) {
    if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
      modifiers |= InputEvent.SHIFT_MASK;
    }
    if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
      modifiers |= InputEvent.ALT_MASK;
    }
    if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
      modifiers |= InputEvent.ALT_GRAPH_MASK;
    }
    if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
      modifiers |= InputEvent.CTRL_MASK;
    }
    if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
      modifiers |= InputEvent.META_MASK;
    }

    return modifiers;
  }

  public static String getKeystrokeText(KeyStroke accelerator) {
    if (accelerator == null) return "";
    String acceleratorText = "";
    int modifiers = accelerator.getModifiers();
    if (modifiers > 0) {
      acceleratorText = getModifiersText(modifiers);
    }

    String keyText = KeyEvent.getKeyText(accelerator.getKeyCode());
    // [vova] this is dirty fix for bug #35092 
    if(CANCEL_KEY_TEXT.equals(keyText)){
      keyText = BREAK_KEY_TEXT;
    }

    acceleratorText += keyText;
    return acceleratorText.trim();
  }

  private static String getModifiersText(@JdkConstants.InputEventMask int modifiers) {
    if (SystemInfo.isMac) {
      try {
        Class appleLaf = Class.forName(APPLE_LAF_AQUA_LOOK_AND_FEEL_CLASS_NAME);
        Method getModifiers = appleLaf.getMethod(GET_KEY_MODIFIERS_TEXT_METHOD, int.class, boolean.class);
        return (String)getModifiers.invoke(appleLaf, modifiers, Boolean.FALSE);
      }
      catch (Exception e) {
        if (SystemInfo.isMacOSLeopard) {
          return getKeyModifiersTextForMacOSLeopard(modifiers);
        }
        
        // OK do nothing here.
      }
    }

    final String keyModifiersText = KeyEvent.getKeyModifiersText(modifiers);
    if (!keyModifiersText.isEmpty()) {
      return keyModifiersText + "+";
    } else {
      return keyModifiersText;
    }
  }

  public static String getFirstKeyboardShortcutText(AnAction action) {
    Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
    for (Shortcut shortcut : shortcuts) {
      if (shortcut instanceof KeyboardShortcut) {
        return getShortcutText(shortcut);
      }
    }
    return "";
  }

  public static String getShortcutsText(Shortcut[] shortcuts) {
    if (shortcuts.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < shortcuts.length; i++) {
      Shortcut shortcut = shortcuts[i];
      if (i > 0) {
        buffer.append(' ');
      }
      buffer.append(getShortcutText(shortcut));
    }
    return buffer.toString();
  }

  /**
   * Factory method. It parses passed string and creates <code>MouseShortcut</code>.
   *
   * @param keystrokeString       target keystroke
   * @return                      shortcut for the given keystroke
   * @throws InvalidDataException if <code>keystrokeString</code> doesn't represent valid <code>MouseShortcut</code>.
   */
  public static MouseShortcut parseMouseShortcut(String keystrokeString) throws InvalidDataException {
    int button = -1;
    int modifiers = 0;
    int clickCount = 1;
    for (StringTokenizer tokenizer = new StringTokenizer(keystrokeString); tokenizer.hasMoreTokens();) {
      String token = tokenizer.nextToken();
      if (SHIFT.equals(token)) {
        modifiers |= InputEvent.SHIFT_DOWN_MASK;
      }
      else if (CONTROL.equals(token) || CTRL.equals(token)) {
        modifiers |= InputEvent.CTRL_DOWN_MASK;
      }
      else if (META.equals(token)) {
        modifiers |= InputEvent.META_DOWN_MASK;
      }
      else if (ALT.equals(token)) {
        modifiers |= InputEvent.ALT_DOWN_MASK;
      }
      else if (ALT_GRAPH.equals(token)) {
        modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
      }
      else if (token.startsWith("button") && token.length() > 6) {
        try {
          button = Integer.parseInt(token.substring(6));
        }
        catch (NumberFormatException e) {
          throw new InvalidDataException("unparseable token: " + token);
        }
      }
      else if (DOUBLE_CLICK.equals(token)) {
        clickCount = 2;
      }
      else {
        throw new InvalidDataException("unknown token: " + token);
      }
    }
    return new MouseShortcut(button, modifiers, clickCount);
  }

  public static String getKeyModifiersTextForMacOSLeopard(@JdkConstants.InputEventMask int modifiers) {
    StringBuilder buf = new StringBuilder();
      if ((modifiers & InputEvent.META_MASK) != 0) {
          buf.append(Toolkit.getProperty("AWT.meta", "Meta"));
      }
      if ((modifiers & InputEvent.CTRL_MASK) != 0) {
          buf.append(Toolkit.getProperty("AWT.control", "Ctrl"));
      }
      if ((modifiers & InputEvent.ALT_MASK) != 0) {
          buf.append(Toolkit.getProperty("AWT.alt", "Alt"));
      }
      if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
          buf.append(Toolkit.getProperty("AWT.shift", "Shift"));
      }
      if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
          buf.append(Toolkit.getProperty("AWT.altGraph", "Alt Graph"));
      }
      if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
          buf.append(Toolkit.getProperty("AWT.button1", "Button1"));
      }
      return buf.toString();
  }

  public static boolean isTooltipRequest(KeyEvent keyEvent) {
    if (ourTooltipKeysProperty == null) {
      ourTooltipKeysProperty = Registry.get("ide.forcedShowTooltip");
      ourTooltipKeysProperty.addListener(new RegistryValueListener.Adapter() {
        @Override
        public void afterValueChanged(RegistryValue value) {
          updateTooltipRequestKey(value);
        }
      }, Disposer.get("ui"));

      updateTooltipRequestKey(ourTooltipKeysProperty);
    }

    if (keyEvent.getID() != KeyEvent.KEY_PRESSED) return false;

    for (Integer each : ourTooltipKeys) {
      if ((keyEvent.getModifiers() & each.intValue()) == 0) return false;
    }

    for (Integer each : ourOtherTooltipKeys) {
      if ((keyEvent.getModifiers() & each.intValue()) > 0) return false;
    }

    final int code = keyEvent.getKeyCode();

    return code == KeyEvent.VK_META || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_ALT;
  }

  private static void updateTooltipRequestKey(RegistryValue value) {
    final String text = value.asString();

    ourTooltipKeys.clear();
    ourOtherTooltipKeys.clear();

    processKey(text.contains("meta"), InputEvent.META_MASK);
    processKey(text.contains("control") || text.contains("ctrl"), InputEvent.CTRL_MASK);
    processKey(text.contains("shift"), InputEvent.SHIFT_MASK);
    processKey(text.contains("alt"), InputEvent.ALT_MASK);

  }

  private static void processKey(boolean condition, int value) {
    if (condition) {
      ourTooltipKeys.add(value);
    } else {
      ourOtherTooltipKeys.add(value);
    }
  }

  public static boolean isEmacsKeymap() {
    return isEmacsKeymap(KeymapManager.getInstance().getActiveKeymap());
  }
  
  public static boolean isEmacsKeymap(@Nullable Keymap keymap) {
    for (; keymap != null; keymap = keymap.getParent()) {
      if ("Emacs".equalsIgnoreCase(keymap.getName())) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static KeyStroke getKeyStroke(@NotNull final ShortcutSet shortcutSet) {
    final Shortcut[] shortcuts = shortcutSet.getShortcuts();
    if (shortcuts.length == 0 || !(shortcuts[0] instanceof KeyboardShortcut)) return null;
    final KeyboardShortcut shortcut = (KeyboardShortcut)shortcuts[0];
    if (shortcut.getSecondKeyStroke() != null) {
      return null;
    }
    return shortcut.getFirstKeyStroke();
  }
}
