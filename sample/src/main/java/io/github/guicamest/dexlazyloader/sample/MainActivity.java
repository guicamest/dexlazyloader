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
package io.github.guicamest.dexlazyloader.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import io.github.guicamest.dexlazyloader.LazyLoader;
import io.github.guicamest.dexlazyloader.task.SimpleLazyLoadAsyncTask;

public class MainActivity extends Activity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.image_view);

        new SimpleLazyLoadAsyncTask<Void>(this){
            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                if ( success ){
                    final PicassoWrapper picassoWrapper = PicassoWrapper.get(mContext);
                    Log.d("TAG", "loading image");
                    picassoWrapper.load("http://i.imgur.com/CqmBjo5.jpg", imageView);
                }
            }
        }.execute("picasso-2.5.2.dex.jar");
    }

    public void onLoadImageClick(View v) {
        Log.d("TAG", "onLoadImageClick");

        // TODO: Yes ,this should not be done in the main thread. Sorry Kittens.
        LazyLoader.loadModulesWithContext(this, "picasso-2.5.2.dex.jar");
        Log.d("TAG", "module loaded");

        final PicassoWrapper picassoWrapper = PicassoWrapper.get(this);
        Log.d("TAG", "loading image");
        picassoWrapper.load("http://www.memecreator.org/static/images/memes/3767015.jpg", imageView);
    }
}
