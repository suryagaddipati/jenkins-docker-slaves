package suryagaddipati.jenkinsdockerslaves.dashboard;

import akka.actor.ActorSystem;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import scala.util.Either;
import suryagaddipati.jenkinsdockerslaves.DockerSlaveInfo;
import suryagaddipati.jenkinsdockerslaves.DockerSwarmPlugin;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.nodes.ListNodesRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiException;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.SerializationException;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class Dashboard {
    private final List<SwarmNode> nodes;

    public Dashboard(){
       this.nodes = calculateNodes();
    }

    public Iterable getQueue() {
        final List<SwarmQueueItem> queue = new ArrayList<>();
        final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        for (int i = items.length - 1; i >= 0; i--) { //reverse order
            final Queue.Item item = items[i];
            final DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class);
            if (slaveInfo != null && item instanceof Queue.BuildableItem) {
                queue.add(new SwarmQueueItem((Queue.BuildableItem) item));
            }
        }
        return queue;
    }

    public List<SwarmNode> getNodes(){
       return this.nodes;
    }

    public String getUsage() {

        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("Job", "cpu"));

        final Map<String, Long> usagePerJob = new HashMap<>();
        final List<SwarmNode> nodes = calculateNodes();
        final long totalCpus = nodes.stream().map(node -> node.getTotalCPUs()).reduce(0l, Long::sum );
        final long totalReservedCpus = nodes.stream().map(node -> node.getReservedCPUs()).reduce(0l, Long::sum);

        for (final SwarmNode node : calculateNodes()) {
            final Map<Task, Run> map = node.getTaskRunMap();
            for (final Task task : map.keySet()) {
                final String jobName = getJobName(map.get(task));
                final Long reservedCpus = task.Spec.Resources.Reservations.NanoCPUs/ 1000000000;
                if (usagePerJob.containsKey(jobName)) {
                    usagePerJob.put(jobName, usagePerJob.get(jobName) + reservedCpus);
                } else {
                    usagePerJob.put(jobName, reservedCpus);
                }
            }
        }
        usagePerJob.put("Available ", totalCpus - totalReservedCpus);

        for (final String jobName : usagePerJob.keySet()) {
            final Long jobUsage = usagePerJob.get(jobName);
            usage.add(Arrays.asList(jobName + " - " + jobUsage, jobUsage));
        }


        final JSONArray mJSONArray = new JSONArray();
        mJSONArray.addAll(usage);
        return mJSONArray.toString();
    }

    private List<SwarmNode> calculateNodes() {
        final DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        final ActorSystem as = swarmPlugin.getActorSystem();

        CompletionStage<Either<SerializationException, ?>> nodesStage = new DockerApiRequest(as, new ListNodesRequest()).execute();
        CompletionStage<Either<SerializationException,?>> swarmNodesFuture = nodesStage.thenComposeAsync(nodesResponse -> {
            if(nodesResponse.isRight()){
                return processNodes(as,nodesResponse.right().get());
            }
            return CompletableFuture.completedFuture(nodesResponse);

//            Either<SerializationException, ? extends CompletableFuture<?>> x = nodesResponse.map(nodes -> {
//                return processNodes(as, nodes);
//            });
//                final CompletableFuture<?> tasksFuture = processNodes(as, nodes);
//                if (tasksFuture != null) return tasksFuture;
//                return CompletableFuture.completedFuture(nodes);
        });

        return (List<SwarmNode>) getFuture(null);
    }


    private CompletionStage<Either<SerializationException,?>>  processNodes(ActorSystem as, Object nodes) {
//        if (nodes instanceof List) {
//            CompletionStage<Either<SerializationException, ?>> tasksResponse = new DockerApiRequest(as, new ListTasksRequest()).execute();
//
//            return tasksFuture.thenApply(tasks -> {
//                if(tasks instanceof  List){
//                    final List<Node> nodeList = (List<Node>) nodes;
//                    return nodeList.stream().map(node -> {
//                        Stream<Task> tasksForNode = ((List<Task>) tasks).stream()
//                                .filter(task -> node.ID.equals(task.NodeID) && task.Spec.getComputerName() != null);
//                        return    new SwarmNode(node, tasksForNode.collect(Collectors.toList()));
//                    }).collect(Collectors.toList());
//                }
//                return  CompletableFuture.completedFuture(tasks);
//            });
//        }
        return null;
    }

    private Object getFuture(final CompletionStage<Object> future) {
        try {
            final Object result = future.toCompletableFuture().get(); // .get(5, TimeUnit.SECONDS);
            if(result instanceof SerializationException){
                throw new RuntimeException (((SerializationException)result).getCause());
            }
            if(result instanceof ApiException){
                throw new RuntimeException (((ApiException)result).getCause());
            }
            return result;
        } catch (InterruptedException|ExecutionException  e) {
            throw  new RuntimeException(e);
        }
    }

    private String getJobName(final Run build) {
        final Job parent = build.getParent();
        return (parent.getParent() instanceof Job ? (Job) parent.getParent() : parent).getFullDisplayName();
    }
}
