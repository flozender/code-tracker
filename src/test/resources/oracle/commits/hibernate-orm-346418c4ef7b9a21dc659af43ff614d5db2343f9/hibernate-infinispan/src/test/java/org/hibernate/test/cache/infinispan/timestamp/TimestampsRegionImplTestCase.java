/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.timestamp;
import java.util.Properties;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.Region;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.impl.ClassLoaderAwareCache;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheAdapterImpl;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.infinispan.AbstractGeneralDataRegionTestCase;
import org.hibernate.test.cache.infinispan.functional.classloader.Account;
import org.hibernate.test.cache.infinispan.functional.classloader.AccountHolder;
import org.hibernate.test.cache.infinispan.functional.classloader.SelectedClassnameClassLoader;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.infinispan.AdvancedCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.Event;

/**
 * Tests of TimestampsRegionImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TimestampsRegionImplTestCase extends AbstractGeneralDataRegionTestCase {

   public TimestampsRegionImplTestCase(String name) {
      super(name);
   }

    @Override
   protected String getStandardRegionName(String regionPrefix) {
      return regionPrefix + "/" + UpdateTimestampsCache.class.getName();
   }

   @Override
   protected Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
      return regionFactory.buildTimestampsRegion(regionName, properties);
   }

   @Override
   protected CacheAdapter getInfinispanCache(InfinispanRegionFactory regionFactory) {
      return CacheAdapterImpl.newInstance(regionFactory.getCacheManager().getCache("timestamps"));
   }

   public void testClearTimestampsRegionInIsolated() throws Exception {
      Configuration cfg = createConfiguration();
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
			  getServiceRegistry(), cfg, getCacheTestSupport()
	  );
      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      Configuration cfg2 = createConfiguration();
      InfinispanRegionFactory regionFactory2 = CacheTestUtil.startRegionFactory(
			  getServiceRegistry(), cfg2, getCacheTestSupport()
	  );
      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      TimestampsRegionImpl region = (TimestampsRegionImpl) regionFactory.buildTimestampsRegion(getStandardRegionName(REGION_PREFIX), cfg.getProperties());
      TimestampsRegionImpl region2 = (TimestampsRegionImpl) regionFactory2.buildTimestampsRegion(getStandardRegionName(REGION_PREFIX), cfg2.getProperties());
//      QueryResultsRegion region2 = regionFactory2.buildQueryResultsRegion(getStandardRegionName(REGION_PREFIX), cfg2.getProperties());

//      ClassLoader cl = Thread.currentThread().getContextClassLoader();
//      Thread.currentThread().setContextClassLoader(cl.getParent());
//      log.info("TCCL is " + cl.getParent());

      Account acct = new Account();
      acct.setAccountHolder(new AccountHolder());
      region.getCacheAdapter().withFlags(FlagAdapter.FORCE_SYNCHRONOUS).put(acct, "boo");

//      region.put(acct, "boo");
//
//      region.evictAll();

//      Account acct = new Account();
//      acct.setAccountHolder(new AccountHolder());



   }

   @Override
   protected Configuration createConfiguration() {
      Configuration cfg = CacheTestUtil.buildConfiguration("test", MockInfinispanRegionFactory.class, false, true);
      return cfg;
   }

   public static class MockInfinispanRegionFactory extends InfinispanRegionFactory {

      public MockInfinispanRegionFactory() {
      }

      public MockInfinispanRegionFactory(Properties props) {
         super(props);
      }

//      @Override
//      protected TimestampsRegionImpl createTimestampsRegion(CacheAdapter cacheAdapter, String regionName) {
//         return new MockTimestampsRegionImpl(cacheAdapter, regionName, getTransactionManager(), this);
//      }

      @Override
      protected ClassLoaderAwareCache createCacheWrapper(AdvancedCache cache) {
         return new ClassLoaderAwareCache(cache, Thread.currentThread().getContextClassLoader()) {
            @Override
            public void addListener(Object listener) {
               super.addListener(new MockClassLoaderAwareListener(listener, this));
            }
         };
      }

      //      @Override
//      protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
//         try {
//            EmbeddedCacheManager manager = new DefaultCacheManager(InfinispanRegionFactory.DEF_INFINISPAN_CONFIG_RESOURCE);
//            org.infinispan.config.Configuration ispnCfg = new org.infinispan.config.Configuration();
//            ispnCfg.setCacheMode(org.infinispan.config.Configuration.CacheMode.REPL_SYNC);
//            manager.defineConfiguration("timestamps", ispnCfg);
//            return manager;
//         } catch (IOException e) {
//            throw new CacheException("Unable to create default cache manager", e);
//         }
//      }

      @Listener      
      public static class MockClassLoaderAwareListener extends ClassLoaderAwareCache.ClassLoaderAwareListener {
         MockClassLoaderAwareListener(Object listener, ClassLoaderAwareCache cache) {
            super(listener, cache);
         }

         @CacheEntryActivated
         @CacheEntryCreated
         @CacheEntryEvicted
         @CacheEntryInvalidated
         @CacheEntryLoaded
         @CacheEntryModified
         @CacheEntryPassivated
         @CacheEntryRemoved
         @CacheEntryVisited
         public void event(Event event) throws Throwable {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String notFoundPackage = "org.hibernate.test.cache.infinispan.functional.classloader";
            String[] notFoundClasses = { notFoundPackage + ".Account", notFoundPackage + ".AccountHolder" };
            SelectedClassnameClassLoader visible = new SelectedClassnameClassLoader(null, null, notFoundClasses, cl);
            Thread.currentThread().setContextClassLoader(visible);
            super.event(event);
            Thread.currentThread().setContextClassLoader(cl);            
         }
      }
   }

//   @Listener
//   public static class MockTimestampsRegionImpl extends TimestampsRegionImpl {
//
//      public MockTimestampsRegionImpl(CacheAdapter cacheAdapter, String name, TransactionManager transactionManager, RegionFactory factory) {
//         super(cacheAdapter, name, transactionManager, factory);
//      }
//
//      @CacheEntryModified
//      public void nodeModified(CacheEntryModifiedEvent event) {
////         ClassLoader cl = Thread.currentThread().getContextClassLoader();
////         String notFoundPackage = "org.hibernate.test.cache.infinispan.functional.classloader";
////         String[] notFoundClasses = { notFoundPackage + ".Account", notFoundPackage + ".AccountHolder" };
////         SelectedClassnameClassLoader visible = new SelectedClassnameClassLoader(null, null, notFoundClasses, cl);
////         Thread.currentThread().setContextClassLoader(visible);
//         super.nodeModified(event);
////         Thread.currentThread().setContextClassLoader(cl);
//      }
//   }

}
