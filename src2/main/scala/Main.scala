package main.scala;

import scala.collection.mutable.ArrayBuffer
import akka.actor._
import akka.actor.Props
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {

    
    if(args.length<6) {
      println("parameter error")
      return
    }
    
//topo: imperfect2d,2d,line,full    
//    val actorNum = 8000
//    val MaxGossipToHear = 10
//    val MaxSimilarNumToCal = 10
//    val topo = "full"
//    val algm = "gossip"  
      
    var showlog = false
    var stratedy = "null"
    val actorNum = args(0).toInt
    val MaxGossipToHear = args(1).toInt
    val MaxSimilarNumToCal = args(2).toInt
    val topo = args(3)
    val algm = args(4)    
    val interval = args(5).toInt
    var failProbability = 0.0
    if(args.length >= 7)
      failProbability = args(6).toDouble
    if(args.length>=8){
      if(args(7) == "true"){
        showlog = true 
      }else{
        showlog = false
      }       
    }
       
    val actorsystem = ActorSystem("actorsystem", ConfigFactory.load(ConfigFactory.parseString("""
  akka {
    log-dead-letters = off
  }
  """)))
    
    //println("main system: " + actorsystem)
    
   // val actorsystem = ActorSystem("actorSystem");
//    println(actorsystem)
    val actorMonitor = actorsystem.actorOf(Props(classOf[WorkerMonitor], actorNum,MaxGossipToHear,MaxSimilarNumToCal,algm,topo,interval, failProbability, showlog))
    actorMonitor ! StartReportActorStatus
    
  }
}
