/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.rendering.api;

import com.android.ide.common.rendering.api.IProjectCallback;
//import com.android.util.Pair;

import java.net.URL;

public class ProjectCallbackDummy implements IProjectCallback {
    public Object loadView(String name, Class[] constructorSignature, Object[] constructorArgs)
        throws ClassNotFoundException, Exception {
        return null;
    }

    public String getNamespace() {
        return null;
    }

    //    Pair<ResourceType, String> resolveResourceId(int id);
    //
    public String resolveResourceId(int[] id) {
        return null;
    }

    // Integer getResourceId(ResourceType type, String name);

    //ILayoutPullParser getParser(String layoutName);

    //ILayoutPullParser getParser(ResourceValue layoutResource);

    /*Object getAdapterItemValue(ResourceReference adapterView, Object adapterCookie,
            ResourceReference itemRef,
            int fullPosition, int positionPerType,
            int fullParentPosition, int parentPositionPerType,
            ResourceReference viewRef, ViewAttribute viewAttribute, Object defaultValue);
    */

    /*
    AdapterBinding getAdapterBinding(ResourceReference adapterViewRef, Object adapterCookie,
            Object viewObject);*/
}
