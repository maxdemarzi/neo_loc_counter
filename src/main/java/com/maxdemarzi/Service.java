package com.maxdemarzi;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Path("/service")
public class Service {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GET
    @Path("/helloworld")
    public Response helloWorld() throws IOException {
        Map<String, String> results = new HashMap<String,String>(){{
            put("hello","world");
        }};
        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/warmup")
    public Response warmUp(@Context GraphDatabaseService db) throws IOException {
        Map<String, String> results = new HashMap<String,String>(){{
            put("warmed","up");
        }};

        try (Transaction tx = db.beginTx()) {
            for (Node n : db.getAllNodes()) {
                n.getPropertyKeys();
                for (Relationship relationship : n.getRelationships()) {
                    relationship.getPropertyKeys();
                    relationship.getStartNode();
                }
            }

            for (Relationship relationship : db.getAllRelationships()) {
                relationship.getPropertyKeys();
                relationship.getNodes();
            }
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/loc/{program_id}")
    public Response loc(@PathParam("program_id") String program_id, @Context GraphDatabaseService db) throws IOException {
        Long loc = 0L;
        try (Transaction tx = db.beginTx()) {
            final Node program = db.findNode(Labels.Program, "id", program_id);

            if (program != null) {

                TraversalDescription td = db.traversalDescription()
                        .depthFirst()
                        .expand(PathExpanders.forTypeAndDirection(RelationshipTypes.CALLS, Direction.OUTGOING))
                        .uniqueness(Uniqueness.NODE_GLOBAL);

                Set<Node> nodes = new HashSet<>();
                for (org.neo4j.graphdb.Path position : td.traverse(program)) {
                    nodes.add(position.endNode());
                }

                for (Node node: nodes) {
                    loc += (Long)node.getProperty("loc");
                }
            }
            program.setProperty("tree_loc", loc);
            tx.success();
        }

        Map<String, Long> results = new HashMap<>();
        results.put("loc", loc);

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/locs")
    public Response locs(@Context GraphDatabaseService db) throws IOException {

        try (Transaction tx = db.beginTx()) {

            TraversalDescription td = db.traversalDescription()
                    .depthFirst()
                    .expand(PathExpanders.forTypeAndDirection(RelationshipTypes.CALLS, Direction.OUTGOING))
                    .uniqueness(Uniqueness.NODE_GLOBAL);

            ResourceIterator<Node> programs = db.findNodes(Labels.Program);

            while(programs.hasNext()) {
                Node program = programs.next();
                Long loc = 0L;

                Set<Node> nodes = new HashSet<>();
                for (org.neo4j.graphdb.Path position : td.traverse(program)) {
                    nodes.add(position.endNode());
                }

                for (Node node: nodes) {
                    loc += (Long)node.getProperty("loc");
                }
                program.setProperty("tree_loc", loc);
            }

            tx.success();
        }

        Map<String, String> results = new HashMap<>();
        results.put("locs", "calculated");

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/overlap/{program_id1}/{program_id2}")
    public Response loc(@PathParam("program_id1") String program_id1,
                        @PathParam("program_id2") String program_id2,
                        @Context GraphDatabaseService db) throws IOException {
        Long loc = 0L;
        try (Transaction tx = db.beginTx()) {
            final Node program1 = db.findNode(Labels.Program, "id", program_id1);
            final Node program2 = db.findNode(Labels.Program, "id", program_id2);

            if (program1 != null && program2 != null) {

                TraversalDescription td = db.traversalDescription()
                        .depthFirst()
                        .expand(PathExpanders.forTypeAndDirection(RelationshipTypes.CALLS, Direction.OUTGOING))
                        .uniqueness(Uniqueness.NODE_GLOBAL);

                Set<Node> nodes1 = new HashSet<>();
                for (org.neo4j.graphdb.Path position : td.traverse(program1)) {
                    nodes1.add(position.endNode());
                }

                Set<Node> nodes2 = new HashSet<>();
                for (org.neo4j.graphdb.Path position : td.traverse(program2)) {
                    nodes2.add(position.endNode());
                }
                nodes1.retainAll(nodes2);

                for (Node node: nodes1) {
                    loc += (Long)node.getProperty("loc");
                }
            }
            tx.success();
        }

        Map<String, Long> results = new HashMap<>();
        results.put("overlap_loc", loc);

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/locs2")
    public Response locs2(@Context GraphDatabaseService db) throws IOException {

        try (Transaction tx = db.beginTx()) {
            // Figure out how many nodes we have
            int maxNodeId = 1;
            Result result = db.execute( "CYPHER runtime=compiled MATCH (n) RETURN max(id(n)) AS maxId" );
            Map response = result.next();
            if (response.get("maxId") != null) {
                maxNodeId = ((Number) response.get("maxId")).intValue() + 1;
            }

            // Get all the LOCs once for all nodes
            long locs[] = new long[maxNodeId];
            ResourceIterator<Node> nodes =  db.getAllNodes().iterator();
            while (nodes.hasNext()) {
                Node node= nodes.next();
                locs[(int)node.getId()] =  (long)node.getProperty("loc");
            }

            // Find all my 1st level calls
            nodes =  db.getAllNodes().iterator();
            HashMap<Long, Set<Long>> chain = new HashMap<>();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                Set<Long> calls = new HashSet<>();
                for (Relationship rel : node.getRelationships(Direction.OUTGOING, RelationshipTypes.CALLS)) {
                    calls.add(rel.getEndNode().getId());
                }
                chain.put(node.getId(), calls);
            }

            // For each node get the chain
            nodes =  db.getAllNodes().iterator();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                Long loc = locs[(int)node.getId()];
                Queue<Long> todo = new LinkedBlockingQueue<>();
                Set<Long> calls = chain.get(node.getId());
                todo.addAll(calls);
                while (!todo.isEmpty()) {
                    for (Long link : chain.get(todo.poll()) ){
                        if (calls.add(link)) {
                            todo.add(link);
                        }
                    }
                }

                for (Long call : calls) {
                    loc += locs[call.intValue()];
                }
                node.setProperty("tree_loc2", loc);
            }
            tx.success();
        }

        Map<String, String> results = new HashMap<>();
        results.put("locs2", "calculated");

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }
}
