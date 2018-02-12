package suryagaddipati.jenkinsdockerslaves;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class DockerSwarmCloud extends Cloud {
    private static final String DOCKER_SWARM_CLOUD_NAME = "Docker Swarm";
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmCloud.class.getName());
    String dockerSwarmApiUrl;
    private String jenkinsUrl;
    private String swarmNetwork;
    private String cacheDriverName;
    private List<LabelConfiguration> agentTemplates = new ArrayList<>();

    @DataBoundConstructor
    public DockerSwarmCloud(String dockerSwarmApiUrl, String jenkinsUrl, String swarmNetwork, String cacheDriverName, List<LabelConfiguration> agentTemplates) {
        super(DOCKER_SWARM_CLOUD_NAME);
        this.dockerSwarmApiUrl = dockerSwarmApiUrl;
        this.jenkinsUrl = jenkinsUrl;
        this.swarmNetwork = swarmNetwork;
        this.cacheDriverName = cacheDriverName;
        this.agentTemplates = agentTemplates;
    }

    //for yaml serialization
    public DockerSwarmCloud(){
        super(DOCKER_SWARM_CLOUD_NAME);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(final Label label, final int excessWorkload) {
        return new ArrayList<>();
    }

    @Override
    public boolean canProvision(final Label label) {
        return getLabels().contains(label.getName());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {
        @Override
        public String getDisplayName() {
            return "Docker Swarm";
        }
    }

    public String getDockerSwarmApiUrl() {
        return dockerSwarmApiUrl;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public String getSwarmNetwork() {
        return swarmNetwork;
    }

    public String getCacheDriverName() {
        return cacheDriverName;
    }

    public List<LabelConfiguration> getAgentTemplates() {
        return agentTemplates;
    }

    public LabelConfiguration getLabelConfiguration(final String label) {
        for (final LabelConfiguration labelConfiguration : this.agentTemplates) {
            if (label.equals(labelConfiguration.getLabel())) {
                return labelConfiguration;
            }
        }
        return null;
    }
    public List<String> getLabels() {
        final Iterable<String> labels = Iterables.transform(getAgentTemplates(), labelConfiguration -> labelConfiguration.getLabel());
        return Lists.newArrayList(labels);
    }

    public static DockerSwarmCloud get() {
        return (DockerSwarmCloud ) Jenkins.getInstance().getCloud(DOCKER_SWARM_CLOUD_NAME);
    }

    public void save(){
        getDescriptor().save();
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void initFromYaml() throws IOException {
        File configsDir = new File(Jenkins.getInstance().getRootDir(), "pluginConfigs");
        File swarmConfigYaml = new File(configsDir, "swarm.yml");
        if (swarmConfigYaml.exists()) {
            LOGGER.info("Configuring swarm plugin from " + swarmConfigYaml.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try (InputStream in = new BufferedInputStream(new FileInputStream(swarmConfigYaml))) {
                DockerSwarmCloud configuration = mapper.readValue(in, DockerSwarmCloud.class);
                DockerSwarmCloud existingCloud = DockerSwarmCloud.get();
                if(existingCloud != null){
                    Jenkins.getInstance().clouds.remove(existingCloud);
                }
                Jenkins.getInstance().clouds.add(configuration);
            }
        }
        scheduleReaperActor();
    }

    private static void scheduleReaperActor() {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        final ActorRef deadAgentReaper = swarmPlugin.getActorSystem().actorOf(DeadAgentServiceReaperActor.props(), "dead-agentService-reaper");
        deadAgentReaper.tell("start",ActorRef.noSender());
    }
}

