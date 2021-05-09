
package final_project

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._
import org.apache.spark.storage.StorageLevel
import org.apache.log4j.{Level, Logger}

object main {
  val seed = new java.util.Date().hashCode;
  val rand = new scala.util.Random(seed);
  val rootLogger = Logger.getRootLogger()
  rootLogger.setLevel(Level.ERROR)

  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.spark-project").setLevel(Level.WARN)

  def LubyMIS1(g_in: Graph[Float, Int]): RDD[Edge[Int]] = {
    //Without the Augmenting Part, it should be:
    //def LubyMIS1(g_in: Graph[Float, Int]): Graph[Float, Int] = {
    var active_vs = 1.asInstanceOf[Long]
    var counter = 0
    var g = g_in.mapEdges((attr) => (-1, 0.asInstanceOf[Float])).mapVertices((id, i) => (-1.asInstanceOf[Float], 0L))//start with all active vertices

    while (active_vs >= 1){ // remaining vertices
      counter += 1
      g = g.mapEdges((e) => (e.attr._1, rand.nextFloat()))
      //assign  edges random float number

      val v_in = g.aggregateMessages[(Float, Long)]( //aggregate the random float on edge to vertices
        d => { // Map Function
          d.sendToDst(if(d.attr._1==1 || d.attr._1==0)(d.attr._1,d.dstAttr._2) else(d.attr._2,0));
          d.sendToSrc(if(d.attr._1==1 || d.attr._1==0)(d.attr._1,d.srcAttr._2) else(d.attr._2,0));
        },
        (a,b) => (if(a._1>b._1) a else b) //take the max if two msg at one vertex
      )
      var g2 = Graph(v_in, g.edges)

      var newEdges = g2.triplets.map(
        t => {
          if ((t.attr._1) == 1)
          {Edge(t.srcId, t.dstId,(1, t.attr._2));}
          else if ((t.srcAttr._1 == 1) || ( t.dstAttr._1 == 1))
          {Edge(t.srcId, t.dstId,(0, t.attr._2));}
          else {
            if (t.srcAttr._1 == t.dstAttr._1)
            {Edge(t.srcId, t.dstId,(1, t.attr._2))}
            else
            {Edge(t.srcId, t.dstId,(-1, t.attr._2))}
          }
        }
      )
      //println("*****************************************************")
      //g2.edges.collect.foreach(println(_))
      var v_deactivate = g2.aggregateMessages[(Float, Long)](
        d => {
          if (d.attr._1 == 1 || d.attr._1 == 0) { //edge is already deactive
            d.sendToDst(d.attr._1, d.dstAttr._2);
            d.sendToSrc(d.attr._1, d.srcAttr._2);
          } else {
            if (d.dstAttr._1 == d.srcAttr._1) { //selected
              d.sendToDst(1.asInstanceOf[Float], d.srcId);
              d.sendToSrc(1.asInstanceOf[Float], d.dstId);
            } else {
              d.sendToDst(-1.asInstanceOf[Float], 0L);
              d.sendToSrc(-1.asInstanceOf[Float], 0L);
            }
          }

        },
        (a,b) => (if (a._1 > b._1) a else b)
      )

      //g2.mapVertices((VertexId, attr) => if (attr.))
      g = Graph(v_deactivate,newEdges)
      g.cache()
      active_vs = g.vertices.filter({case (id, x) => (x._1 == -1F)} ).count()
      println("***********************************************")
      println("Iteration " + counter +"  " + active_vs)
      println("***********************************************")
    }
    println("_______________________________")
    println("Total iterations = " + counter)

    //Without the Augmenting Part, it should be:
    //val result = g.mapEdges((i) => i.attr._1).mapVertices((id, i) => i._1)

    return augmentation(g.mapEdges((i) => i.attr._1).mapVertices((id, i) => i._1))
  }

