package com.qingningshe.net;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;

/**
 * @author wanglei
 * @version 1.0.0
 * @description
 * @createTime 2015/11/13
 * @editTime
 * @editor
 */
public class ResultCallBack<T> {
    public void onStart() {
    }

    public void onFinish() {
    }

    public void onSuccess(int statusCode, Headers headers, T model) {

    }

    public void onFailure(int statusCode, Request request, Exception e) {

    }

    public void onProgress(long bytesWritten, long totalSize) {

    }
}
