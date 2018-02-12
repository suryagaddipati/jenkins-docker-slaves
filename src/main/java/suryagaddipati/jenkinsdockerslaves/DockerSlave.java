package suryagaddipati.jenkinsdockerslaves;

import akka.actor.ActorRef;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import jenkins.model.Jenkins;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.DeleteServiceRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerSlave extends AbstractCloudSlave implements EphemeralNode {

    private static final Logger LOGGER = Logger.getLogger(DockerSlave.class.getName());

    public DockerSlave(final Queue.BuildableItem bi, final String labelString) throws Descriptor.FormException, IOException {
        super(labelString, "Container slave for building " + bi.task.getFullDisplayName(),
                "/home/jenkins", 1, Mode.EXCLUSIVE, labelString,
                new DockerSwarmComputerLauncher(bi),
                new DockerSlaveRetentionStrategy(1),
                Collections.emptyList());
    }

    public DockerComputer createComputer() {
        return new DockerComputer(this);
    }

    @Override
    protected void _terminate(final TaskListener listener) throws IOException, InterruptedException {
    }

    @Override
    public Node asNode() {
        return this;
    }


    @Override
    public CauseOfBlockage canTake(final Queue.BuildableItem item) {
        final Label l = item.getAssignedLabel();
        if (l != null && this.name.equals(l.getName())) {
            return null;
        }
        return super.canTake(item);
    }

    @Override
    public Set<LabelAtom> getAssignedLabels() {
        final TreeSet<LabelAtom> labels = new TreeSet<>();
        labels.add(new LabelAtom(getLabelString()));
        return labels;
    }

    public void terminate() {
        try {
            DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
            ActorRef agentLauncherRef = swarmPlugin.getActorSystem().actorFor("/user/" + getComputer().getName());
            agentLauncherRef.tell(new DeleteServiceRequest(getComputer().getName()),ActorRef.noSender());
        } finally {
            try {
                Jenkins.getInstance().removeNode(this);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,"Failed to remove computer",e);
            }
        }
    }
}

