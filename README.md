# Large Scale Data Processing: Final Project
### Group 2 Members : Zehua Zhang, Zheng Zhou

## Graph matching Results

| File name                   | Number of edges | Size of Matching | Running Time                                                 |      |
| --------------------------- | --------------- | ---------------- | ------------------------------------------------------------ | ---- |
| com-orkut.ungraph.csv       | 117185083       | 1048225          |More than 12 hours on GCP customized  2* (2cores 10GB memory) |      |
| twitter_original_edges.csv  | 63555749        |90209             |More than 12 hours on GCP customized  2* (2cores 10GB memory) |      |
| soc-LiveJournal1.csv        | 42851237        |1048121           |More than 12 hours on GCP customized  2* (2cores 10GB memory) |      |
| soc-pokec-relationships.csv | 22301964        |653796            | 4032s on GCP                                                 |      |
| musae_ENGB_edges.csv        | 35324           | 2630             | 7s locally (only Luby’s) <br />20s locally (Luby’s +Augment) |      |
| log_normal_100.csv          | 2671            | 42               | 3s locally (only Luby’s)                                     |      |



## Discussion of Technicality 

### Revised Luby's Algorithm + Augmenting Path of Length 3

* To get a maximal matching, we modify the bidding variant of Luby's algorithm on the line graph.

  * Generate values for each edges and pass to vertex

    We first generate random float numbers for each edge and use one round of aggregate message to send edge values to each vertex. The vertex will take the max between two messages it receives.

  * Transform edge attribute and deactivate edges

    We deactivate and pick an edge into our matching if it is largest at both endpoints.
    We initially tried maptriplet and managed to transform each edge attribute, but it cannot change the entire edge's attribute tuple efficiently. So we use triplet.map instead to generate new edges, i.e. transform edges.

  * Deactivate the vertices and inform their neighbors

    Then we use another round of aggregate message to deactivate the vertices with respect to the edge values. If two vertices receive the same and the largest edge value, we deactivate the two vertices and their neighbors.

* Augmenting Path of Length 3

  We use augmenting path part for all the files except the log_normal csv because it contains smaller M and the result increased by augmenting is not obvious.

  * Proof : 

    By the theorem, while there exists an augmenting path P, we can increase the cardinality of maximal matching by 1 by augmenting P. So we have showed that our augmenting part indeed increases the size of matching. 

    

  * Idea behind Implementation : 

    Based on Professor Su's discussion, by augmenting the paths of length 3 and 5, we can get about 0.8 approximation of maximum matching. Since it is hard to search all augmenting paths in limited time, we only augment Ps with length of 3 here. In the future, we will try to implement a blossom algorithm that includes all augmenting paths.

    We initially planned to randomly label all the vertices with 1,2,3,4 and find augmenting paths which 		follow 1-2-3-4 with 1-2, 3-4 unmatched and 2-3 matched. However, it is hard to simultaneously 		identify all these paths using Graph API. After exploration, we fail to think about a good solution. 		Thanks to the idea of Jien's group, we learn to : 
    * Let every vertex pick a matched edge with its vertices. 
    * Similarly, the chosen matched edge with its vertices chooses two free vertices. 
    * Then matched vertices inform M that whether there is a match between picked-chosen.
    * Finally we augment the 3-length paths into M.


  

  * Implementation : 

    1. We use one round of aggregateMessage to let the edges send values. Each vertex randomly chooses one edge value.

    2. Use another round of aggregateMessage to handle the process for the unmatched to pick a matched edge. For each edge whose source vertex is in M and destination vertex is not in M, we generate a random float for its two vertices. For other edges, we generate -1 for their vertices. Each vertex picks the larger edge value. 

    3. Similarly with last step, we can handle the process for the matched to choose the free vertices.

    4. From source to destination vertex of matched edges and the reverse, use a round of aggregateMessage again to exchange info and check if both are picked. All the edges are updated. 

    5. Use map triplets to update attributes for edges and then update vertices based on the new infos of edges

    6. Repeat last four steps until the new matched count does not increase by a value compared with the previous iteration
       (Usage : musae_ENGB_edges.csv with value = 0.3% in step 6; Rest of files value = 2%)

       

## **Merits of our algorithm**

* Since we are modifying the Luby’s variant and employing the same logic, we still expect the algorithm to finish in log(n) rounds. In general, we aggregate the message to the neighbor vertices as we did in finding the MIS, but only take an extra step of reassigning and updating activation status for all the triplets. So it will not change the rounds that it takes to run the algorithms. However, since the way we deactivate the vertex is slightly different from the previously implemented Luby’s, we expect that the rate of deactivating edges would be smaller than calculating MIS (expected to remove ½ edges for each iteration).

* Previously, if we use X = 1 to denote that one vertex will be deactivated, <img src="https://bit.ly/33vsKrK" align="center" border="0" alt=" [P(x=1) \geq \frac{1}{(d(u)+d(v))}]" width="221" height="46" /> , and because of that any vertex can be removed at most once and any edge can be removed twice, we expect numbers of edge removed as <img src="http://www.sciweavers.org/tex2img.php?eq=%5C%5B%5Cfrac%7B1%7D%7B2%7D%20%2A%20%5Csum%20%28d%28u%29%2AP%28X%7Bv%7D%20%3D1%29%2Bd%28v%29%2AP%28X%7Bu%7D%20%3D1%29%29%5C%5D&bc=White&fc=Black&im=jpg&fs=12&ff=arev&edit=0" align="center" border="0" alt="\[\frac{1}{2} * \sum (d(u)*P(X{v} =1)+d(v)*P(X{u} =1))\]" width="369" height="43" />. Finally we expect half of the edge removed for each iteration to achieve log(n) rounds.

* Now, we would expect the possibility of having 2 vertices with the same assigned random float number to be smaller (also biggest among all the neighbors), which means that the rate of removing edges will be slower as well. However, since once we observed such 2 vertices, we could remove all the neighbor edges and the corresponding vertices (endpoints), which would boost the removal rate slightly but not enough to make up for the slowdown. 

* In all, we expect the algorithm still runs with O(log(n)) rounds, but there could be some variations here.

* Reference : http://www.cs.cmu.edu/~haeupler/15859F14/docs/lecture6.pdf

* Another advantage is Easier implementation: Compared with Israti algorithm, our revised Luby's is easier to implement,  following the one we implemented in the last project.Also our revised Luby's algorithm does not create a new line graph which requires too much memory.







