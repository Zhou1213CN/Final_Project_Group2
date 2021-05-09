# Large Scale Data Processing: Final Project
### Group 2 Members : Zehua Zhang, Zheng Zhou

## Graph matching Results

| File name                   | Number of edges | Size of Matching | Running Time                                                 |      |
| --------------------------- | --------------- | ---------------- | ------------------------------------------------------------ | ---- |
| com-orkut.ungraph.csv       | 117185083       |                  |                                                              |      |
| twitter_original_edges.csv  | 63555749        |                  |                                                              |      |
| soc-LiveJournal1.csv        | 42851237        |                  |                                                              |      |
| soc-pokec-relationships.csv | 22301964        |                  |                                                              |      |
| musae_ENGB_edges.csv        | 35324           | 2630             | 7s locally (only Luby’s) <br />20s locally (Luby’s +Augment) |      |
| log_normal_100.csv          | 2671            | 42               | 3s locally (only Luby’s)                                     |      |

## Discussion of Technicality 

### 1. Revised Luby's Algorithm

To get a maximal matching, we modify the bidding variant of Luby's algorithm on the line graph.

1. Generate values for each edges and pass to vertex

We first generate random float numbers for each edges and use one round of aggregate message to send edge values to each vertex. The vertex will take the max between two messages it receives. 

2. Transform edge attribute and deactivate edges

We deactivate and pick an edge into our matching if it is largest at both endpoints.

We intially tried maptriplet and managed to transform each edge attribute, but it cannot change the entire edge's attribute tuple efficiently. So we use triplet.map instead to generate new edges, i.e. transform edges.

3. Deactivate the vertices and inform their neighbors

Then we use another round of aggregate message to deactivate the vertices with respect to the edge values. If two vertices receive the same and the largest edge value, we deactivate the two vertices and their neighbors. 

### 2. Augmenting Path of Length 3 

We only use augmenting path part for all the files except the log_normal csv because it contains smaller M and the result increased by augmenting is not obvious.

### Proof 

By the theorem, while there exists an augmenting path P, we can increase the cardinality of maximal matching by 1 by augmenting P. So we have showed that our augmenting part indeed increases the size of matching.

### Implementation

Based on Professor Su's discussion, by augmenting the paths of length 3 and 5, we can get about 0.8 approximation of maximum matching. Since it is hard to search all augmenting paths in limited time, we only augment Ps with length of 3 here. () In the future, we will try to implement a blossom algorithm that includes all augmenting paths.

We initially planned to randomly label all the vertices with 1,2,3,4 and find augmenting paths which follow 1-2-3-4 with 1-2, 3-4 unmatched and 2-3 matched. However, it is hard to simultaneously identify all these paths using Graph API. After exploration, we fail to think about a good solution. Thanks to the idea of Jien's group, we learn to :

1. Let every vertex pick a matched edge with its vertices. 
2. Similarly, the chosen matched edge with its vertices chooses two free vertices. 
3. Then matched vertices inform M that whether there is a match between picked-chosen.
4. Finally we augment the 3-length paths into M.

So we are able to implement it with the process below:

1. We use one round of aggregateMessage to let the edges send values. Each vertex randomly chooses one edge value.

2. Use another round of aggregateMessage to handle the process for the unmatched to pick a matched edge. For each edge whose source vertex is in M and destination vertex is not in M, we generate a random float for its two vertices. For other edges, we generate -1 for their vertices. Each vertex picks the larger edge value. 

3. Similarly with step 2, we can handle the process for the matched to choose the free vertices.

4. From source to destination vertex of matched edges and the reverse, use a round of aggregateMessage again to exchange info and check if both are picked. All the edges are updated. 

5. Use map triplets to update attributes for edges and then update vertices based on the new infos of edges

6. Repeat 2-5 until the new matched count does not increase by a value compared with the previous iteration

   Usage in step 6: Musae_ENGB_edges.csv with value = 0.3%; Rest of files with value =2%

## **Merits of our algorithm**


### Input format
Each input file consists of multiple lines, where each line contains 2 numbers that denote an undirected edge. For example, the input below is a graph with 3 edges.  
1,2  
3,2  
3,4  

### Output format
Your output should be a CSV file listing all of the matched edges, 1 on each line. For example, the ouput below is a 2-edge matching of the above input graph. Note that `3,4` and `4,3` are the same since the graph is undirected.  
1,2  
4,3  

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


