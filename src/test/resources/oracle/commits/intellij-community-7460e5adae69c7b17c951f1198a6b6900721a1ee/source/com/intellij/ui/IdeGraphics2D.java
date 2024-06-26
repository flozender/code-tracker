package com.intellij.ui;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.util.Map;
import java.text.AttributedCharacterIterator;

/**
 * @author Vladimir Kondratyev
 */
public class IdeGraphics2D extends Graphics2D{
  private Graphics2D myDelegate;

  public IdeGraphics2D(Graphics2D g2d){
    myDelegate=g2d;
    setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  public void addRenderingHints(Map hints) {
    myDelegate.addRenderingHints(hints);
  }

  public void clearRect(int x, int y, int width, int height) {
    myDelegate.clearRect(x, y, width, height);
  }

  public void clip(Shape s) {
    myDelegate.clip(s);
  }

  public void clipRect(int x, int y, int width, int height) {
    myDelegate.clipRect(x, y, width, height);
  }

  public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    myDelegate.copyArea(x, y, width, height, dx, dy);
  }

  public Graphics create() {
    return new IdeGraphics2D((Graphics2D)myDelegate.create());
  }

  public void dispose() {
    myDelegate.dispose();
  }

  public void draw(Shape s) {
    myDelegate.draw(s);
  }

  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    myDelegate.drawArc(x, y, width, height, startAngle, arcAngle);
  }

  public void drawGlyphVector(GlyphVector g, float x, float y) {
    myDelegate.drawGlyphVector(g, x, y);
  }

  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    myDelegate.drawImage(img, op, x, y);
  }

  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
    return myDelegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
  }

  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
    return myDelegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
  }

  public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
    return myDelegate.drawImage(img, x, y, bgcolor, observer);
  }

  public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
    return myDelegate.drawImage(img, x, y, observer);
  }

  public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
    return myDelegate.drawImage(img, x, y, width, height, bgcolor, observer);
  }

  public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
    return myDelegate.drawImage(img, x, y, width, height, observer);
  }

  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
    return myDelegate.drawImage(img, xform, obs);
  }

  public void drawLine(int x1, int y1, int x2, int y2) {
    myDelegate.drawLine(x1, y1, x2, y2);
  }

  public void drawOval(int x, int y, int width, int height) {
    myDelegate.drawOval(x, y, width, height);
  }

  public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
    myDelegate.drawPolygon(xPoints, yPoints, nPoints);
  }

  public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
    myDelegate.drawPolyline(xPoints, yPoints, nPoints);
  }

  public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
    myDelegate.drawRenderableImage(img, xform);
  }

  public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
    myDelegate.drawRenderedImage(img, xform);
  }

  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    myDelegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    myDelegate.drawString(iterator, x, y);
  }

  public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    myDelegate.drawString(iterator, x, y);
  }

  public void drawString(String s, float x, float y) {
    myDelegate.drawString(s, x, y);
  }

  public void drawString(String str, int x, int y) {
    myDelegate.drawString(str, x, y);
  }

  public void fill(Shape s) {
    myDelegate.fill(s);
  }

  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    myDelegate.fillArc(x, y, width, height, startAngle, arcAngle);
  }

  public void fillOval(int x, int y, int width, int height) {
    myDelegate.fillOval(x, y, width, height);
  }

  public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
    myDelegate.fillPolygon(xPoints, yPoints, nPoints);
  }

  public void fillRect(int x, int y, int width, int height) {
    myDelegate.fillRect(x, y, width, height);
  }

  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    myDelegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  public Color getBackground() {
    return myDelegate.getBackground();
  }

  public Shape getClip() {
    return myDelegate.getClip();
  }

  public Rectangle getClipBounds() {
    return myDelegate.getClipBounds();
  }

  public Color getColor() {
    return myDelegate.getColor();
  }

  public Composite getComposite() {
    return myDelegate.getComposite();
  }

  public GraphicsConfiguration getDeviceConfiguration() {
    return myDelegate.getDeviceConfiguration();
  }

  public Font getFont() {
    return myDelegate.getFont();
  }

  public FontMetrics getFontMetrics(Font f) {
    return myDelegate.getFontMetrics(f);
  }

  public FontRenderContext getFontRenderContext() {
    return myDelegate.getFontRenderContext();
  }

  public Paint getPaint() {
    return myDelegate.getPaint();
  }

  public Object getRenderingHint(RenderingHints.Key hintKey) {
    return myDelegate.getRenderingHint(hintKey);
  }

  public RenderingHints getRenderingHints() {
    return myDelegate.getRenderingHints();
  }

  public Stroke getStroke() {
    return myDelegate.getStroke();
  }

  public AffineTransform getTransform() {
    return myDelegate.getTransform();
  }

  public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
    return myDelegate.hit(rect, s, onStroke);
  }

  public void rotate(double theta) {
    myDelegate.rotate(theta);
  }

  public void rotate(double theta, double x, double y) {
    myDelegate.rotate(theta, x, y);
  }

  public void scale(double sx, double sy) {
    myDelegate.scale(sx, sy);
  }

  public void setBackground(Color color) {
    myDelegate.setBackground(color);
  }

  public void setClip(Shape sh) {
    myDelegate.setClip(sh);
  }

  public void setClip(int x, int y, int w, int h) {
    myDelegate.setClip(x, y, w, h);
  }

  public void setColor(Color color) {
    myDelegate.setColor(color);
  }

  public void setComposite(Composite comp) {
    myDelegate.setComposite(comp);
  }

  public void setFont(Font font) {
    myDelegate.setFont(font);
  }

  public void setPaint(Paint paint) {
    myDelegate.setPaint(paint);
  }

  public void setPaintMode() {
    myDelegate.setPaintMode();
  }

  public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
    myDelegate.setRenderingHint(hintKey, hintValue);
  }

  public void setRenderingHints(Map hints) {
    myDelegate.setRenderingHints(hints);
  }

  /*
   * Sets the Stroke in the current graphics state.
   * @param s The Stroke object to be used to stroke a Path in
   * the rendering process.
   * @see BasicStroke
   */
  public void setStroke(Stroke s) {
    myDelegate.setStroke(s);
  }

  public void setTransform(AffineTransform Tx) {
    myDelegate.setTransform(Tx);
  }

  public void setXORMode(Color c) {
    myDelegate.setXORMode(c);
  }

  public void shear(double shx, double shy) {
    myDelegate.shear(shx, shy);
  }

  public void transform(AffineTransform xform) {
    myDelegate.transform(xform);
  }

  public void translate(double tx, double ty) {
    myDelegate.translate(tx, ty);
  }

  public void translate(int x, int y) {
    myDelegate.translate(x, y);
  }
}
