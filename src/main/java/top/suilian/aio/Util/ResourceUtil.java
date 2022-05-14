package top.suilian.aio.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceUtil {

	protected static Logger logger = LoggerFactory.getLogger(ResourceUtil.class);
	public static final int DEFAULT_SOCKET_TIME_OUT = 60000;
	public static final int DEFAULT_READ_TIME_OUT = 10*1000;
	/**
	 * 取出异常信息
	 * 
	 * @param ex
	 * @return
	 */
	public static String getExceptionMessage(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		return sw.toString();
	}

	public static String httpsRequest(String requestUrl, String requestMethod, String outputStr) {
		return httpsRequest(requestUrl, requestMethod, outputStr, "UTF-8", null);
	}

	public static String httpsRequest(String requestUrl, String requestMethod, String outputStr, String charsetName) {
		return httpsRequest(requestUrl, requestMethod, outputStr, charsetName, null);
	}

	public static String httpsRequest(String requestUrl, String requestMethod, String outputStr, String charsetName,
			Map<String, String> requestPropertyMap) {
		String result = null;
		StringBuffer buffer = new StringBuffer();
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			httpUrlConn.setConnectTimeout(DEFAULT_SOCKET_TIME_OUT);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if (requestPropertyMap != null && requestPropertyMap.keySet().size() > 0) {
				for (String key : requestPropertyMap.keySet()) {
					String property = requestPropertyMap.get(key);
					httpUrlConn.setRequestProperty(key, property);
				}
			}

			if ("GET".equalsIgnoreCase(requestMethod)) {
				httpUrlConn.connect();
			}

			// 当有数据需要提交时
			if (!StringUtils.isEmpty(outputStr)) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes(charsetName));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charsetName);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			result = buffer.toString();

		} catch (Exception e) {
			logger.error(getExceptionMessage(e));
		}
		return result;
	}

	public static String httpsRequest(String requestUrl, String requestMethod, String outputStr, String charsetName,
			Map<String, String> requestPropertyMap, Integer timeout) {
		String result = null;
		StringBuffer buffer = new StringBuffer();
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			httpUrlConn.setConnectTimeout(timeout);
			// httpUrlConn.setReadTimeout(timeout);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if (requestPropertyMap != null && requestPropertyMap.keySet().size() > 0) {
				for (String key : requestPropertyMap.keySet()) {
					String property = requestPropertyMap.get(key);
					httpUrlConn.setRequestProperty(key, property);
				}
			}

			if ("GET".equalsIgnoreCase(requestMethod)) {
				httpUrlConn.connect();
			}

			// 当有数据需要提交时
			if (!StringUtils.isEmpty(outputStr)) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes(charsetName));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charsetName);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			result = buffer.toString();

		} catch (Exception e) {
			logger.error(getExceptionMessage(e));
		}
		return result;
	}

	public static String httpRequest(String requestUrl, String requestMethod, String outputStr) {
		return httpRequest(requestUrl, requestMethod, outputStr, "UTF-8", null);
	}

	public static String httpRequest(String requestUrl, String requestMethod, String outputStr, String charsetName) {
		return httpRequest(requestUrl, requestMethod, outputStr, charsetName, null);
	}

	public static String httpRequest(String requestUrl, String requestMethod, String outputStr, String charsetName,
			Map<String, String> requestPropertyMap) {
		String result = null;
		StringBuffer buffer = new StringBuffer();
		try {

			URL url = new URL(requestUrl);
			HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			httpUrlConn.setConnectTimeout(DEFAULT_SOCKET_TIME_OUT);
			httpUrlConn.setReadTimeout(DEFAULT_READ_TIME_OUT);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if (requestPropertyMap != null && requestPropertyMap.keySet().size() > 0) {
				for (String key : requestPropertyMap.keySet()) {
					String property = requestPropertyMap.get(key);
					httpUrlConn.setRequestProperty(key, property);
				}
			}

			if ("GET".equalsIgnoreCase(requestMethod)) {
				httpUrlConn.connect();
			}

			// 当有数据需要提交时
			if (!StringUtils.isEmpty(outputStr)) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes(charsetName));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charsetName);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			result = buffer.toString();

		} catch (Exception e) {
			logger.error(getExceptionMessage(e));
		}
		return result;
	}

	public static String doPostForm(String url, Map<String, Object> param,Map<String,String> head) throws Exception {
		String result = null;
		try {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setHeader("Content-Type", head.get("Content-Type"));
			List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			if (param != null && !param.isEmpty()) {
				for (Map.Entry<String, Object> entry : param.entrySet()) {
					if (entry.getValue() != null) {
						parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
					}
				}
			}
			HttpEntity entity = new UrlEncodedFormEntity(parameters, "UTF-8");
			httpPost.setEntity(entity);
			result = client(httpPost, null);
			return result;
		} catch (Exception e) {
			logger.error(getExceptionMessage(e));
			throw e;
		}
	}
	private static String client(HttpRequestBase request, Integer timeout) throws Exception {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		CloseableHttpResponse response = null;
		try {
			if (timeout == null) {
				timeout = 10*1000;
			}
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout)
					.setConnectTimeout(timeout).build();
			request.setConfig(requestConfig);
			response = httpclient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					return EntityUtils.toString(entity, "UTF-8");
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (httpclient != null) {
					httpclient.close();
				}
			} catch (IOException e) {
			}
		}
		return null;
	}
}
