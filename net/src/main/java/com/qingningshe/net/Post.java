package com.qingningshe.net;

import android.text.TextUtils;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wanglei
 * @version 1.0.0
 * @description
 * @createTime 2015/11/13
 * @editTime
 * @editor
 */
public class Post extends OkRequest {
    private String content;
    private byte[] bytes;
    private File file;

    private int type = 0;
    //简单的post
    private static final int TYPE_PARAMS = 1;
    private static final int TYPE_STRING = 2;
    private static final int TYPE_BYTES = 3;
    private static final int TYPE_FILE = 4;
    //文件上传
    private static final int TYPE_UPLOAD = 5;

    private final MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream;charset=utf-8");
    private final MediaType MEDIA_TYPE_STRING = MediaType.parse("text/plain;charset=utf-8");


    protected Post(String url, String tag, RequestHeaders headers, RequestParams params, String content, File file, byte[] bytes) {
        super(url, tag, headers, params);
        this.content = content;
        this.file = file;
        this.bytes = bytes;
    }


    protected boolean validParams() {
        boolean flag = true;
        int hitCount = 0;

        if (!params.containFiles() && TextUtils.isEmpty(content) && file == null && bytes == null) {
            type = TYPE_PARAMS;
            hitCount++;
        }

        if (!TextUtils.isEmpty(content) && file == null && bytes == null && !params.containFiles()) {
            type = TYPE_STRING;
            hitCount++;
        }

        if (bytes != null && params.isEmpty() && TextUtils.isEmpty(content) && file == null) {
            type = TYPE_BYTES;
            hitCount++;
        }

        if (file != null && params.isEmpty() && TextUtils.isEmpty(content) && bytes == null) {
            type = TYPE_FILE;
            hitCount++;
        }

        if (params.containFiles() && TextUtils.isEmpty(content) && file == null && bytes == null) {
            type = TYPE_UPLOAD;
            hitCount++;
        }

        if (hitCount <= 0 || hitCount > 1) {
            RequestClient.L.e("the params , content , file , bytes must has one and only one .");
            flag = false;
        }

        return flag;

    }

    @Override
    protected Request buildRequest() {
        return new Request.Builder()
                .url(url)
                .headers(headers.getHeaders())
                .tag(tag)
                .post(requestBody)
                .build();
    }

    @Override
    protected RequestBody buildRequestBody() {
        if (!validParams()) {
            return null;
        }

        RequestBody requestBody = null;
        switch (type) {
            case TYPE_PARAMS:
                FormEncodingBuilder builder = new FormEncodingBuilder();
                addParams(builder);
                requestBody = builder.build();
                break;

            case TYPE_STRING:
                requestBody = RequestBody.create(MEDIA_TYPE_STRING, content);
                break;

            case TYPE_FILE:
                requestBody = RequestBody.create(MEDIA_TYPE_STREAM, file);
                break;

            case TYPE_BYTES:
                requestBody = RequestBody.create(MEDIA_TYPE_STREAM, bytes);
                break;

            case TYPE_UPLOAD:
                requestBody = buildMultipartFormBody(null);
                break;
        }
        return requestBody;
    }

    @Override
    protected RequestBody wrapRequestBody(RequestBody requestBody, final ResultCallBack callBack) {
        PercentRequestBody body = new PercentRequestBody(requestBody, new PercentRequestBody.RercentCallBack() {
            @Override
            public void onChange(long bytesWritten, long totalSize) {
                requestClient.sendProgressMessage(bytesWritten, totalSize, callBack);
            }
        });
        return body;
    }

    private void addParams(FormEncodingBuilder builder) {
        if (params.getMap() != null && !params.getMap().isEmpty()) {
            for (String key : params.getMap().keySet()) {
                Object value = params.getMap().get(key);
                if (value != null && !TextUtils.isEmpty(value.toString())) {
                    builder.addEncoded(key, value.toString());
                }
            }
        }
    }

    private RequestBody buildMultipartFormBody(MultipartBuilder builder) {
        if (builder == null) {
            builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        }

        if (params != null) {
            HashMap<String, Object> dataMap = params.getMap();
            ConcurrentMap<String, List<RequestParams.FileWrapper>> fileWrapperListMap = params.getFileWrapperListMap();
            ConcurrentMap<String, List<RequestParams.StreamWrapper>> streamWrapperListMap = params.getStreamWrapperListMap();
            RequestBody fileBody = null;

            if (dataMap != null && dataMap.size() > 0) {

                for (String key : dataMap.keySet()) {
                    builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                            RequestBody.create(null, dataMap.get(key).toString()));
                }
            }

            if (fileWrapperListMap != null && fileWrapperListMap.size() > 0) {
                for (String key : fileWrapperListMap.keySet()) {
                    List<RequestParams.FileWrapper> fileWrapperList = fileWrapperListMap.get(key);
                    if (fileWrapperList != null && fileWrapperList.size() > 0) {
                        for (RequestParams.FileWrapper fileWrapper : fileWrapperList) {
                            if (fileWrapper != null) {
                                fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileWrapper.getFileName())), fileWrapper.getFileBody());
                                builder.addPart(Headers.of("Content-Disposition",
                                                "form-data; name=\"" + key + "\"; filename=\"" + fileWrapper.getFileName() + "\""),
                                        fileBody);
                            }

                        }
                    }
                }
            }

            if (streamWrapperListMap != null && streamWrapperListMap.size() > 0) {
                for (String key : streamWrapperListMap.keySet()) {
                    List<RequestParams.StreamWrapper> streamWrapperList = streamWrapperListMap.get(key);
                    if (streamWrapperList != null && streamWrapperList.size() > 0) {
                        for (RequestParams.StreamWrapper streamWrapper : streamWrapperList) {
                            if (streamWrapper != null) {
                                fileBody = RequestBody.create(MediaType.parse(guessMimeType(streamWrapper.getFileName())), streamWrapper.getFileBody());
                                builder.addPart(Headers.of("Content-Disposition",
                                                "form-data; name=\"" + key + "\"; filename=\"" + streamWrapper.getFileName() + "\""),
                                        fileBody);
                            }
                        }
                    }
                }
            }
        }
        return builder.build();

    }


    private String guessMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }


}