  //Basic idea is derived with Jien Li's help
  //We only care about if edge is matched (1) or not (0)
  //Vertices do not inherit attributes from Luby's Algo
  def augmentation(g_in: Graph[Float, Int]): RDD[Edge[Int]] = {
    println("\n\n================================================================")
    println("We only search augmenting paths of length 3 here. ")
    println("================================================================")
    val r = scala.util.Random

    //Int : Edge Value
    //Long: Vertex ID
    //Float: A Random Number
    var v = g_in.aggregateMessages[(Int, Long, Float)](
      e => {
        e.sendToDst((e.attr, 0, 0));
        e.sendToSrc((e.attr, 0, 0))
      },
      //Each vertex randomly pick one message between the two it receives
      (msg1, msg2) => (msg1._1 | msg2._1, 0, 0)
    )

    var g = Graph(v, g_in.edges)

    var iteration = 0

    //var matchedCount = 0.asInstanceOf[Long]
    var matchedCount = 0L
    var new_matchedCount = g.edges.filter({case x => (x.attr == 1)}).count()
    var notProgressingStreak = 0

    while(notProgressingStreak < 2){


      v = g.aggregateMessages[(Int, Long, Float)](
        e => {
          e.sendToDst(if(e.srcAttr._1 == 1 && e.dstAttr._1 == 0) (e.dstAttr._1, e.srcId, r.nextFloat()) else (e.dstAttr._1, e.srcId, -1) );
          e.sendToSrc(if(e.dstAttr._1 == 1 && e.srcAttr._1 == 0) (e.srcAttr._1, e.dstId, r.nextFloat()) else (e.srcAttr._1, e.dstId, -1) )
        },
        (msg1, msg2) => if (msg1._3 > msg2._3) msg1 else msg2
      ) //the unmatched select a matched edge
      g = Graph(v, g.edges)

      v = g.aggregateMessages[(Int, Long, Float)](
        e => {
          e.sendToDst(if(e.srcAttr._3 > 0 && e.srcAttr._2 == e.dstId) (e.dstAttr._1, e.srcId, r.nextFloat()) else e.dstAttr );
          e.sendToSrc(if(e.dstAttr._3 > 0 && e.dstAttr._2 == e.srcId) (e.srcAttr._1, e.dstId, r.nextFloat()) else e.srcAttr )
        },
        (msg1, msg2) => if (msg1._3 > msg2._3) msg1 else msg2
      ) //the matched edge select unmatched
      g = Graph(v, g.edges)

      v = g.aggregateMessages[(Int, Long, Float)](
        e => {
          e.sendToDst(if(e.attr == 1 && (e.srcAttr._3 < 0 || e.dstAttr._3 < 0)) (e.dstAttr._1, e.dstAttr._2, -1) else e.dstAttr );
          e.sendToSrc(if(e.attr == 1 && (e.srcAttr._3 < 0 || e.dstAttr._3 < 0)) (e.srcAttr._1, e.srcAttr._2, -1) else e.srcAttr )
        },
        (msg1, msg2) => if (msg1._3 < msg2._3) msg1 else msg2
      ) //The vertices on matched edges exchange information to see if both are picked
      g = Graph(v, g.edges)

      g = g.mapTriplets(t =>
        if (t.srcAttr._3 > 0 && t.dstAttr._3 > 0 && t.srcAttr._2 == t.dstId && t.dstAttr._2 == t.srcId) 1
        else if (t.srcAttr._3 > 0 && t.dstAttr._3 > 0 && t.attr == 1) 0
        else t.attr
      )
      //Update attributes for vertices based on the new infos of edges

      v = g.aggregateMessages[(Int, Long, Float)](
        e => {
          e.sendToDst((e.attr, 0, 0));
          e.sendToSrc((e.attr, 0, 0))
        },
        (msg1, msg2) => (msg1._1 | msg2._1, 0, 0)
      )
      g = Graph(v, g.edges)
      g.cache()

      iteration += 1
      matchedCount = new_matchedCount
      new_matchedCount = g.edges.filter({case x => (x.attr == 1)}).count()
      println("************************************************************")
      println("Counts = " + iteration + ". number of matches: " + new_matchedCount)
      println("************************************************************")

      if ((new_matchedCount - matchedCount).asInstanceOf[Float] / new_matchedCount.asInstanceOf[Float] < 0.003) {
        notProgressingStreak += 1
      } else {
        notProgressingStreak = 0
      }
    }

    return g.edges.filter({case x => (x.attr == 1)})
  }


  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("project_3")
    conf.setMaster("local[*]")

    val sc = new SparkContext(conf)

    val spark = SparkSession.builder.config(conf).getOrCreate()
    /* You can either use sc or spark */

    if (args.length == 0) {
      println("Error of arguments")
      sys.exit(1)
    }
    else{
      if (args.length != 2) {
        println("Usage: final project graph_path output_path")
        sys.exit(1)
      }
      val startTimeMillis = System.currentTimeMillis()
      val edges = sc.textFile(args(0)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
      val g = Graph.fromEdges[Float, Int](edges, 0F, edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)
      var g2 = LubyMIS1(g)
      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
      println("==================================")
      println("Revised Luby's algorithm completed in " + durationSeconds + "s.")
      println("==================================")

      var g2df = spark.createDataFrame(g2)

      //Without augmenting part, it should be:
      //var g2df = spark.createDataFrame(g2.edges.filter({case (id) => (id.attr == 1)}))

      g2df = g2df.drop(g2df.columns.last)
      g2df.coalesce(1).write.format("csv").mode("overwrite").save(args(1))

    }
  }
}