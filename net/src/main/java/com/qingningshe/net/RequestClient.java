package com.qingningshe.net;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


/**
 * @author wanglei
 * @version 1.0.0
 * @description 请求客户端
 * @createTime 2015/11/13
 * @editTime
 * @editor
 */
public class RequestClient {
    private static RequestClient instance;
    private OkHttpClient okHttpClient;
    private Handler delivery;

    public static final int STATUS_CODE_NONE = -1;        //无返回状态码

    private RequestClient() {
        okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(5000, TimeUnit.MILLISECONDS);
        okHttpClient.setRetryOnConnectionFailure(true);
        okHttpClient.setReadTimeout(50000, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(50000, TimeUnit.MILLISECONDS);
        okHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));

        delivery = new Handler(Looper.getMainLooper());
    }

    public static RequestClient getInstance() {
        if (instance == null) {
            synchronized (RequestClient.class) {
                if (instance == null) {
                    instance = new RequestClient();
                }
            }
        }
        return instance;
    }


    public <T> void invoke(final Request request, ResultCallBack<T> callBack) {
        if (callBack == null) {
            callBack = new ResultCallBack<T>() {
            };
        }
        sendStartMessage(callBack);

        final ResultCallBack<T> resultCallBack = callBack;

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                sendFailMessage(STATUS_CODE_NONE, request, e, resultCallBack);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                int statusCode = response.code();

                if (statusCode >= 400 && statusCode <= 599) {
                    try {
                        sendFailMessage(statusCode, request, new RuntimeException(response.body().string()), resultCallBack);
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendFailMessage(statusCode, request, new RuntimeException(""), resultCallBack);
                    } finally {
                        return;
                    }
                }

                String result = response.body().string();

                if (response != null && !TextUtils.isEmpty(result)) {

                    try {
                        ParameterizedType pt = (ParameterizedType) resultCallBack
                                .getClass().getGenericSuperclass();
                        if (pt.getActualTypeArguments()[0] instanceof Class) {

                            Class<?> clazz = (Class<?>) pt.getActualTypeArguments()[0];
                            if ("java.lang.String".equals(clazz.getName())) {
                                sendSuccessMessage(statusCode, response.headers(), (T) result, resultCallBack);

                            } else {
                                T model = (T) JSON.parseObject(result, clazz);
                                sendSuccessMessage(statusCode, response.headers(), model, resultCallBack);
                            }
                        } else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType) {
                            ParameterizedType type = (ParameterizedType) pt.getActualTypeArguments()[0];
                            Class<?> clazz = (Class<?>) type.getActualTypeArguments()[0];

                            T model = (T) JSON.parseArray(result, clazz);
                            sendSuccessMessage(statusCode, response.headers(), model, resultCallBack);
                        }

                    } catch (Exception ex) {
                        sendFailMessage(statusCode, request, ex, resultCallBack);
                    } finally {
                        L.i("result:" + result);
                    }

                }
            }
        });

    }

    /**
     * 取消请求
     *
     * @param tag
     */
    public void cancel(Object tag) {
        okHttpClient.cancel(tag);
    }


    public <T> void sendFailMessage(final int statusCode, final Request request, final Exception e, final ResultCallBack<T> callBack) {
        delivery.post(new Runnable() {
            @Override
            public void run() {
                L.e(e);
                callBack.onFinish();
            }
        });
    }

    public <T> void sendSuccessMessage(final int statusCode, final Headers headers, final T model, final ResultCallBack<T> callBack) {
        delivery.post(new Runnable() {
            @Override
            public void run() {
                callBack.onSuccess(statusCode, headers, model);
                callBack.onFinish();
            }
        });
    }

    public <T> void sendStartMessage(final ResultCallBack<T> callBack) {
        delivery.post(new Runnable() {
            @Override
            public void run() {
                callBack.onStart();
            }
        });
    }

    public <T> void sendProgressMessage(final long bytesWritten, final long totalSize, final ResultCallBack<T> callBack) {
        delivery.post(new Runnable() {
            @Override
            public void run() {
                callBack.onProgress(bytesWritten, totalSize);
            }
        });
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public TrustManager[] prepareTrustManager(InputStream... certificates) {
        if (certificates == null || certificates.length <= 0) return null;

        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);

            int index = 0;
            for (InputStream cerficate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(cerficate));

                try {
                    if (cerficate != null) {
                        cerficate.close();
                    }
                } catch (IOException e) {
                }
            }

            TrustManagerFactory trustManagerFactory = null;
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            return trustManagerFactory.getTrustManagers();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public KeyManager[] prepareKeyManager(InputStream bksFile, String password) {
        try {
            if (bksFile == null || password == null) return null;

            KeyStore clientKeyStore = KeyStore.getInstance("BKS");
            clientKeyStore.load(bksFile, password.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, password.toCharArray());
            return keyManagerFactory.getKeyManagers();

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setCertificates(InputStream[] certificates, InputStream bksFile, String password) {
        try {
            TrustManager[] trustManagers = prepareTrustManager(certificates);
            KeyManager[] keyManagers = prepareKeyManager(bksFile, password);
            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(keyManagers, new TrustManager[]{new MyTrustManager(chooseTrustManager(trustManagers))}, new SecureRandom());
            okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    private X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }


    private class MyTrustManager implements X509TrustManager {
        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;

        public MyTrustManager(X509TrustManager localTrustManager) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory var4 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            var4.init((KeyStore) null);
            defaultTrustManager = chooseTrustManager(var4.getTrustManagers());
            this.localTrustManager = localTrustManager;
        }


        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }


        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * 设置通用头信息
     *
     * @param headers
     */
    public RequestClient setCommonHeaders(final RequestHeaders headers) {

        okHttpClient.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {

                Headers localHeaders = chain.request().headers();
                Request wrapperReq = null;

                if (headers == null || headers.getMap().size() < 1)
                    return chain.proceed(chain.request());

                if (localHeaders == null || localHeaders.size() < 1) {
                    wrapperReq = chain.request().newBuilder().headers(headers.getHeaders()).build();
                } else {
                    Headers.Builder builder = localHeaders.newBuilder();
                    for (String key : headers.getMap().keySet()) {
                        if (TextUtils.isEmpty(localHeaders.get(key))) {
                            builder.add(key, headers.getMap().get(key));
                        }
                    }
                    wrapperReq = chain.request().newBuilder().headers(builder.build()).build();
                }

                return chain.proceed(wrapperReq);
            }
        });
        return this;
    }


    /**
     * 设置代理服务器
     *
     * @param proxyServer
     * @param port
     */
    public RequestClient setProxy(String proxyServer, int port) {
        okHttpClient.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServer, port)));
        return this;
    }

    /**
     * 设置调试模式
     *
     * @param debug
     * @return
     */
    public RequestClient debug(boolean debug) {
        L.DEBUG = debug;
        return this;
    }


    public static class L {

        private static final String TAG = "NET_DEBUG";

        public static boolean DEBUG = true;

        public static void i(Object msg) {
            if (DEBUG) {
                Log.i(TAG, msg.toString());
            }
        }

        public static void e(Object msg) {
            if (DEBUG) {
                Log.e(TAG, msg.toString());
            }
        }

        public static void e(Throwable throwable) {
            if (DEBUG) {
                Log.e(TAG, throwable.getMessage());
            }
        }
    }

}
