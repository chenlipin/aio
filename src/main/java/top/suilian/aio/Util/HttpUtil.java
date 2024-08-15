    package top.suilian.aio.Util;


    import com.alibaba.fastjson.JSON;
    import net.sf.json.JSONObject;
    import org.apache.http.*;
    import org.apache.http.client.ClientProtocolException;
    import org.apache.http.client.config.RequestConfig;
    import org.apache.http.client.entity.UrlEncodedFormEntity;
    import org.apache.http.client.methods.*;
    import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
    import org.apache.http.entity.ContentType;
    import org.apache.http.entity.StringEntity;
    import org.apache.http.entity.mime.HttpMultipartMode;
    import org.apache.http.entity.mime.MultipartEntityBuilder;
    import org.apache.http.entity.mime.content.StringBody;
    import org.apache.http.impl.client.CloseableHttpClient;
    import org.apache.http.impl.client.DefaultHttpClient;
    import org.apache.http.impl.client.HttpClientBuilder;
    import org.apache.http.impl.client.HttpClients;
    import org.apache.http.message.BasicNameValuePair;
    import org.apache.http.util.CharArrayBuffer;
    import org.apache.http.util.EntityUtils;
    import org.apache.log4j.Logger;
    import org.springframework.stereotype.Component;

    import javax.net.ssl.SSLContext;
    import javax.net.ssl.TrustManager;
    import javax.net.ssl.X509TrustManager;
    import java.io.*;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.net.URLConnection;
    import java.net.URLEncoder;
    import java.security.KeyManagementException;
    import java.security.NoSuchAlgorithmException;
    import java.security.cert.X509Certificate;
    import java.util.*;
    import java.util.Map.Entry;

@Component
public class HttpUtil {
    private static Logger logger = Logger.getLogger(HttpUtil.class);


