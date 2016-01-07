package com.qingningshe.net;

import android.text.TextUtils;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.File;

/**
 * @author wanglei
 * @version 1.0.0
 * @description
 * @createTime 2015/11/13
 * @editTime
 * @editor
 */
public abstract class OkRequest {
    protected RequestClient requestClient;
    protected OkHttpClient okHttpClient;

    protected String url;
    protected String tag;
    protected RequestHeaders headers;
    protected RequestParams params;

    protected Request request;
    protected RequestBody requestBody;

    protected OkRequest(String url, String tag, RequestHeaders headers, RequestParams params) {
        requestClient = RequestClient.getInstance();
        okHttpClient = requestClient.getOkHttpClient();

        this.url = url;
        this.tag = tag;
        this.headers = headers == null ? new RequestHeaders() : headers;
        this.params = params == null ? RequestParams.newInstance() : params;
    }

    protected abstract Request buildRequest();

    protected abstract RequestBody buildRequestBody();

    protected RequestBody wrapRequestBody(RequestBody requestBody, ResultCallBack callBack) {
        return requestBody;
    }


    protected OkRequest invoke(ResultCallBack callBack) {
        prepareInvoke(callBack);

        logInfo();

        requestClient.invoke(request, callBack);

        return this;
    }


    protected void prepareInvoke(ResultCallBack callBack) {
        requestBody = buildRequestBody();
        requestBody = wrapRequestBody(requestBody, callBack);
        request = buildRequest();
    }

    /**
     * 取消请求
     */
    public void cancel() {
        if (!TextUtils.isEmpty(tag)) {
            okHttpClient.cancel(tag);
        }
    }

    protected void logInfo() {
        RequestClient.L.i("*************************************");
        RequestClient.L.i("url:" + url);
        RequestClient.L.i("method:" + request.method());
        RequestClient.L.i("data:" + params.toString());
    }


    public static class Builder {
        private String url;
        private String tag;
        private RequestHeaders headers;
        private RequestParams params;

        //download
        private String destFileDir;
        private String destFileName;

        //post
        private String content;
        private File file;
        private byte[] bytes;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder headers(RequestHeaders headers) {
            this.headers = headers;
            return this;
        }

        public Builder params(RequestParams params) {
            this.params = params;
            return this;
        }

        public Builder destFileDir(String destFileDir) {
            this.destFileDir = destFileDir;
            return this;
        }

        public Builder destFileName(String destFileName) {
            this.destFileName = destFileName;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder file(File file) {
            this.file = file;
            return this;
        }

        public Builder bytes(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        public OkRequest get(ResultCallBack callBack) {
            return new Get(url, tag, headers, params).invoke(callBack);
        }

        public OkRequest post(ResultCallBack callBack) {
            return new Post(url, tag, headers, params, content, file, bytes).invoke(callBack);
        }

        public OkRequest download(ResultCallBack callBack) {
            if (TextUtils.isEmpty(destFileDir) || TextUtils.isEmpty(destFileName)) {
                RequestClient.L.e("destFileDir and destFileName can not be null.");
            }
            return new Download(url, tag, headers, params, destFileDir, destFileName).invoke(callBack);
        }


    }
}
