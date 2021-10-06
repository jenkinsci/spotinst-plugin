package hudson.plugins.spotinst.cloud;

public class BasicSpotCredentials implements SpotCredentials {
    private final String spotToken;
    private final String credentialsId;

    public BasicSpotCredentials(String credentialsId, String spotToken) {
        if (credentialsId == null) {
            throw new IllegalArgumentException("Credentials ID cannot be null.");
        }
        if (spotToken == null) {
            throw new IllegalArgumentException("Spot Token cannot be null.");
        }

        this.credentialsId = credentialsId;
        this.spotToken = spotToken;
    }

    @Override
    public String getSpotToken() {
        return spotToken;
    }

    @Override
    public String getCredentialsId() {
        return credentialsId;
    }
}
