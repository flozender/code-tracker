package com.intellij.codeEditor.printing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.containers.HashMap;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class PageSizes {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeEditor.printing.PageSizes");
  private static ArrayList myPageSizes = null;
  private static HashMap myNamesToPageSizes = null;
  private static final double MM_TO_INCH = 1/25.4;
  @NonNls private static final String PAGE_SIZES_RESOURCE = "/PageSizes.xml";
  @NonNls private static final String ELEMENT_SIZE = "size";
  @NonNls private static final String ATTRIBUTE_NAME = "name";
  @NonNls private static final String ATTRIBUTE_WIDTH = "width";
  @NonNls private static final String ATTRIBUTE_HEIGHT = "height";
  @NonNls private static final String ATTRIBUTE_UNIT = "unit";
  @NonNls private static final String UNIT_MM = "mm";

  public static String[] getNames() {
    init();
    String[] ret = new String[myPageSizes.size()];
    for(int i = 0; i < myPageSizes.size(); i++) {
      PageSize pageSize = (PageSize)myPageSizes.get(i);
      ret[i] = pageSize.name;
    }
    return ret;
  }

  public static Object getItem(String name) {
    init();
    return myNamesToPageSizes.get(name);
  }

  public static double getWidth(String name) {
    init();
    PageSize pageSize = (PageSize)myNamesToPageSizes.get(name);
    if(pageSize == null) {
      return 0;
    }
    return pageSize.width;
  }

  public static double getHeight(String name) {
    init();
    PageSize pageSize = (PageSize)myNamesToPageSizes.get(name);
    if(pageSize == null) {
      return 0;
    }
    return pageSize.height;
  }

  public static String getName(Object item) {
    init();
    if(!(item instanceof PageSize)) {
      return null;
    }
    PageSize pageSize = (PageSize)item;
    return pageSize.name;
  }

  private static void addPageSizeIn(String name, String dimensions, double width, double height) {
    PageSize pageSize = new PageSize();
    pageSize.name = name;
    pageSize.visualName = name + "    (" + dimensions + ")";
    pageSize.width = width;
    pageSize.height = height;
    myPageSizes.add(pageSize);
    myNamesToPageSizes.put(pageSize.name, pageSize);
  }

  private static void init() {
    if(myPageSizes != null) {
      return;
    }
    myPageSizes = new ArrayList();
    myNamesToPageSizes = new HashMap();

    try {
      final Document document = JDOMUtil.loadDocument(PageSizes.class.getResourceAsStream(PAGE_SIZES_RESOURCE));
      final List elements = document.getRootElement().getChildren(ELEMENT_SIZE);
      for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
        final Element element = (Element) iterator.next();
        String name = element.getAttributeValue(ATTRIBUTE_NAME);
        final String widthStr = element.getAttributeValue(ATTRIBUTE_WIDTH);
        final String heightStr = element.getAttributeValue(ATTRIBUTE_HEIGHT);
        String unit = element.getAttributeValue(ATTRIBUTE_UNIT);

        final String unitName = unit.equals(UNIT_MM)
                                ? CodeEditorBundle.message("print.page.size.unit.mm")
                                : CodeEditorBundle.message("print.page.size.unit.in");
        final String dimensions = CodeEditorBundle.message("print.page.width.x.height.unit.template",
                                                           widthStr, heightStr, unitName);

        double width = parsePageSize(widthStr);
        double height = parsePageSize(heightStr);
        if (unit.equals(UNIT_MM)) {
          width *= MM_TO_INCH;
          height *= MM_TO_INCH;
        }
        addPageSizeIn(name, dimensions, width, height);
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private static double parsePageSize(final String sizeStr) {
    int slashPos = sizeStr.indexOf('/');
    if (slashPos >= 0) {
      int spacePos = sizeStr.indexOf(' ');
      int intPart = Integer.valueOf(sizeStr.substring(0, spacePos));
      double numerator = Double.valueOf(sizeStr.substring(spacePos+1, slashPos));
      double denominator = Double.valueOf(sizeStr.substring(slashPos+1));
      return intPart + numerator / denominator;
    }
    return Integer.valueOf(sizeStr);
  }

  private static class PageSize {
    public double width;
    public double height;
    public String name;
    public String visualName;

    public String toString() {
      return visualName;
    }
  }
}