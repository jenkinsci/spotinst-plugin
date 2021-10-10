package hudson.plugins.spotinst.common;

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
