/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Method interceptor for {@link ObjectDatabase#get(ObjectId, ObjectReader)} that applies caching.
 * <p>
 * <!-- increases random object lookup on revtrees by 20x, ~40K/s instad of ~2K/s as per
 * RevSHA1TreeTest.testPutGet -->
 */
public class CachingObjectDatabaseGetInterceptor implements MethodInterceptor {

    private Cache<ObjectId, RevObject> cache = CacheBuilder.newBuilder().maximumSize(50 * 1000)
            .expireAfterAccess(30, TimeUnit.SECONDS).concurrencyLevel(4).build();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final ObjectId oid = (ObjectId) invocation.getArguments()[0];

        Object object = cache.getIfPresent(oid);
        if (object == null) {
            object = invocation.proceed();
            if (object instanceof RevTree) {
                cache.put(oid, (RevObject) object);
            }
        }
        return object;
    }

}
