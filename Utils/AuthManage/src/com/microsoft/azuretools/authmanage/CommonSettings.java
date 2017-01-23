/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.authmanage.interact.IUIFactory;

/**
 * Created by shch on 10/10/2016.
 */
public class CommonSettings {
    public static String settingsBaseDir = null;
    public static final String authMethodDetailsFileName = "AuthMethodDetails.json";
<<<<<<< HEAD
    public static IUIFactory uiFactory;
    public static String USER_AGENT = "Azure Toolkit";

    /**
     * Need this as a static method when we call this class directly from Eclipse or IntelliJ plugin to know plugin version
     */
    public static void setUserAgent(String userAgent) {
        USER_AGENT = userAgent;
    }
=======

    private static IUIFactory uiFactory;
    public static IUIFactory getUiFactory() {
        return uiFactory;
    }
    public static void setUiFactory(IUIFactory uiFactory) {
        CommonSettings.uiFactory = uiFactory;
    }

>>>>>>> origin/origin/work/v2.9.6
}
