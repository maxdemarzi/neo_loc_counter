package com.maxdemarzi;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class ServiceTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withExtension("/v1", Service.class);

    public static final String MODEL_STATEMENT =
            new StringBuilder()
                    .append("CREATE (p1:Program {id: 'p1', loc:101})")
                    .append("CREATE (p2:Program {id: 'p2', loc:102})")
                    .append("CREATE (p3:Program {id: 'p3', loc:103})")
                    .append("CREATE (p4:Program {id: 'p4', loc:104})")
                    .append("CREATE (p5:Program {id: 'p5', loc:105})")
                    .append("CREATE (p6:Program {id: 'p6', loc:106})")
                    .append("CREATE (p7:Program {id: 'p7', loc:107})")
                    .append("CREATE (p8:Program {id: 'p8', loc:108})")
                    .append("CREATE (p9:Program {id: 'p9', loc:109})")
                    .append("CREATE (p10:Program {id: 'p10', loc:110})")

                    .append("CREATE (p11:Program {id: 'p11', loc:111})")
                    .append("CREATE (p12:Program {id: 'p12', loc:112})")
                    .append("CREATE (p13:Program {id: 'p13', loc:113})")

                    .append("CREATE (p1)-[:CALLS]->(p2)")
                    .append("CREATE (p1)-[:CALLS]->(p3)")
                    .append("CREATE (p2)-[:CALLS]->(p4)")
                    .append("CREATE (p4)-[:CALLS]->(p5)")
                    .append("CREATE (p5)-[:CALLS]->(p6)")
                    .append("CREATE (p3)-[:CALLS]->(p7)")
                    .append("CREATE (p3)-[:CALLS]->(p8)")
                    .append("CREATE (p7)-[:CALLS]->(p9)")
                    .append("CREATE (p8)-[:CALLS]->(p5)")
                    .append("CREATE (p9)-[:CALLS]->(p10)")

                    .append("CREATE (p11)-[:CALLS]->(p12)")
                    .append("CREATE (p11)-[:CALLS]->(p13)")
                    .append("CREATE (p12)-[:CALLS]->(p4)")
                    .append("CREATE (p13)-[:CALLS]->(p4)")

                    .toString();

    @Test
    public void shouldRespondToLOCRequest() {
        HTTP.Response response = HTTP.GET(neo4j.httpURI().resolve("/v1/service/loc/p1").toString());
        HashMap actual = response.content();
        assertTrue(actual.equals(expected));
    }

    private static final HashMap expected = new HashMap<String, Object>() {{
        put("loc", 1055);
    }};

    @Test
    public void shouldRespondToLOCRequest2() {
        HTTP.Response response = HTTP.GET(neo4j.httpURI().resolve("/v1/service/loc/p11").toString());
        HashMap actual = response.content();
        assertTrue(actual.equals(expected2));
    }

    private static final HashMap expected2 = new HashMap<String, Object>() {{
        put("loc", 651);
    }};


    @Test
    public void shouldRespondToLOCSRequest() {
        HTTP.Response response = HTTP.GET(neo4j.httpURI().resolve("/v1/service/locs").toString());
        HashMap actual = response.content();
        assertTrue(actual.equals(expected3));
    }

    private static final HashMap expected3 = new HashMap<String, Object>() {{
        put("locs", "calculated");
    }};


    @Test
    public void shouldRespondToOverlapRequest() {
        HTTP.Response response = HTTP.GET(neo4j.httpURI().resolve("/v1/service/overlap/p1/p11").toString());
        HashMap actual = response.content();
        assertTrue(actual.equals(expected4));
    }

    private static final HashMap expected4 = new HashMap<String, Object>() {{
        put("overlap_loc", 315);
    }};
}
