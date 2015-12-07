/*
 * Copyright 2015 guicamest
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
package io.github.guicamest.dexlazyloader;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.dyl.multidex.MultiDex;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class to dynamically load dexed jars in assets folder
 */
public class LazyLoader {

    private static final int BUF_SIZE = 8 * 1024;
    private static final String TAG = "LazyLoader";

    private final ClassLoader cl;
    private final File optimizedDexOutputPath;
    private final AssetManager am;

    public LazyLoader(Context ctx){
        optimizedDexOutputPath = getOutDexDir(ctx);
        am = ctx.getAssets();
        cl = ctx.getClassLoader();
    }

    private static File getOutDexDir(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            return ctx.getCodeCacheDir();
        }
        File dir = new File(ctx.getCacheDir(),"code_cache");
        if ( !dir.getParentFile().exists() ){
            dir.getParentFile().mkdir();
        }
        if ( !dir.exists() ){
            dir.mkdir();
        }
        return dir;
    }

    /**
     * Loads dexed jars in assets folders
     * @param dexAssetPaths Path to dexed jars in assets folders to load
     * @return true if all jars loaded correctly
     */
    public static boolean loadModulesWithContext(Context ctx, String... dexAssetPaths) {
        return loadModulesWithContext(ctx, false, dexAssetPaths);
    }

    /**
     * Loads dexed jars in assets folders
     * @param dexAssetPaths Path to dexed jars in assets folders to load
     * @param forceReload If true, behaviour is as the assets hadn't been loaded
     * @return true if all jars loaded correctly
     */
    public static boolean loadModulesWithContext(Context ctx, boolean forceReload, String... dexAssetPaths) {
        return new LazyLoader(ctx).loadModules(forceReload, dexAssetPaths);
    }

    /**
     * Loads dexed jars in assets folders
     * @param dexAssetPaths Path to dexed jars in assets folders to load
     * @return true if all jars loaded correctly
     */
    public boolean loadModules(String... dexAssetPaths) {
        return loadModules(false, dexAssetPaths);
    }

    /**
     * Loads dexed jars in assets folders
     * @param dexAssetPaths Path to dexed jars in assets folders to load
     * @param forceReload If true, behaviour is as the assets hadn't been loaded
     * @return true if all jars loaded correctly
     */
    public boolean loadModules(boolean forceReload, String... dexAssetPaths) {
        if ( dexAssetPaths == null || dexAssetPaths.length == 0 ){
            return false;
        }
        Log.d(TAG, "Moving dexed jars to code cache dir");
        final long startTime = System.currentTimeMillis();

        final int length = dexAssetPaths.length;
        List<File> movedJars = new ArrayList<File>();
        for(int i=0; i < length; i++){
            String dexAssetPath = dexAssetPaths[i];
            if ( dexAssetPath == null || dexAssetPath.length() == 0 ){
                continue;
            }
            Log.d(TAG, "Start loading " + dexAssetPath);

            final File dexInternalStoragePath = new File(optimizedDexOutputPath, dexAssetPath);
            if (!dexInternalStoragePath.exists() || forceReload) {
                final long startPrepareDex = System.currentTimeMillis();
                if (prepareDex(dexInternalStoragePath, dexAssetPath)) {
                    Log.d(TAG, dexAssetPath+" moved in " + (System.currentTimeMillis() - startPrepareDex)+"ms");
                    movedJars.add(dexInternalStoragePath);
                } else {
                    Log.d(TAG, "Failed to move "+dexAssetPath+" dexed jar");
                    return false;
                }
            }
            
            if (!movedJars.contains(dexInternalStoragePath) && dexInternalStoragePath.exists()){
                movedJars.add(dexInternalStoragePath);
            }
        }
            
        Log.d(TAG, "Done moving dexed jars to code cache dir. Took "+(System.currentTimeMillis() - startTime)+"ms");
        Log.d(TAG, "Installing dexed jars using cl with class "+cl.getClass());

        try {
            MultiDex.installSecondaryDexes(cl, optimizedDexOutputPath, movedJars);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "Success! Whole process took: " + (System.currentTimeMillis() - startTime)+"ms");
        return true;
    }

    private boolean prepareDex(File dexInternalStoragePath, String dexAssetPath) {
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;

        try {
            final InputStream is = am.open(dexAssetPath);
            bis = new BufferedInputStream(is);
            dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            final byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            Log.d("General I/O exception: ", e.getMessage());
            return false;
        }finally {
            safeClose(dexWriter);
            safeClose(bis);
        }
    }

    private static void safeClose(Closeable c){
        if (c != null) {
            try {
                c.close();
            } catch (IOException ioe) {
                Log.w(TAG, "Failed to close stream", ioe);
            }
        }
    }
}
