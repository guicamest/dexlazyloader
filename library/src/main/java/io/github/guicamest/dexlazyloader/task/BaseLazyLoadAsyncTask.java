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
package io.github.guicamest.dexlazyloader.task;

import android.content.Context;

import io.github.guicamest.dexlazyloader.LazyLoader;

public abstract class BaseLazyLoadAsyncTask<Params, Progress, Result> extends android.os.AsyncTask<Params, Progress, Result> {

    final protected Context mContext;

    public BaseLazyLoadAsyncTask(Context mContext) {
        this.mContext = mContext;
    }

    public boolean loadModules(String... dexAssetPaths) {
        return LazyLoader.loadModulesWithContext(mContext, dexAssetPaths);
    }

}
