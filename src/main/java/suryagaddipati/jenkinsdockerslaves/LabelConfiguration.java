package suryagaddipati.jenkinsdockerslaves;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelConfiguration  implements Describable<LabelConfiguration> {
    private String tmpfsDir;
    private long limitsNanoCPUs;
    private long limitsMemoryBytes;
    private long reservationsNanoCPUs;
    private long reservationsMemoryBytes;
    String image;
    String hostBinds;
    private String label;
    private String cacheDir;
    private String envVars;
    private String baseWorkspaceLocation;
    private String placementConstraints;

    public  LabelConfiguration(){
        //For Yaml Load
    }

    @DataBoundConstructor
    public LabelConfiguration(final String image, final String hostBinds,
                              final String label,
                              final String cacheDir,final String tmpfsDir,
                              final String envVars,
                              final long limitsNanoCPUs, final long limitsMemoryBytes,
                              final long reservationsNanoCPUs, final long reservationsMemoryBytes, final String baseWorkspaceLocation, final String placementConstraints) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.label = label;
        this.cacheDir = cacheDir;
        this.tmpfsDir = tmpfsDir;
        this.limitsNanoCPUs = limitsNanoCPUs;
        this.limitsMemoryBytes = limitsMemoryBytes;
        this.reservationsNanoCPUs = reservationsNanoCPUs;
        this.reservationsMemoryBytes = reservationsMemoryBytes;
        this.envVars = envVars;
        this.baseWorkspaceLocation = baseWorkspaceLocation;
        this.placementConstraints = placementConstraints;
    }

    public String[] getCacheDirs() {
        return StringUtils.isEmpty(this.cacheDir) ? new String[]{} : this.cacheDir.split(" ");
    }

    public String getLabel() {
        return this.label;
    }

    public String getImage() {
        return this.image;
    }

    public String[] getHostBindsConfig() {
        return StringUtils.isEmpty(this.hostBinds) ? new String[]{} : this.hostBinds.split(" ");
    }

    public String[] getEnvVarsConfig() {
        return StringUtils.isEmpty(this.envVars) ? new String[]{} : this.envVars.split(" ");
    }
    public long getLimitsNanoCPUs() {
        return limitsNanoCPUs;
    }

    public long getLimitsMemoryBytes() {
        return limitsMemoryBytes;
    }

    public long getReservationsNanoCPUs() {
        return reservationsNanoCPUs;
    }

    public long getReservationsMemoryBytes() {
        return reservationsMemoryBytes;
    }

    public String getPlacementConstraints() {
        return placementConstraints;
    }

    public String getTmpfsDir() {
        return tmpfsDir;
    }
    public String getEnvVars() {
        return envVars;
    }

    @Override
    public Descriptor<LabelConfiguration> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(getClass());
    }

    public String getBaseWorkspaceLocation() {
        return this.baseWorkspaceLocation;
    }

    public String[] getPlacementConstraintsConfig() {
        return StringUtils.isEmpty(this.placementConstraints) ? new String[]{} : this.placementConstraints.split(";");
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<LabelConfiguration> {
        @Override
        public String getDisplayName() {
            return "Docker Agent Template";
        }
    }
}
