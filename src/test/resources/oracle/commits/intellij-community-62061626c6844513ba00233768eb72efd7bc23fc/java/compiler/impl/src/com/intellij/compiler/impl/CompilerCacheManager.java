package com.intellij.compiler.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 *         Date: May 4, 2008
 */
public class CompilerCacheManager implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.CompilerCacheManager");
  private final Map<Compiler, Object> myCompilerToCacheMap = new HashMap<Compiler, Object>();
  private final List<Disposable> myCacheDisposables = new ArrayList<Disposable>();
  private final File myCachesRoot;
  private final Runnable myShutdownTask = new Runnable() {
    public void run() {
      flushCaches();
    }
  };

  public CompilerCacheManager(Project project) {
    myCachesRoot = CompilerPaths.getCacheStoreDirectory(project);
  }

  public static CompilerCacheManager getInstance(Project project) {
    return project.getComponent(CompilerCacheManager.class);
  }
  
  public void projectOpened() {
    ShutDownTracker.getInstance().registerShutdownTask(myShutdownTask);
  }

  public void projectClosed() {
    ShutDownTracker.getInstance().unregisterShutdownTask(myShutdownTask);
    flushCaches();
  }

  @NotNull
  public String getComponentName() {
    return "CompilerCacheManager";
  }

  public void initComponent() {
  }

  public void disposeComponent() {
    flushCaches();
  }
  
  private File getCompilerRootDir(final Compiler compiler) {
    final File dir = new File(myCachesRoot, getCompilerIdString(compiler));
    dir.mkdirs();
    return dir;
  }

  public synchronized FileProcessingCompilerStateCache getFileProcessingCompilerCache(FileProcessingCompiler compiler) throws IOException {
    Object cache = myCompilerToCacheMap.get(compiler);
    if (cache == null) {
      final FileProcessingCompilerStateCache stateCache = new FileProcessingCompilerStateCache(getCompilerRootDir(compiler),
          compiler
      );
      myCompilerToCacheMap.put(compiler, stateCache);
      myCacheDisposables.add(new Disposable() {
        public void dispose() {
          stateCache.close();
        }
      });
      cache = stateCache;
    }
    else {
      LOG.assertTrue(cache instanceof FileProcessingCompilerStateCache);
    }
    return (FileProcessingCompilerStateCache)cache;
  }

  public synchronized StateCache<ValidityState> getGeneratingCompilerCache(final GeneratingCompiler compiler) throws IOException {
    Object cache = myCompilerToCacheMap.get(compiler);
    if (cache == null) {
      final File cacheDir = getCompilerRootDir(compiler);
      final StateCache<ValidityState> stateCache = new StateCache<ValidityState>(new File(cacheDir, "timestamps")) {
        public ValidityState read(DataInput stream) throws IOException {
          return compiler.createValidityState(stream);
        }
  
        public void write(ValidityState validityState, DataOutput out) throws IOException {
          validityState.save(out);
        }
      };
      myCompilerToCacheMap.put(compiler, stateCache);
      myCacheDisposables.add(new Disposable() {
        public void dispose() {
          try {
            stateCache.close();
          }
          catch (IOException e) {
            LOG.info(e);
          }
        }
      });
      cache = stateCache;
    }
    return (StateCache<ValidityState>)cache;
  }

  public static String getCompilerIdString(Compiler compiler) {
    @NonNls String description = compiler.getDescription();
    return description.replaceAll("\\s+", "_").replaceAll("[\\.\\?]", "_").toLowerCase();
  }
  
  private synchronized void flushCaches() {
    for (Disposable disposable : myCacheDisposables) {
      try {
        disposable.dispose();
      }
      catch (Throwable e) {
        LOG.info(e);
      }
    }
    myCacheDisposables.clear();
    myCompilerToCacheMap.clear();
  }

  public void clearCaches(final CompileContext context) {
    flushCaches();
    final File[] children = myCachesRoot.listFiles();
    if (children != null) {
      for (final File child : children) {
        final boolean deleteOk = FileUtil.delete(child);
        if (!deleteOk) {
          context.addMessage(CompilerMessageCategory.ERROR, CompilerBundle.message("compiler.error.failed.to.delete", child.getPath()), null, -1, -1);
        }
      }
    }
  }
}
