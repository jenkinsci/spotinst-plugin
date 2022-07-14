package hudson.plugins.spotinst.slave;

import hudson.slaves.OfflineCause;

public class SpotinstSingleTaskOfflineCause extends OfflineCause.SimpleOfflineCause {
    public SpotinstSingleTaskOfflineCause(SpotinstNonLocalizable nonLocalizable) {
        super(nonLocalizable);
    }
}
