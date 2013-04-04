/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the Modified BSD license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import org.geogit.api.Platform;
import org.junit.Test;

/**
 *
 */
public class ResolveGeogitDirTest {

    @Test
    public void test() throws Exception {

        File workingDir = new File("target", "mockWorkingDir");
        File fakeRepo = new File(workingDir, ".geogit");
        fakeRepo.mkdirs();

        Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);

        URL resolvedRepoDir = new ResolveGeogitDir(platform).call();
        assertEquals(fakeRepo.toURI().toURL(), resolvedRepoDir);

        workingDir = new File(new File(workingDir, "subdir1"), "subdir2");
        workingDir.mkdirs();
        when(platform.pwd()).thenReturn(workingDir);

        resolvedRepoDir = new ResolveGeogitDir(platform).call();
        assertEquals(fakeRepo.toURI().toURL(), resolvedRepoDir);

        when(platform.pwd()).thenReturn(new File("target"));
        assertNull(new ResolveGeogitDir(platform).call());

    }

}
