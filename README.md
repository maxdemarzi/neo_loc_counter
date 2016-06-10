# Neo LOC Counter

Count lines of code in a call tree without duplicates


Two API calls:

One: Count given a program, count the total LOC down the tree

One (b): Do this for all programs naively

One (c): Do this for all programs optimally-ish

Two: Overlap (given 2 programs, count the overlap LOCs)

Prerequisites
-------------

Download Neo4j 3.0.2 community or enterprise edition:

        wget http://neo4j.com/download-thanks/?edition=community&release=3.0.2&flavour=unix
        tar -xvzf neo4j-community-3.0.2-unix.tar.gz


Instructions
-------------

1. Build it:

        mvn clean package

2. Copy target/neo_loc_counter-1.0-SNAPSHOT.jar to the plugins/ directory of your Neo4j 3.0.2 server.

        mv target/neo_loc_counter-1.0-SNAPSHOT.jar neo4j/plugins/.

3. Configure Neo4j by adding a line to the neo4j/conf/neo4j.conf file:

        dbms.unmanaged_extension_classes=com.maxdemarzi=/v1

4. Start Neo4j server.

        neo4j-community-3.0.2/bin/neo4j start

5. Create sample dataset:

        CREATE (p1:Program {id: 'p1', loc:101})
        CREATE (p2:Program {id: 'p2', loc:102})
        CREATE (p3:Program {id: 'p3', loc:103})
        CREATE (p4:Program {id: 'p4', loc:104})
        CREATE (p5:Program {id: 'p5', loc:105})
        CREATE (p6:Program {id: 'p6', loc:106})
        CREATE (p7:Program {id: 'p7', loc:107})
        CREATE (p8:Program {id: 'p8', loc:108})
        CREATE (p9:Program {id: 'p9', loc:109})
        CREATE (p10:Program {id: 'p10', loc:110})
        CREATE (p11:Program {id: 'p11', loc:111})
        CREATE (p12:Program {id: 'p12', loc:112})
        CREATE (p13:Program {id: 'p13', loc:113})
        CREATE (p1)-[:CALLS]->(p2)
        CREATE (p1)-[:CALLS]->(p3)
        CREATE (p2)-[:CALLS]->(p4)
        CREATE (p4)-[:CALLS]->(p5)
        CREATE (p5)-[:CALLS]->(p6)
        CREATE (p3)-[:CALLS]->(p7)
        CREATE (p3)-[:CALLS]->(p8)
        CREATE (p7)-[:CALLS]->(p9)
        CREATE (p8)-[:CALLS]->(p5)
        CREATE (p9)-[:CALLS]->(p10)
        CREATE (p11)-[:CALLS]->(p12)
        CREATE (p11)-[:CALLS]->(p13)
        CREATE (p12)-[:CALLS]->(p4)
        CREATE (p13)-[:CALLS]->(p4)

6. Call the loc endpoint:

        :GET /v1/service/loc/p1

        You should see "loc: 1055"

7. Try a different program:

        :GET /v1/service/loc/p11

        You should see "loc: 651"

8. Calculate the call tree loc for all Programs (naively):

        :GET /v1/service/locs

        You should see "locs: calculated".

9. Check the results with this Cypher query:

        MATCH (n:Program) RETURN n.id, n.tree_loc LIMIT 25

10. Calculate the overlap of call tree loc for 2 programs

        :GET /v1/service/overlap/p1/p11

        You should see "overlap: 315"