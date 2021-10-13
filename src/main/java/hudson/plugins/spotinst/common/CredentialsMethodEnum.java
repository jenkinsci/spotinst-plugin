package hudson.plugins.spotinst.common;

/**
 * Created by Liron Arad on 06/10/2021.
 */
public enum CredentialsMethodEnum {
    CredentialsStore("Credentials Store"),
    PlainText("Plain Text");

    private String name;

    CredentialsMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
