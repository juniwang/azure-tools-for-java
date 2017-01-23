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

package com.microsoft.azuretools.authmanage.srvpri.rest;

/**
 * Created by vlashch on 8/29/16.
 */


import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureGraphException;

public class GraphRequestFactory extends RequestFactoryBase {

    public GraphRequestFactory(String tenantId) {
        this.tenantId = tenantId;
        this.urlPrefix = Constants.resourceGraph + this.tenantId + "/";
        this.resource =  Constants.resourceGraph;;
        apiVersion = "api-version=1.6";
    }

    @Override
    public AzureException newAzureException(String message) {
        return new AzureGraphException(message);
    }
}
