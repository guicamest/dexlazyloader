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
package io.github.guicamest.dexlazyloader.service;

import android.app.IntentService;
import android.content.Intent;

import java.util.ArrayList;

import io.github.guicamest.dexlazyloader.LazyLoader;

public class LazyLoadService extends IntentService {

    public static final String INSTALL_MODULE_ACTION = "INSTALL_MODULE";
    public static final String MODULE_EXTRA = "MODULE_EXTRA";
    public static final String MODULES_EXTRA = "MODULES_EXTRA";
    protected final LazyLoader ll;

    public LazyLoadService(){
        super("LazyLoadService");
        ll = new LazyLoader(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if ( intent == null || !INSTALL_MODULE_ACTION.equals(intent.getAction()) ){
            return;
        }
        String[] modulesToLoad = null;
        String moduleToLoad = intent.getStringExtra(MODULE_EXTRA);
        if ( moduleToLoad != null ){
            modulesToLoad = new String[]{moduleToLoad};
        }else {
            modulesToLoad = intent.getStringArrayExtra(MODULES_EXTRA);
            if (modulesToLoad == null || modulesToLoad.length == 0) {
                ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra(MODULES_EXTRA);
                if (stringArrayListExtra != null && !stringArrayListExtra.isEmpty()) {
                    modulesToLoad = stringArrayListExtra.toArray(new String[]{});
                }
            }
        }
        if ( modulesToLoad != null && modulesToLoad.length != 0 ){
            ll.loadModules(modulesToLoad);
            return;
        }
    }
}
