package hudson.plugins.spotinst.common;

/**
 * Created by ohadmuchnik on 24/05/2016.
 */
public class SpotinstContext {

    //region Members
    private static SpotinstContext instance;
    private        String          spotinstToken;
    //endregion

    public static SpotinstContext getInstance() {
        if (instance == null) {
            instance = new SpotinstContext();
        }
        return instance;
    }

    //region Public Methods
    public String getSpotinstToken() {
        return spotinstToken;
    }

    public void setSpotinstToken(String spotinstToken) {
        this.spotinstToken = spotinstToken;
    }

    //endregion
}
