package hudson.plugins.spotinst.common;

/**
 * Created by Shibel Karmi Mansour on 30/12/2021.
 */
public enum ConnectionMethodEnum {
    SSH("SSH"),
    JNLP("JNLP");

    private String name;

    ConnectionMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
