package com.mt.agent.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp工具类，用于发送HTTP请求
 */
@Slf4j
public class OkHttpUtil {

    /**
     * 连接超时时间(秒)
     */
    private static final int CONNECT_TIMEOUT = 10;

    /**
     * 读取超时时间(秒)
     */
    private static final int READ_TIMEOUT = 300;

    /**
     * 写入超时时间(秒)
     */
    private static final int WRITE_TIMEOUT = 10;

    /**
     * OkHttpClient实例
     */
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    /**
     * JSON媒体类型
     */
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 表单媒体类型
     */
    private static final MediaType FORM_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    /**
     * 文件媒体类型
     */
    private static final MediaType FILE_TYPE = MediaType.parse("multipart/form-data");

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * 发送带请求头的GET请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送查询参数的GET请求
     *
     * @param url    请求URL
     * @param params 查询参数
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response getWithParams(String url, Map<String, String> params) throws IOException {
        return getWithParams(url, params, null);
    }

    /**
     * 发送带查询参数和请求头的GET请求
     *
     * @param url     请求URL
     * @param params  查询参数
     * @param headers 请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response getWithParams(String url, Map<String, String> params, Map<String, String> headers)
            throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(urlBuilder::addQueryParameter);
        }

        Request.Builder builder = new Request.Builder().url(urlBuilder.build()).get();
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送POST请求，JSON格式数据
     *
     * @param url  请求URL
     * @param json JSON字符串
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response postJson(String url, String json) throws IOException {
        return postJson(url, json, null);
    }

    /**
     * 发送带请求头的POST请求，JSON格式数据
     *
     * @param url     请求URL
     * @param json    JSON字符串
     * @param headers 请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response postJson(String url, String json, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(json, JSON_TYPE);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送POST请求，表单格式数据
     *
     * @param url    请求URL
     * @param params 表单参数
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response postForm(String url, Map<String, String> params) throws IOException {
        return postForm(url, params, null);
    }

    /**
     * 发送带请求头的POST请求，表单格式数据
     *
     * @param url     请求URL
     * @param params  表单参数
     * @param headers 请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response postForm(String url, Map<String, String> params, Map<String, String> headers)
            throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(formBuilder::add);
        }

        RequestBody body = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送POST请求，文件上传
     *
     * @param url      请求URL
     * @param params   表单参数
     * @param fileKey  文件参数名
     * @param filePath 文件路径
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response postFile(String url, Map<String, String> params, String fileKey, String filePath)
            throws IOException {
        return postFile(url, params, fileKey, filePath, null);
    }

    /**
     * 发送带请求头的POST请求，文件上传
     *
     * @param url      请求URL
     * @param params   表单参数
     * @param fileKey  文件参数名
     * @param filePath 文件路径
     * @param headers  请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response postFile(String url, Map<String, String> params, String fileKey, String filePath,
            Map<String, String> headers) throws IOException {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        // 添加表单参数
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(multipartBuilder::addFormDataPart);
        }

        // 添加文件
        if (StringUtils.hasText(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
                multipartBuilder.addFormDataPart(fileKey, file.getName(), fileBody);
            } else {
                log.error("文件不存在: {}", filePath);
            }
        }

        RequestBody body = multipartBuilder.build();
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送PUT请求，JSON格式数据
     *
     * @param url  请求URL
     * @param json JSON字符串
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response putJson(String url, String json) throws IOException {
        return putJson(url, json, null);
    }

    /**
     * 发送带请求头的PUT请求，JSON格式数据
     *
     * @param url     请求URL
     * @param json    JSON字符串
     * @param headers 请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response putJson(String url, String json, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(json, JSON_TYPE);
        Request.Builder builder = new Request.Builder().url(url).put(body);
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送DELETE请求
     *
     * @param url 请求URL
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response delete(String url) throws IOException {
        return delete(url, null);
    }

    /**
     * 发送带请求头的DELETE请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    public static Response delete(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).delete();
        addHeaders(builder, headers);
        Request request = builder.build();
        return executeSync(request);
    }

    /**
     * 发送异步GET请求
     *
     * @param url      请求URL
     * @param callback 回调接口
     */
    public static void getAsync(String url, Callback callback) {
        getAsync(url, null, callback);
    }

    /**
     * 发送带请求头的异步GET请求
     *
     * @param url      请求URL
     * @param headers  请求头
     * @param callback 回调接口
     */
    public static void getAsync(String url, Map<String, String> headers, Callback callback) {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        Request request = builder.build();
        executeAsync(request, callback);
    }

    /**
     * 发送异步POST请求，JSON格式数据
     *
     * @param url      请求URL
     * @param json     JSON字符串
     * @param callback 回调接口
     */
    public static void postJsonAsync(String url, String json, Callback callback) {
        postJsonAsync(url, json, null, callback);
    }

    /**
     * 发送带请求头的异步POST请求，JSON格式数据
     *
     * @param url      请求URL
     * @param json     JSON字符串
     * @param headers  请求头
     * @param callback 回调接口
     */
    public static void postJsonAsync(String url, String json, Map<String, String> headers, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON_TYPE);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        Request request = builder.build();
        executeAsync(request, callback);
    }

    /**
     * 添加请求头
     *
     * @param builder 请求构建器
     * @param headers 请求头
     */
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (!CollectionUtils.isEmpty(headers)) {
            headers.forEach(builder::addHeader);
        }
    }

    /**
     * 执行同步请求
     *
     * @param request 请求对象
     * @return Response响应对象
     * @throws IOException 如果请求失败
     */
    private static Response executeSync(Request request) throws IOException {
        Response response = CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            log.error("HTTP请求失败: {}, 状态码: {}", request.url(), response.code());
        }
        return response;
    }

    /**
     * 执行异步请求
     *
     * @param request  请求对象
     * @param callback 回调接口
     */
    private static void executeAsync(Request request, Callback callback) {
        CLIENT.newCall(request).enqueue(callback);
    }
}
