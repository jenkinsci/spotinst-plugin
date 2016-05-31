package hudson.plugins.spotinst.common;

public class RestResponse {

    private int statusCode;
    private String body;

    public RestResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}