    public static String doPostFormData(String url, Map<String, String> paramMap) {
        // 创建Http实例
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建HttpPost实例
        HttpPost httpPost = new HttpPost(url);

        // 请求参数配置
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000)
                .setConnectionRequestTimeout(10000).build();
        httpPost.setConfig(requestConfig);

        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(java.nio.charset.Charset.forName("UTF-8"));
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            for(Map.Entry<String, String> entry: paramMap.entrySet()) {
                builder.addPart(entry.getKey(),new StringBody(entry.getValue(), ContentType.create("text/plain", Consts.UTF_8)));
            }

            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);// 执行提交

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // 返回
                String res = EntityUtils.toString(response.getEntity(), java.nio.charset.Charset.forName("UTF-8"));
                return res;
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("调用HttpPost失败！" + e.toString());
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("关闭HttpPost连接失败！");
                }
            }
        }
        return null;
    }

    /**
     * post请求，参数为json字符串
     *
     * @param url           请求地址
     * @param json<HashMap> params
     * @return 响应
     */
    //post json提交
    public String post(String url, String json) {

        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        String result = null;
        CloseableHttpResponse response = null;
        try {
            StringEntity s = new StringEntity(json, "utf-8");
            s.setContentEncoding("UTF-8");
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            post.setEntity(s);
            response = httpclient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity());// 返回json格式：
            } else {
                result = EntityUtils.toString(response.getEntity());
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String deleted(String url,String json) throws UnsupportedEncodingException,IOException{
        String result = null;
        DefaultHttpClient httpClient = new DefaultHttpClient();

        MyHttpDelete delete = new MyHttpDelete(url);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        StringEntity input = new StringEntity(json, "utf-8");
        delete.setEntity(input);

        HttpResponse response = httpClient.execute(delete);
        if (response != null && response.getStatusLine().getStatusCode() == 200) {
            result = EntityUtils.toString(response.getEntity());// 返回json格式：
        } else {
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }

    public static String delete(String url, String jsonString) throws IOException {
        String result = null;
        HttpResponse response;
        CloseableHttpClient client = HttpClientBuilder.create().build();

        MyHttpDelete delete = new MyHttpDelete(url);
        delete.setEntity(new StringEntity(jsonString));


        delete.addHeader("content-type", "application/json");

        response = client.execute(delete);
        if (response != null && response.getStatusLine().getStatusCode() == 200) {
            result = EntityUtils.toString(response.getEntity());// 返回json格式：
        } else {
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }



    public static String post(String url, Map<String, Object> params) throws UnsupportedEncodingException {
        List<NameValuePair> list = new ArrayList<>();
        for (String key : params.keySet()) {
            list.add(new BasicNameValuePair(key, params.get(key).toString()));
        }
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        UrlEncodedFormEntity entityParam = new UrlEncodedFormEntity(list, "UTF-8");
        entityParam.setContentEncoding("UTF-8");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(entityParam);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() != 100) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String postBasic(String url, Map<String, String> params) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("Charset", "UTF-8");
        con.setRequestMethod("POST");
        con.setConnectTimeout(30 * 1000);
        con.setReadTimeout(100 * 1000);
        con.setDoOutput(true);
        if (params != null) {
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            StringBuffer httpParams = new StringBuffer();
            for (Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                httpParams.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
            }
            if (httpParams.length() > 0) {
                httpParams.deleteCharAt(httpParams.length() - 1);
            }
            wr.writeBytes(httpParams.toString());
            wr.flush();
            wr.close();
        }
        return toConnection(con);
    }

    public static String toConnection(HttpURLConnection con) throws Exception {
        int responseCode = con.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            System.out.println("Request failed:" + responseCode);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringWriter writer = new StringWriter();
        char[] chars = new char[1024];
        int count = 0;
        while ((count = in.read(chars)) > 0) {
            writer.write(chars, 0, count);
        }
        return writer.toString();
    }


    private static  RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();//设置请求和传输超时时间

    public static String sslPost(String httpUrl, Map<String, Object> params) throws Exception {
        String result = null ;
        CloseableHttpClient httpclient = createSSLClientDefault();
        //httpclient.
        //httpclient.
        BufferedReader in = null ;
        HttpPost httpPost = new HttpPost(httpUrl);
        httpPost.setConfig(requestConfig);
        List <NameValuePair> list = new ArrayList <NameValuePair>();
        StringBuffer paramsBuf = new StringBuffer() ;
        for (String key : params.keySet()) {
            list.add(new BasicNameValuePair(key, params.get(key).toString()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(list, "UTF-8"));
        try {
//          报文参数27：&id=jn-3-767744&groupPlatProTerminalId=119667&extend=uwJZ8j3CkpGPL4rM5J6KJhjR99O7yAe3BAGLS8ooI8ijNqKHfzTaK6W9wQvjZEVOmWJ3HxFb2O9D
//          wDbe3++UiQ==&xxtCode=370000&terminalType=1&role=3&type=3
            System.out.println("post请求报文地址：" + httpUrl+"?"+paramsBuf.toString()) ;
            CloseableHttpResponse response = httpclient.execute(httpPost);
            InputStream content = response.getEntity().getContent() ;
            in = new BufferedReader(new InputStreamReader(content, "UTF-8"));
//          in = new BufferedReader(new InputStreamReader(content, "GBK"));
//          in = new BufferedReader(new InputStreamReader(content));
            StringBuilder sb = new StringBuilder();
            String line = "" ;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString() ;
            System.out.println("响应报文：" + result) ;
            //  响应报文：{"ret":0,"msg":"成功","callbackurl":"https://edu.10086.cn/test-sso/login?service=http%3A%2F%2F112.35.7.169%3A9010%2Feducloud%2Flogin%2Flogin%3Ftype%3D3%26mode%3D1%26groupId%3D4000573%26provincePlatformId%3D54","accesstoken":"2467946a-bee9-4d8c-8cce-d30181073b75"}Í

            return result ;
        } catch (Exception e) {
            e.printStackTrace() ;
        } finally {
            httpclient.close();
        }
        return null ;
    }



    public static CloseableHttpClient createSSLClientDefault(){

        try {
            //SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            // 在JSSE中，证书信任管理器类就是实现了接口X509TrustManager的类。我们可以自己实现该接口，让它信任我们指定的证书。
            // 创建SSLContext对象，并使用我们指定的信任管理器初始化
            //信任所有
            X509TrustManager x509mgr = new X509TrustManager() {

                //　　该方法检查客户端的证书，若不信任该证书则抛出异常
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) {
                }
                // 　　该方法检查服务端的证书，若不信任该证书则抛出异常
                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) {
                }
                // 　返回受信任的X509证书数组。
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { x509mgr }, null);
            ////创建HttpsURLConnection对象，并设置其SSLSocketFactory对象
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            //  HttpsURLConnection对象就可以正常连接HTTPS了，无论其证书是否经权威机构的验证，只要实现了接口X509TrustManager的类MyX509TrustManager信任该证书。
            return HttpClients.custom().setSSLSocketFactory(sslsf).build();


        } catch (KeyManagementException e) {

            e.printStackTrace();

        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();

        } catch (Exception e) {

            e.printStackTrace();

        }

        // 创建默认的httpClient实例.
        return  HttpClients.createDefault();

    }
    public static String sendMultipartFormData(String url, Map<String, String> stringParams) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            for (Map.Entry<String, String> entry : stringParams.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue(), ContentType.TEXT_PLAIN.withCharset("UTF-8"));
            }

            HttpEntity multipartEntity = builder.build();
            httpPost.setEntity(multipartEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    return EntityUtils.toString(responseEntity, "UTF-8");
                }
            }
        }
        return null;
    }


    public static String post(String url, Map<String, String> params, HashMap<String, String> headers) throws UnsupportedEncodingException {
        List<NameValuePair> list = new LinkedList<>();
        for (String key : params.keySet()) {
            BasicNameValuePair param1 = new BasicNameValuePair(key, String.valueOf(params.get(key)));
            list.add(param1);
        }
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        UrlEncodedFormEntity entityParam = new UrlEncodedFormEntity(list, "UTF-8");
        post.setEntity(entityParam);
//        post.addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.6)");
        boolean contentType = true;
        for (String key : headers.keySet()) {
            if ("Content-Type".equals(key)) {
                contentType = false;
            }
            post.addHeader(key, headers.get(key));
        }
        if (contentType) {
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null ) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     * get请求，参数拼接在地址上
     *
     * @param url 请求地址加参数
     * @return 响应
     */
    public static String get(String url) {
        String result = null;
        HttpClientBuilder builder = HttpClients.custom();
        builder.setUserAgent("Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");
        final CloseableHttpClient httpClient = builder.build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpGet get = new HttpGet(url);
        get.setConfig(config);
        get.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(get);
            if (response != null && response.getStatusLine().getStatusCode() != 000) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            } else {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
                logger.info(url + "错误返回结果" + result);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static String entityToString(HttpEntity entity) throws IOException {
        String result = null;
        if (entity != null) {
            long lenth = entity.getContentLength();
            if (lenth != -1 && lenth < 2048) {
                result = EntityUtils.toString(entity, "UTF-8");
            } else {
                InputStreamReader reader1 = new InputStreamReader(entity.getContent(), "UTF-8");
                CharArrayBuffer buffer = new CharArrayBuffer(2048);
                char[] tmp = new char[1024];
                int l;
                while ((l = reader1.read(tmp)) != -1) {
                    buffer.append(tmp, 0, l);
                }
                result = buffer.toString();
            }
        }
        return result;
    }

    public static  String doGet(String url, String apikey) {
        String result = null;
        HttpClientBuilder builder = HttpClients.custom();
        builder.setUserAgent("Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");

        final CloseableHttpClient httpClient = builder.build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpGet get = new HttpGet(url);
        get.setConfig(config);
        get.setHeader("Content-type", "application/x-www-form-urlencoded");
        get.addHeader("X-MBX-APIKEY", apikey);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(get);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            } else {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //post json提交
    public String doPost(String url, String json, String apiKey) {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        String result = null;
        CloseableHttpResponse response = null;
        try {
            StringEntity s = new StringEntity(json, "utf-8");
            s.setContentEncoding("UTF-8");
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            post.setHeader("User-Agent","Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");
            post.addHeader("X-MBX-APIKEY", apiKey);
            post.setEntity(s);
            response = httpclient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity());// 返回json格式：
            } else {
                result = EntityUtils.toString(response.getEntity());
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public String doPostgate(String url, String json,  HashMap<String, String> headers) {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        String result = null;
        CloseableHttpResponse response = null;
        try {
            StringEntity s = new StringEntity(json, "utf-8");
            s.setContentEncoding("UTF-8");
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            post.setHeader("User-Agent","Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");
            for (String key : headers.keySet()) {
                post.addHeader(key, headers.get(key));
            }
            post.setEntity(s);
            response = httpclient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity());// 返回json格式：
            } else {
                result = EntityUtils.toString(response.getEntity());
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String delete(String url) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
        httpDelete.setHeader("Content-type", "application/x-www-form-urlencoded");

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpDelete);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            return result;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }



    public static String doDeletes(String url,Map<String,String> header) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
        httpDelete.setHeader("Content-type", "application/json");
        Iterator<Map.Entry<String, String>> it = header.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            httpDelete.addHeader(entry.getKey(),entry.getValue());
        }
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpDelete);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            return result;
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public String delete(String url,HashMap<String,String> header) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
        Iterator<Map.Entry<String, String>> it = header.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            httpDelete.addHeader(entry.getKey(),entry.getValue());
        }
        httpDelete.setHeader("Content-type", "application/json");
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpDelete);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            return result;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public String doDelete(String url, String token, String apiKey) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
        httpDelete.setHeader("Content-type", "application/x-www-form-urlencoded");
        httpDelete.setHeader("User-Agent","Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");
        httpDelete.addHeader("X-MBX-APIKEY", apiKey);
        httpDelete.setHeader("DataEncoding", "UTF-8");
        httpDelete.setHeader("token", token);

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpDelete);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            return result;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String getJsonPost(JSONObject jsonObject, String method, String secretKey) {
        String url = method.trim();
        String signString = HMAC.jsonToString(JSONObject.fromObject(jsonObject).toString());
        String signature = HMAC.sha256_HMAC1(signString, secretKey);
        jsonObject.put("signature",signature);
        String json = "";
        try {
            json = sendPost(url, jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * 发送POST方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数
     * @return result 响应结果
     */
    public static String sendPost(String url, JSONObject param) {
        OutputStreamWriter out = null;
        BufferedReader in = null;
        String result = "";
        System.out.println(param.toString());
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            // 获取URLConnection对象对应的输出流
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            // 发送请求参数
            out.write(param.toString());
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
                logger.info("resultfubt:"+result);
            }
        } catch (Exception e) {
            logger.info("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        } finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static String httpDelete(String url,JSONObject pamas){

        //HttpResponse response = new HttpResponse();

        String encode = "utf-8";

        String content = null;
        //since 4.3 不再使用 DefaultHttpClient
        CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
        HttpDelete httpdelete = new HttpDelete(url);
        // 5、设置header信息

/**header中通用属性*/
httpdelete.setHeader("Accept","*/*");
httpdelete.setHeader("Accept-Encoding","gzip, deflate");
httpdelete.setHeader("Cache-Control","no-cache");
httpdelete.setHeader("Connection", "keep-alive");
httpdelete.setHeader("Content-Type", "application/json;charset=UTF-8");

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = closeableHttpClient.execute(httpdelete);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, encode);

        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                httpResponse.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {   //关闭连接、释放资源
            closeableHttpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * 发送GET方法的请求
     *
     * @param method   发送请求的方法
     * @param param 请求参数
     * @return result 响应结果
     */
    public static String sendGet(String method, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = method.trim()+param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            logger.info("发送GET请求出现异常！" + e);
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    public String put(String url,Map<String ,String> map ) {
        String result = null;
        HttpClientBuilder builder = HttpClients.custom();
        builder.setUserAgent("Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");
        final CloseableHttpClient httpClient = builder.build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPut put = new HttpPut(url);
        put.setConfig(config);
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            put.addHeader(entry.getKey(),entry.getValue());
        }
        put.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(put);
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            } else {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
                logger.info(url + "错误返回结果" + result);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String getAddHead(String url, Map<String, String> map) {
        String result = null;
        HttpClientBuilder builder = HttpClients.custom();
        builder.setUserAgent("Mozilla/5.0(Windows;U;Windows NT 5.1;en-US;rv:0.9.4)");
        final CloseableHttpClient httpClient = builder.build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpGet get = new HttpGet(url);
        get.setConfig(config);
//        get.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            get.addHeader(entry.getKey(),entry.getValue());
        }
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(get);
            if (response != null && response.getStatusLine().getStatusCode() != 100000) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            } else {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
                logger.info(url + "错误返回结果" + result);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String postes(String url, Map<String, Object> params) throws UnsupportedEncodingException {
        List<NameValuePair> list = new ArrayList<>();
        for (String key : params.keySet()) {
            list.add(new BasicNameValuePair(key, params.get(key).toString()));
        }
        StringEntity s = new StringEntity(JSON.toJSONString(params), "utf-8");
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept-Language","zh-cn");
        post.setEntity(s);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String postesss(String url, Map<String, Object> params,String str) throws UnsupportedEncodingException {
        List<NameValuePair> list = new ArrayList<>();
        for (String key : params.keySet()) {
            list.add(new BasicNameValuePair(key, params.get(key).toString()));
        }
        StringEntity s = new StringEntity(JSON.toJSONString(params), "utf-8");
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept-Language","zh-cn");
        post.setHeader("API-KEY",str);
        post.setEntity(s);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String postByPackcoin(String url, Map<String, String> params, HashMap<String, String> headers) throws UnsupportedEncodingException {
        List<NameValuePair> list = new LinkedList<>();
        for (String key : params.keySet()) {
            BasicNameValuePair param1 = new BasicNameValuePair(key, String.valueOf(params.get(key)));
            list.add(param1);
        }
        StringEntity body = new StringEntity(JSON.toJSONString(params), "utf-8");
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        // UrlEncodedFormEntity entityParam = new UrlEncodedFormEntity(list, "UTF-8");
        post.setEntity(body);
        post.addHeader("Content-Type", "application/json");
        boolean contentType = true;
        if (headers!=null) {
            for (String key : headers.keySet()) {
                if ("Content-Type".equals(key)) {
                    contentType = false;
                }
                post.addHeader(key, headers.get(key));
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() != 2000) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String postByPackcoin1(String url, Map<String, Object> params, HashMap<String, String> headers) throws UnsupportedEncodingException {
        List<NameValuePair> list = new LinkedList<>();
        for (String key : params.keySet()) {
            BasicNameValuePair param1 = new BasicNameValuePair(key, String.valueOf(params.get(key)));
            list.add(param1);
        }
        StringEntity body = new StringEntity(JSON.toJSONString(params), "utf-8");
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        // UrlEncodedFormEntity entityParam = new UrlEncodedFormEntity(list, "UTF-8");
        post.setEntity(body);
        post.addHeader("Content-Type", "application/json");
        boolean contentType = true;
        for (String key : headers.keySet()) {
            if ("Content-Type".equals(key)) {
                contentType = false;
            }
            post.addHeader(key, headers.get(key));
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() != 2000) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public String postByPackcoin2(String url, Map<String, String> headers) throws UnsupportedEncodingException {
        List<NameValuePair> list = new LinkedList<>();
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        post.addHeader("Content-Type", "application/json");
        boolean contentType = true;
        for (String key : headers.keySet()) {
            if ("Content-Type".equals(key)) {
                contentType = false;
            }
            post.addHeader(key, headers.get(key));
        }
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() != 2000) {
                HttpEntity entity = response.getEntity();
                result = entityToString(entity);
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static String doPostMart(String url, String json, Map<String, String> map) {
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(35000) //连接超时时间
                .setConnectionRequestTimeout(35000) //从连接池中取的连接的最长时间
                .setSocketTimeout(60000) //数据传输的超时时间
                .build();
        HttpPost post = new HttpPost(url);
        post.setConfig(config);
        String result = null;
        CloseableHttpResponse response = null;
        try {
            StringEntity s = new StringEntity(json, "utf-8");
            s.setContentEncoding("UTF-8");
            post.setHeader("Content-type", "application/json");
            for (String key : map.keySet()) {
                post.addHeader(key, map.get(key));
            }
            post.setEntity(s);
            response = httpclient.execute(post);
            if (response != null && response.getStatusLine().getStatusCode() != 45) {
                System.out.println("--0--"+JSON.toJSONString(response.getAllHeaders()));
                result = EntityUtils.toString(response.getEntity());// 返回json格式：
            } else {
                result = EntityUtils.toString(response.getEntity());
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {

                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}
