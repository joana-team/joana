/*
 * Copyright (C) 2011 The Android Open Source Project
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
//com/android/ide/common/rendering/api/RenderResources
//import com.android.resources.ResourceType;

/**
 * A class containing all the resources needed to do a rendering.
 * <p/>
 * This contains both the project specific resources and the framework resources, and provide
 * convenience methods to resolve resource and theme references.
 */
public class RenderResources {

    public static final String REFERENCE_NULL = "@null";

    public RenderResources() {

    }

    /*
    public static class FrameworkResourceIdProvider {
        public Integer getId(ResourceType resType, String resName) {
            return null;
        }
    }

    public void setFrameworkResourceIdProvider(FrameworkResourceIdProvider provider) {
    }

    public void setLogger(LayoutLog logger) {
    }

    public StyleResourceValue getCurrentTheme() {
        return null;
    }
    public StyleResourceValue getTheme(String name, boolean frameworkTheme) {
        return null;
    }

    public boolean themeIsParentOf(StyleResourceValue parentTheme, StyleResourceValue childTheme) {
        return false;
    }

    public ResourceValue getFrameworkResource(ResourceType resourceType, String resourceName) {
        return null;
    }

    public ResourceValue getProjectResource(ResourceType resourceType, String resourceName) {
        return null;
    }

    @Deprecated
    public ResourceValue findItemInTheme(String itemName) {
        return null;
    }

    public ResourceValue findItemInTheme(String attrName, boolean isFrameworkAttr) {
        return null;
    }

    @Deprecated
    public ResourceValue findItemInStyle(StyleResourceValue style, String attrName) {
        return null;
    }

    public ResourceValue findItemInStyle(StyleResourceValue style, String attrName,
            boolean isFrameworkAttr) {
        return null;
    }

    public ResourceValue findResValue(String reference, boolean forceFrameworkOnly) {
        return null;
    }

    public ResourceValue resolveValue(ResourceType type, String name, String value,
            boolean isFrameworkValue) {
        return null;
    }

    public ResourceValue resolveResValue(ResourceValue value) {
        return null;
    } */
}
