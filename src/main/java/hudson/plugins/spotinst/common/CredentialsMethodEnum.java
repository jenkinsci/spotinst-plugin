package hudson.plugins.spotinst.common;

/**
 * Created by Shibel Karmi Mansour on 30/12/2021.
 */
public enum CredentialsMethodEnum {
    CredentialsStore("Credentials Store"),
    GlobalConfiguration("Global configuration (plain text)");

    private String name;

    CredentialsMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
