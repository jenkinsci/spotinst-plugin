package hudson.plugins.spotinst.api.infra;

public class RestResponse {

    private final int statusCode;
    private final String body;

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
