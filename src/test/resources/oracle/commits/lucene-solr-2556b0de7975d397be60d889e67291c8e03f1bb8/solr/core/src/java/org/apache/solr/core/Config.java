/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.core;

import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrException;
import org.apache.solr.util.DOMUtil;
import org.apache.solr.util.SystemIdResolver;
import org.apache.solr.common.util.XMLErrorLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.commons.io.IOUtils;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class Config {
  public static final Logger log = LoggerFactory.getLogger(Config.class);
  private static final XMLErrorLogger xmllog = new XMLErrorLogger(log);

  static final XPathFactory xpathFactory = XPathFactory.newInstance();

  private final Document doc;
  private final String prefix;
  private final String name;
  private final SolrResourceLoader loader;

  /**
   * Builds a config from a resource name with no xpath prefix.
   * @param loader
   * @param name
   * @throws javax.xml.parsers.ParserConfigurationException
   * @throws java.io.IOException
   * @throws org.xml.sax.SAXException
   */
  public Config(SolrResourceLoader loader, String name) throws ParserConfigurationException, IOException, SAXException 
  {
    this( loader, name, null, null );
  }

  
  public Config(SolrResourceLoader loader, String name, InputSource is, String prefix) throws ParserConfigurationException, IOException, SAXException 
  {
    this(loader, name, is, prefix, true);
  }
  /**
   * Builds a config:
   * <p>
   * Note that the 'name' parameter is used to obtain a valid input stream if no valid one is provided through 'is'.
   * If no valid stream is provided, a valid SolrResourceLoader instance should be provided through 'loader' so
   * the resource can be opened (@see SolrResourceLoader#openResource); if no SolrResourceLoader instance is provided, a default one
   * will be created.
   * </p>
   * <p>
   * Consider passing a non-null 'name' parameter in all use-cases since it is used for logging & exception reporting.
   * </p>
   * @param loader the resource loader used to obtain an input stream if 'is' is null
   * @param name the resource name used if the input stream 'is' is null
   * @param is the resource as a SAX InputSource
   * @param prefix an optional prefix that will be preprended to all non-absolute xpath expressions
   * @throws javax.xml.parsers.ParserConfigurationException
   * @throws java.io.IOException
   * @throws org.xml.sax.SAXException
   */
  public Config(SolrResourceLoader loader, String name, InputSource is, String prefix, boolean subProps) throws ParserConfigurationException, IOException, SAXException 
  {
    if( loader == null ) {
      loader = new SolrResourceLoader( null );
    }
    this.loader = loader;
    this.name = name;
    this.prefix = (prefix != null && !prefix.endsWith("/"))? prefix + '/' : prefix;
    try {
      javax.xml.parsers.DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      
      if (is == null) {
        is = new InputSource(loader.openConfig(name));
        is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(name));
      }

      // only enable xinclude, if a SystemId is available
      if (is.getSystemId() != null) {
        try {
          dbf.setXIncludeAware(true);
          dbf.setNamespaceAware(true);
        } catch(UnsupportedOperationException e) {
          log.warn(name + " XML parser doesn't support XInclude option");
        }
      }
      
      final DocumentBuilder db = dbf.newDocumentBuilder();
      db.setEntityResolver(new SystemIdResolver(loader));
      db.setErrorHandler(xmllog);
      try {
        doc = db.parse(is);
      } finally {
        // some XML parsers are broken and don't close the byte stream (but they should according to spec)
        IOUtils.closeQuietly(is.getByteStream());
      }
      if (subProps) {
        DOMUtil.substituteProperties(doc, loader.getCoreProperties());
      }
    } catch (ParserConfigurationException e)  {
      SolrException.log(log, "Exception during parsing file: " + name, e);
      throw e;
    } catch (SAXException e)  {
      SolrException.log(log, "Exception during parsing file: " + name, e);
      throw e;
    } catch( SolrException e ){
      SolrException.log(log,"Error in "+name,e);
      throw e;
    }
  }
  
  public Config(SolrResourceLoader loader, String name, Document doc) {
    this.prefix = null;
    this.doc = doc;
    this.name = name;
    this.loader = loader;
  }

  /**
   * @since solr 1.3
   */
  public SolrResourceLoader getResourceLoader()
  {
    return loader;
  }

  /**
   * @since solr 1.3
   */
  public String getResourceName() {
    return name;
  }

  public String getName() {
    return name;
  }
  
  public Document getDocument() {
    return doc;
  }

  public XPath getXPath() {
    return xpathFactory.newXPath();
  }

  private String normalize(String path) {
    return (prefix==null || path.startsWith("/")) ? path : prefix+path;
  }
  
  public void substituteProperties() {
    DOMUtil.substituteProperties(doc, loader.getCoreProperties());
  }


  public Object evaluate(String path, QName type) {
    XPath xpath = xpathFactory.newXPath();
    try {
      String xstr=normalize(path);

      // TODO: instead of prepending /prefix/, we could do the search rooted at /prefix...
      Object o = xpath.evaluate(xstr, doc, type);
      return o;

    } catch (XPathExpressionException e) {
      throw new SolrException( SolrException.ErrorCode.SERVER_ERROR,"Error in xpath:" + path +" for " + name,e);
    }
  }

  public Node getNode(String path, boolean errIfMissing) {
   XPath xpath = xpathFactory.newXPath();
   Node nd = null;
   String xstr = normalize(path);

    try {
      nd = (Node)xpath.evaluate(xstr, doc, XPathConstants.NODE);

      if (nd==null) {
        if (errIfMissing) {
          throw new RuntimeException(name + " missing "+path);
        } else {
          log.debug(name + " missing optional " + path);
          return null;
        }
      }

      log.trace(name + ":" + path + "=" + nd);
      return nd;

    } catch (XPathExpressionException e) {
      SolrException.log(log,"Error in xpath",e);
      throw new SolrException( SolrException.ErrorCode.SERVER_ERROR,"Error in xpath:" + xstr + " for " + name,e);
    } catch (SolrException e) {
      throw(e);
    } catch (Throwable e) {
      SolrException.log(log,"Error in xpath",e);
      throw new SolrException( SolrException.ErrorCode.SERVER_ERROR,"Error in xpath:" + xstr+ " for " + name,e);
    }
  }

  public String getVal(String path, boolean errIfMissing) {
    Node nd = getNode(path,errIfMissing);
    if (nd==null) return null;

    String txt = DOMUtil.getText(nd);

    log.debug(name + ' '+path+'='+txt);
    return txt;

    /******
    short typ = nd.getNodeType();
    if (typ==Node.ATTRIBUTE_NODE || typ==Node.TEXT_NODE) {
      return nd.getNodeValue();
    }
    return nd.getTextContent();
    ******/
  }


  public String get(String path) {
    return getVal(path,true);
  }

  public String get(String path, String def) {
    String val = getVal(path, false);
    if (val == null || val.length() == 0) {
      return def;
    }
    return val;
  }

  public int getInt(String path) {
    return Integer.parseInt(getVal(path, true));
  }

  public int getInt(String path, int def) {
    String val = getVal(path, false);
    return val!=null ? Integer.parseInt(val) : def;
  }

  public boolean getBool(String path) {
    return Boolean.parseBoolean(getVal(path, true));
  }

  public boolean getBool(String path, boolean def) {
    String val = getVal(path, false);
    return val!=null ? Boolean.parseBoolean(val) : def;
  }

  public float getFloat(String path) {
    return Float.parseFloat(getVal(path, true));
  }

  public float getFloat(String path, float def) {
    String val = getVal(path, false);
    return val!=null ? Float.parseFloat(val) : def;
  }


  public double getDouble(String path){
     return Double.parseDouble(getVal(path, true));
   }

   public double getDouble(String path, double def) {
     String val = getVal(path, false);
     return val!=null ? Double.parseDouble(val) : def;
   }
   
   public Version getLuceneVersion(String path) {
     return parseLuceneVersionString(getVal(path, true));
   }
   
   public Version getLuceneVersion(String path, Version def) {
     String val = getVal(path, false);
     return val!=null ? parseLuceneVersionString(val) : def;
   }
  
  private static final AtomicBoolean versionWarningAlreadyLogged = new AtomicBoolean(false);
  
  public static final Version parseLuceneVersionString(final String matchVersion) {
    final Version version;
    try {
      version = Version.parseLeniently(matchVersion);
    } catch (IllegalArgumentException iae) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
        "Invalid luceneMatchVersion '" + matchVersion +
        "', valid values are: " + Arrays.toString(Version.values()) +
        " or a string in format 'V.V'", iae);
    }
    
    if (version == Version.LUCENE_CURRENT && !versionWarningAlreadyLogged.getAndSet(true)) {
      log.warn(
        "You should not use LUCENE_CURRENT as luceneMatchVersion property: "+
        "if you use this setting, and then Solr upgrades to a newer release of Lucene, "+
        "sizable changes may happen. If precise back compatibility is important "+
        "then you should instead explicitly specify an actual Lucene version."
      );
    }
    
    return version;
  }
}
