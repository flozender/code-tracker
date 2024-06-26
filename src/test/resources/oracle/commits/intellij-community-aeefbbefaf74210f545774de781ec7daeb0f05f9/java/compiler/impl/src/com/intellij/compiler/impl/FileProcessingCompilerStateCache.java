/*
 * @author: Eugene Zhuravlev
 * Date: Apr 1, 2003
 * Time: 1:17:50 PM
 */
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.compiler.ValidityStateFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Collection;

public class FileProcessingCompilerStateCache {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.FileProcessingCompilerStateCache");
  private final StateCache<MyState> myCache;

  public FileProcessingCompilerStateCache(File storeDirectory, final ValidityStateFactory stateFactory) throws IOException {
    myCache = new StateCache<MyState>(new File(storeDirectory, "timestamps")) {
      public MyState read(DataInput stream) throws IOException {
        return new MyState(stream.readLong(), stateFactory.createValidityState(stream));
      }

      public void write(MyState state, DataOutput out) throws IOException {
        out.writeLong(state.getTimestamp());
        final ValidityState extState = state.getExtState();
        if (extState != null) {
          extState.save(out);
        }
      }
    };
  }

  public void update(VirtualFile sourceFile, ValidityState extState) throws IOException {
    if (!sourceFile.isValid()) {
      LOG.error("Source file must be valid " + sourceFile.getPresentableUrl() +
                ", state.getClass() = " + (extState != null ? extState.getClass().getName() : "null"));
    }
    myCache.update(sourceFile.getUrl(), new MyState(sourceFile.getTimeStamp(), extState));
  }

  public void remove(String url) throws IOException {
    myCache.remove(url);
  }

  public long getTimestamp(String url) throws IOException {
    final Serializable savedState = myCache.getState(url);
    if (savedState != null) {
      LOG.assertTrue(savedState instanceof MyState);
    }
    MyState state = (MyState)savedState;
    return (state != null)? state.getTimestamp() : -1L;
  }

  public ValidityState getExtState(String url) throws IOException {
    MyState state = myCache.getState(url);
    return (state != null)? state.getExtState() : null;
  }

  public void force() {
    myCache.force();
  }

  public Collection<String> getUrls() throws IOException {
    return myCache.getUrls();
  }

  public boolean wipe() {
    return myCache.wipe();
  }

  public void close() {
    try {
      myCache.close();
    }
    catch (IOException ignored) {
      LOG.info(ignored);
    }
  }

  private static class MyState implements Serializable {
    private final long myTimestamp;
    private final ValidityState myExtState;

    public MyState(long timestamp, @Nullable ValidityState extState) {
      myTimestamp = timestamp;
      myExtState = extState;
    }

    public long getTimestamp() {
      return myTimestamp;
    }

    public @Nullable ValidityState getExtState() {
      return myExtState;
    }
  }

}
