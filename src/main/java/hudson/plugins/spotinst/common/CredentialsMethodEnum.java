package hudson.plugins.spotinst.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Liron Arad on 06/10/2021.
 */
public enum CredentialsMethodEnum {
    CredentialsStore("Credentials Store"),
    PlainText("Plain Text");

    // region members
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsMethodEnum.class);

    private String name;
    // endregion

    CredentialsMethodEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public CredentialsMethodEnum fromName(String name){
        CredentialsMethodEnum retVal = null;

        for (CredentialsMethodEnum enumValues : CredentialsMethodEnum.values()) {
            if (enumValues.name.equals(name)) {
                retVal = enumValues;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.warn(String.format(
                    "Tried to create CredentialsMethodEnum for name: %s, but we don't support such type ", name));
        }
        return retVal;
   }

}
