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

package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.tooling.msservices.helpers.NotNull;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class JobViewPanel extends JFXPanel {

    private final Object jobUtil;
    private final String rootPath;
    private final String id;
    private WebView webView;
    private WebEngine webEngine;
    private boolean alreadyLoad = false;

    public JobViewPanel(@NotNull String rootPath, @NotNull String uuid) {
        this.rootPath = rootPath;
        this.id = uuid;
        this.jobUtil = new JobUtils();
        init(this);
    }

    private void init(final JFXPanel panel) {
        String url = rootPath + "/hdinsight/job/html/index.html";
        url = url.replace("\\", "/");
        final String queryString = "?projectid=" + id + "&engintype=javafx";
        final String weburl = "file:///" + url + queryString;

        Platform.setImplicitExit(false);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webView = new WebView();
                panel.setScene(new Scene(webView));
                webEngine = webView.getEngine();
                webEngine.setJavaScriptEnabled(true);

                if (!alreadyLoad) {
                    webEngine.load(weburl);
                    alreadyLoad = true;
                    webEngine.getLoadWorker().stateProperty().addListener(
                            new ChangeListener<Worker.State>() {
                                @Override
                                public void changed(ObservableValue<? extends Worker.State> ov,
                                                    Worker.State oldState, Worker.State newState) {
                                    if (newState == Worker.State.SUCCEEDED) {
                                        JSObject win = (JSObject) webEngine.executeScript("window");
                                        win.setMember("JobUtils", jobUtil);
                                    }
                                }
                            }
                    );
                }
            }
        });
    }

}