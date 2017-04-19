/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.microsoft.azuretools.utils;

import rx.Subscriber;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.util.HashMap;
import java.util.Map;

public class AzureUIRefreshCore {
  public static PublishSubject<AzureUIRefreshEvent> publisher;
  public static Map<String, Subscription> listenersRx;

  public static synchronized void removeListener(String id) {
    if (listenersRx != null && publisher != null) {
      try {
        Subscription listener = listenersRx.remove(id);
        listener.unsubscribe();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public static synchronized void removeAll() {
    if (listenersRx != null && publisher != null) {
      // signal to all the subscribers/listeners that they need to reset
      publisher.onNext(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REMOVE, null));
      publisher.onCompleted();
      try {
        for (String id : listenersRx.keySet()) {
          Subscription listener = listenersRx.remove(id);
          listener.unsubscribe();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      listenersRx = null;
      publisher = null;
    }
  }

  public static synchronized Subscription addOnNextListener(String id, AzureUIRefreshListener onNext) {
    if (publisher == null) {
      publisher = PublishSubject.create();
      listenersRx = new HashMap<>();
    }

    Subscription currentListener = listenersRx.get(id);
    if (currentListener != null) {
      try {
        currentListener.unsubscribe();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    Subscription newSubscriber = publisher.subscribe(new Subscriber<AzureUIRefreshEvent>() {
      @Override
      public void onCompleted() {}

      @Override
      public void onError(Throwable throwable) {}

      @Override
      public void onNext(AzureUIRefreshEvent event) {
        onNext.setEvent(event);
        onNext.run();
      }
    });

    listenersRx.put(id, newSubscriber);

    return newSubscriber;
  }
}
