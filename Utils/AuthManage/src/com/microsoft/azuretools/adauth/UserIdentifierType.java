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

package com.microsoft.azuretools.adauth;

public enum UserIdentifierType
{
    /// <summary>
    /// When a <see cref=" UserIdentifier"/> of this type is passed in a token acquisition operation,
    /// the operation is guaranteed to return a token issued for the user with corresponding <see cref=" UserIdentifier.UniqueId"/> or fail.
    /// </summary>
    UniqueId,

    /// <summary>
    /// When a <see cref=" UserIdentifier"/> of this type is passed in a token acquisition operation,
    /// the operation restricts cache matches to the value provided and injects it as a hint in the authentication experience. However the end user could overwrite that value, resulting in a token issued to a different account than the one specified in the <see cref=" UserIdentifier"/> in input.
    /// </summary>
    OptionalDisplayableId,

    /// <summary>
    /// When a <see cref=" UserIdentifier"/> of this type is passed in a token acquisition operation,
    /// the operation is guaranteed to return a token issued for the user with corresponding <see cref=" UserIdentifier.DisplayableId"/> (UPN or email) or fail
    /// </summary>
    RequiredDisplayableId
}