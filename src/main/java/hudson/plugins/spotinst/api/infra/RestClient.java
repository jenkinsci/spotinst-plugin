package hudson.plugins.spotinst.api.infra;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
import java.util.Map;

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

        return sendRequest(getRequest);
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

        return sendRequest(putRequest);
    }
    //endregion

    //region Private Methods
    private static RestResponse sendRequest(HttpUriRequest urlRequest) throws ApiException {
        RestResponse retVal = null;

        HttpClient httpclient = HttpClientBuilder.create().build();

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
        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String       line   = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            return new RestResponse(response.getStatusLine().getStatusCode(), result.toString());
        }
        catch (IOException e) {
            LOGGER.error("Exception when building Rest response.", e);
            throw new ApiException("Exception when building Rest response.", e);
        }
    }
    //endregion
}
