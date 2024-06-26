package com.intellij.html.preview;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.css.impl.util.CssUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ArrayUtil;
import com.intellij.xml.util.ColorSampleLookupValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author spleaner
 */
public class ColorPreviewComponent extends JComponent {
  private final Color myColor;

  private ColorPreviewComponent(final String hexValue, final Color color) {
    myColor = color;
    setOpaque(true);

/*    if (hexValue != null) {
      final JLabel label = new JLabel('#' + hexValue);
      label.setFont(UIUtil.getToolTipFont());
      label.setForeground(Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2] >= 0.5f ? Color.BLACK : Color.WHITE);
      add(label, BorderLayout.SOUTH);
    } */
  }

  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D)g;

    final Rectangle r = getBounds();

    g2.setPaint(myColor);
    g2.fillRect(1, 1, r.width - 2, r.height - 2);

    g2.setPaint(Color.BLACK);
    g2.drawRect(0, 0, r.width - 1, r.height - 1);
  }

  public Dimension getPreferredSize() {
    return new Dimension(70, 25);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Nullable
  public static JComponent getPreviewComponent(@NotNull final PsiElement element) {
    final PsiNewExpression psiNewExpression = PsiTreeUtil.getParentOfType(element, PsiNewExpression.class);

    if (psiNewExpression != null) {
      final PsiJavaCodeReferenceElement referenceElement = PsiTreeUtil.getChildOfType(psiNewExpression, PsiJavaCodeReferenceElement.class);

      if (referenceElement != null) {
        final PsiReference reference = referenceElement.getReference();

        if (reference != null) {
          final PsiElement psiElement = reference.resolve();

          if (psiElement instanceof PsiClass && "java.awt.Color".equals(((PsiClass)psiElement).getQualifiedName())) {
            final PsiExpressionList argumentList = psiNewExpression.getArgumentList();

            if (argumentList != null) {
              final PsiExpression[] expressions = argumentList.getExpressions();
              int[] values = ArrayUtil.newIntArray(expressions.length);
              float[] values2 = new float[expressions.length];
              int i = 0;
              int j = 0;

              final PsiConstantEvaluationHelper helper = JavaPsiFacade.getInstance(element.getProject()).getConstantEvaluationHelper();
              for (final PsiExpression each : expressions) {
                final Object o = helper.computeConstantExpression(each);
                if (o instanceof Integer) {
                  values[i] = ((Integer)o).intValue();
                  values[i] = values[i] > 255 ? 255 : values[i] < 0 ? 0 : values[i];
                  i++;
                }
                else if (o instanceof Float) {
                  values2[j] = ((Float)o).floatValue();
                  values2[j] = values2[j] > 1 ? 1 : values2[j] < 0 ? 0 : values2[j];
                  j++;
                }
              }


              Color c = null;
              if (i == expressions.length) {
                switch (values.length) {
                  case 1:
                    c = new Color(values[0]);
                    break;
                  case 3:
                    c = new Color(values[0], values[1], values[2]);
                    break;
                  case 4:
                    c = new Color(values[0], values[1], values[2], values[3]);
                    break;
                  default:
                    break;
                }
              }
              else if (j == expressions.length) {
                switch (values2.length) {
                  case 3:
                    c = new Color(values2[0], values2[1], values2[2]);
                    break;
                  case 4:
                    c = new Color(values2[0], values2[1], values2[2], values2[3]);
                    break;
                  default:
                    break;
                }
              }

              if (c != null) {
                return new ColorPreviewComponent(null, c);
              }
            }
          }
        }
      }
    }

    if (PlatformPatterns.psiElement(PsiIdentifier.class).withParent(PlatformPatterns.psiElement(PsiReferenceExpression.class))
      .accepts(element)) {
      final PsiReference reference = element.getParent().getReference();

      if (reference != null) {
        final PsiElement psiElement = reference.resolve();

        if (psiElement instanceof PsiField) {
          if ("java.awt.Color".equals(((PsiField)psiElement).getContainingClass().getQualifiedName())) {
            final String colorName = ((PsiField)psiElement).getName().toLowerCase().replace("_", "");
            final String hex = ColorSampleLookupValue.getHexCodeForColorName(colorName);
            return new ColorPreviewComponent(null, Color.decode("0x" + hex.substring(1)));
          }
        }
      }
    }


    if (element.getParent() instanceof XmlAttributeValue) {
      final PsiElement parentParent = element.getParent().getParent();

      if (parentParent instanceof XmlAttribute) {
        final XmlAttribute attribute = (XmlAttribute)parentParent;
        final String attrName = attribute.getName().toLowerCase();

        if ("alink".equals(attrName) ||
            "link".equals(attrName) ||
            "text".equals(attrName) ||
            "vlink".equals(attrName) ||
            attrName.indexOf("color") >= 0) {
          String s = element.getText();  // TODO: support [#FFF, #FFF]

          if (s.length() > 0) {
            final String hexColor = (s.charAt(0) == '#') ? s : ColorSampleLookupValue.getHexCodeForColorName(s.toLowerCase());
            if (hexColor != null) {
              try {
                return new ColorPreviewComponent(null, Color.decode("0x" + hexColor.substring(1)));
              }
              catch (NumberFormatException e) {
                return null;
              }
            }
          }
        }
      }
    }
    else {
      final Color color = CssUtil.getColor(element);
      if (color != null) {
        try {
          return new ColorPreviewComponent(null, color);
        }
        catch (NumberFormatException e) {
          return null;
        }
      }
    }

    return null;
  }
}
