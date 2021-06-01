package hudson.plugins.spotinst.api.infra;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static hudson.plugins.spotinst.common.Constants.*;

public class RestClient {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);
    //endregion

    //region Public Methods
    public static RestResponse sendGet(String url, Map<String, String> headers,
                                       Map<String, String> queryParams) throws ApiException {

        HttpGet getRequest = new HttpGet(url);
        addQueryParams(getRequest, queryParams);
        addHeaders(getRequest, headers);
        RestResponse retVal = sendRequest(getRequest);

        return retVal;
    }

    public static RestResponse sendPut(String url, String body, Map<String, String> headers,
                                       Map<String, String> queryParams) throws ApiException {

        HttpPut putRequest = new HttpPut(url);

        if (body != null) {
            StringEntity entity = null;
            try {
                entity = new StringEntity(body);
            }
            catch (UnsupportedEncodingException e) {
                LOGGER.error("Exception when building put body", e);
            }
            putRequest.setEntity(entity);
        }

        addQueryParams(putRequest, queryParams);
        addHeaders(putRequest, headers);
        RestResponse retVal = sendRequest(putRequest);

        return retVal;
    }
    //endregion

    //region Private Methods
    private static RestResponse sendRequest(HttpUriRequest urlRequest) throws ApiException {
        RestResponse retVal = null;

        RequestConfig.Builder configBuilder = RequestConfig.custom();

        configBuilder.setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT_IN_SECONDS * 1000);
        configBuilder.setConnectionRequestTimeout(REST_CLIENT_CONNECTION_REQUEST_TIMEOUT_IN_SECONDS * 1000);
        configBuilder.setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT_IN_SECONDS * 1000);

        RequestConfig config = configBuilder.build();

        HttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        CloseableHttpResponse response = null;
        try {
            response = (CloseableHttpResponse) httpclient.execute(urlRequest);
            retVal = buildRestResponse(response);

        }
        catch (IOException e) {
            LOGGER.error("Exception when executing http request", e);
            throw new ApiException("Exception in http request", e);
        }
        finally {
            if (response != null) {
                try {
                    response.close();
                }
                catch (IOException e) {
                    LOGGER.error("Exception when closing http response", e);
                }
            }
        }

        return retVal;
    }

    private static void addQueryParams(HttpRequestBase request, Map<String, String> queryParams) {
        if (queryParams != null) {
            URIBuilder builder = new URIBuilder(request.getURI());
            for (Map.Entry<String, String> currEntry : queryParams.entrySet()) {
                builder.addParameter(currEntry.getKey(), currEntry.getValue());
            }

            URI newUri = null;
            try {
                newUri = builder.build();
            }
            catch (URISyntaxException e) {
                LOGGER.error("Exception when building get url", e);
            }

            request.setURI(newUri);
        }
    }

    private static void addHeaders(HttpRequestBase request, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> currEntry : headers.entrySet()) {
                request.addHeader(currEntry.getKey(), currEntry.getValue());
            }
        }
    }

    private static RestResponse buildRestResponse(HttpResponse response) throws ApiException {
        RestResponse   retVal;
        BufferedReader rd = null;

        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));

            StringBuffer result = new StringBuffer();
            String       line   = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            retVal = new RestResponse(response.getStatusLine().getStatusCode(), result.toString());

        }
        catch (IOException e) {
            LOGGER.error("Exception when building Rest response.", e);
            throw new ApiException("Exception when building Rest response.", e);
        }
        finally {
            if (rd != null) {
                try {
                    rd.close();
                }
                catch (IOException ex) {
                    LOGGER.warn("Failed to close buffer", ex);
                }
            }
        }

        return retVal;
    }
    //endregion
}
