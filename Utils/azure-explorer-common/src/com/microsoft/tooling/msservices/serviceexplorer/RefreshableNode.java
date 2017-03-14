/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import java.util.List;

public abstract class RefreshableNode extends Node {
    protected boolean initialized;
    public static String REFRESH_ICON_LIGHT = "RefreshLight_16.png";
    public static String REFRESH_ICON_DARK = "RefreshDark_16.png";

    public RefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public RefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void loadActions() {
        addAction("Refresh", DefaultLoader.getUIHelper().isDarkTheme() ? REFRESH_ICON_DARK : REFRESH_ICON_LIGHT, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                load(true);
            }
        });

        super.loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        if (!initialized) {
            this.load(false);
            initialized = true;
        }
    }

    // Sub-classes are expected to override this method if they wish to
    // refresh items synchronously. The default implementation does nothing.
    protected abstract void refreshItems() throws AzureCmdException;

    // Sub-classes are expected to override this method if they wish
    // to refresh items asynchronously. The default implementation simply
    // delegates to "refreshItems" *synchronously* and completes the Future
    // with the result of calling getChildNodes.
    protected void refreshItems(SettableFuture<List<Node>> future, boolean forceRefresh) {
        if (!loading) {
            setLoading(true);
            try {
                removeAllChildNodes();
                if (forceRefresh) {
                    refreshFromAzure();
                }
                refreshItems();
                future.set(getChildNodes());
            } catch (Exception e) {
                future.setException(e);
            } finally {
                setLoading(false);
            }
        }
    }

    protected void refreshFromAzure() throws Exception {
    }

    public ListenableFuture<List<Node>> load(boolean forceRefresh) {
        final RefreshableNode node = this;
        final SettableFuture<List<Node>> future = SettableFuture.create();

        DefaultLoader.getIdeHelper().runInBackground(getProject(), "Loading " + getName() + "...", true, true, null,
                new Runnable() {
                    @Override
                    public void run() {
                        final String nodeName = node.getName();
                        updateName(nodeName + " (Refreshing...)", null);
//                        node.setName(nodeName + " (Refreshing...)");

                        Futures.addCallback(future, new FutureCallback<List<Node>>() {
                            @Override
                            public void onSuccess(List<Node> nodes) {
                                updateName(nodeName, null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                updateName(nodeName, throwable);
                            }
                        });
                        node.refreshItems(future, forceRefresh);
                    }

                    private void updateName(String name, final Throwable throwable) {
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                node.setName(name);

                                if (throwable != null) {
                                    DefaultLoader.getUIHelper().showException("An error occurred while attempting " +
                                                    "to load " + node.getName() + ".",
                                            throwable,
                                            "MS Azure Explorer - Error Loading " + node.getName(),
                                            false,
                                            true);
                                }
                            }
                        });
                    }
                }
        );

        return future;
    }

    public Runnable getRefreshHandler() {
        return new Runnable() {
            @Override
            public void run() {
                load(false);
            }
        };
    }
}