package com.qingningshe.net;

import android.text.TextUtils;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

/**
 * @author wanglei
 * @version 1.0.0
 * @description
 * @createTime 2015/11/13
 * @editTime
 * @editor
 */
public class Get extends OkRequest {

    protected Get(String url, String tag, RequestHeaders headers, RequestParams params) {
        super(url, tag, headers, params);
    }

    @Override
    protected Request buildRequest() {
        if (TextUtils.isEmpty(url)) {
            RequestClient.L.e("url can not be empty!");
        }
        return new Request.Builder()
                .url(params.getUrl(url))
                .headers(headers.getHeaders())
                .tag(tag)
                .build();
    }

    @Override
    protected RequestBody buildRequestBody() {
        return null;
    }
}
