## QTNet
来源于hangyangAndroid封装的[okhttp-utils](https://github.com/hongyangAndroid/okhttp-utils)，使用方法类似于本人非常钟爱的[android-async-http](https://github.com/loopj/android-async-http)。
## 特性
* 链式操作
* 易于调试
* 易于设置代理
* 易于设置全局header
* 封装常见header
* 使用fastjson，只能做json解析
## 使用说明
### get请求
    RequestParams params = RequestParams.newInstance()
                .put("username", username)
                .put("password", password)
                .put("channel", Constants.API.CHANNEL_ANDROID);

     new OkRequest.Builder().headers(headers).url(url).params(params).get(callBack);

### post请求
    RequestParams params = RequestParams.newInstance()
                .put("username", username)
                .put("password", password)
                .put("channel", Constants.API.CHANNEL_ANDROID);

    new OkRequest.Builder().url(url).params(params).post(callBack);
### 上传文件(File)
    RequestParams params = RequestParams.newInstance()
                .put("type", type)
                .put("file", file, RequestParams.MEDIA.JPG);

    new OkRequest.Builder().url(url).params(params).post(callBack);
### 上传文件(InputStream)
    RequestParams params = RequestParams.newInstance()
                .put("type", type)
                .put("file", is, RequestParams.MEDIA.JPG);

    new OkRequest.Builder().url(url).params(params).post(callBack);
### 上传文件（File,InputStream混用）
     RequestParams params = RequestParams.newInstance()
                .put("type", type)
                .put("file1", file1, RequestParams.MEDIA.JPG)
				.put("file1", file2, RequestParams.MEDIA.JPG)
				.put("file2", is1, RequestParams.MEDIA.JPG)
				.put("file2", is2, RequestParams.MEDIA.JPG);

     new OkRequest.Builder().url(url).params(params).post(callBack);
### 多文件
     RequestParams params = RequestParams.newInstance()
                .put("type", type)
                .put("file1", file1, RequestParams.MEDIA.JPG)
				.put("file1", file2, RequestParams.MEDIA.JPG);

     new OkRequest.Builder().url(url).params(params).post(callBack);
### 设置代理
	RequestClient.getInstance().setProxy(server,port);
### 设置通用头信息
    RequestClient.getInstance().setCommonHeaders(headers);
### 设置调试
    RequestClient.getInstance().debug(true);
### 完整请求
     new OkRequest.Builder().headers(headers).url(url).params(params).post(new ResultCallBack<User>() {

           @Override
           public void onStart() {
               super.onStart();
           }

           @Override
           public void onSuccess(int statusCode, Headers headers, User model) {
               super.onSuccess(statusCode, headers, model);
           }

           @Override
           public void onProgress(long bytesWritten, long totalSize) {
               super.onProgress(bytesWritten, totalSize);
           }

           @Override
           public void onFailure(int statusCode, Request request, Exception e) {
               super.onFailure(statusCode, request, e);
           }

           @Override
           public void onFinish() {
               super.onFinish();
           }
       });
