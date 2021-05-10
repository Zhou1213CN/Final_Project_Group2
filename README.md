# Large Scale Data Processing: Final Project
### Group 2 Members : Zehua Zhang, Zheng Zhou

## Graph matching Results

| File name                   | Number of edges | Size of Matching | Running Time                                                 |      |
| --------------------------- | --------------- | ---------------- | ------------------------------------------------------------ | ---- |
| com-orkut.ungraph.csv       | 117185083       |                  |                                                              |      |
| twitter_original_edges.csv  | 63555749        |                  |                                                              |      |
| soc-LiveJournal1.csv        | 42851237        |                  |                                                              |      |
| soc-pokec-relationships.csv | 22301964        |                  | 4032s on GCP                                                 |      |
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

    

  ​		We initially planned to randomly label all the vertices with 1,2,3,4 and find augmenting paths which 		follow 1-2-3-4 with 1-2, 3-4 unmatched and 2-3 matched. However, it is hard to simultaneously 		identify all these paths using Graph API. After exploration, we fail to think about a good solution. 		Thanks to the idea of Jien's group, we learn to : 

  ​		1. Let every vertex pick a matched edge with its vertices. 

  ​	 2. Similarly, the chosen matched edge with its vertices chooses two free vertices. 

  ​		3. Then matched vertices inform M that whether there is a match between picked-chosen.

  ​		4. Finally we augment the 3-length paths into M.

  

  * Implementation : 

    1. We use one round of aggregateMessage to let the edges send values. Each vertex randomly chooses one edge value.

    2. Use another round of aggregateMessage to handle the process for the unmatched to pick a matched edge. For each edge whose source vertex is in M and destination vertex is not in M, we generate a random float for its two vertices. For other edges, we generate -1 for their vertices. Each vertex picks the larger edge value. 

    3. Similarly with step 2, we can handle the process for the matched to choose the free vertices.

    4. From source to destination vertex of matched edges and the reverse, use a round of aggregateMessage again to exchange info and check if both are picked. All the edges are updated. 

    5. Use map triplets to update attributes for edges and then update vertices based on the new infos of edges

    6. Repeat 2-5 until the new matched count does not increase by a value compared with the previous iteration
       (Usage : musae_ENGB_edges.csv with value = 0.3% in step 6; Rest of files value = 2%)

       

## **Merits of our algorithm**

* Since we are modifying the Luby’s variant and employing the same logic, we still expect the algorithm to finish in log(n) rounds. In general, we aggregate the message to the neighbor vertices as we did in finding the MIS, but only take an extra step of reassigning and updating activation status for all the triplets. So it will not change the rounds that it takes to run the algorithms. However, since the way we deactivate the vertex is slightly different from the previously implemented Luby’s, we expect that the rate of deactivating edges would be smaller than calculating MIS (expected to remove ½ edges for each iteration).

* Previously, if we use X = 1 to denote that one vertex will be deactivated, \[P(x=1) \geq \frac{1}{(d(u)+d(v))}\] , and because of that any vertex can be removed at most once and any edge can be removed twice, we expect numbers of edge removed as \[\frac{1}{2} * \sum (d(u)*P(X{v} =1)+d(v)*P(X{u} =1))\]. Finally we expect half of the edge removed for each iteration to achieve log(n) rounds.

* Now, we would expect the possibility of having 2 vertices with the same assigned random float number to be smaller (also biggest among all the neighbors), which means that the rate of removing edges will be slower as well. However, since once we observed such 2 vertices, we could remove all the neighbor edges and the corresponding vertices (endpoints), which would boost the removal rate slightly but not enough to make up for the slowdown. 

* In all, we expect the algorithm still runs with O(log(n)) rounds, but there could be some variations here.

* Reference : http://www.cs.cmu.edu/~haeupler/15859F14/docs/lecture6.pdf



### No template is provided
For the final project, you will need to write everything from scratch. Feel free to consult previous projects for ideas on structuring your code. That being said, you are provided a verifier that can confirm whether or not your output is a matching. As usual, you'll need to compile it with
```
sbt clean package
```
The verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file containing the matching. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.verifier target/scala-2.12/project_3_2.12-1.0.jar /data/log_normal_100.csv data/log_normal_100_matching.csv

// Unix
spark-submit --master "local[*]" --class "final_project.verifier" target/scala-2.12/project_3_2.12-1.0.jar data/log_normal_100.csv data/log_normal_100_matching.csv
```

## Deliverables
* The output file (matching) for each test case.
  * For naming conventions, if the input file is `XXX.csv`, please name the output file `XXX_matching.csv`.
  * You'll need to compress the output files into a single ZIP or TAR file before pushing to GitHub. If they're still too large, you can upload the files to Google Drive and include the sharing link in your report.
* The code you've applied to produce the matchings.
  * You should add your source code to the same directory as `verifier.scala` and push it to your repository.
* A project report that includes the following:
  * A table containing the size of the matching you obtained for each test case. The sizes must correspond to the matchings in your output files.
  * An estimate of the amount of computation used for each test case. For example, "the program runs for 15 minutes on a 2x4 N1 core CPU in GCP." If you happen to be executing mulitple algorithms on a test case, report the total running time.
  * Description(s) of your approach(es) for obtaining the matchings. It is possible to use different approaches for different cases. Please describe each of them as well as your general strategy if you were to receive a new test case.
  * Discussion about the advantages of your algorithm(s). For example, does it guarantee a constraint on the number of shuffling rounds (say `O(log log n)` rounds)? Does it give you an approximation guarantee on the quality of the matching? If your algorithm has such a guarantee, please provide proofs or scholarly references as to why they hold in your report.

## Grading policy
* Quality of matchings (40%)
  * For each test case, you'll receive at least 70% of full credit if your matching size is at least half of the best answer in the class.
  * **You will receive a 0 for any case where the verifier does not confirm that your output is a matching.** Please do not upload any output files that do not pass the verifier.
* Project report (35%)
  * Your report grade will be evaluated using the following criteria:
    * Discussion of the merits of your algorithms
    * Depth of technicality
    * Novelty
    * Completeness
    * Readability
* Presentation (15%)
* Formatting (10%)
  * If the format of your submission does not adhere to the instructions (e.g. output file naming conventions), points will be deducted in this category.

## Submission via GitHub
Delete your project's current **README.md** file (the one you're reading right now) and include your report as a new **README.md** file in the project root directory. Have no fear—the README with the project description is always available for reading in the template repository you created your repository from. For more information on READMEs, feel free to visit [this page](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/about-readmes) in the GitHub Docs. You'll be writing in [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown). Be sure that your repository is up to date and you have pushed all of your project's code. When you're ready to submit, simply provide the link to your repository in the Canvas assignment's submission.

## You must do the following to receive full credit:
1. Create your report in the ``README.md`` and push it to your repo.
2. In the report, you must include your (and any partner's) full name in addition to any collaborators.
3. Submit a link to your repo in the Canvas assignment.


