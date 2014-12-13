package main.scala;

import scala.collection.mutable.ArrayBuffer;
import akka.actor._;
import akka.actor.Props
import scala.math._
import scala.concurrent.duration._
import scala.util.Random


case object StartWork

class WorkerMonitor(actorNum: Int, MaxGossipToHear: Int, MaxSimilarNumToCal: Int, algm: String, topo: String, actorInterval:Int, failProbability:Double, showlog:Boolean) extends Actor {

  var actorpool = ArrayBuffer[ActorRef]();
  var actorDone = 0;
  var timer: Cancellable = null
  val interval = 1 seconds
  val random = Random
  val pushsumStratedy = "nonstop"
  startActors
  initAlgm(algm)
  buildTopo(topo)
  var starttime: Long = 0

  def receive = {
    case s: String =>
      println(s)
    case GossipACK =>
      actorDone += 1
      if (actorDone == actorpool.size) {
        println("gossip algm finished.")
        terminate
      }
    case PushSumDone=>
//      println("~~~~~~~~~~~~"+sender())
      actorDone += 1
      if (actorDone >= actorpool.size -1) {
        println("pushsum algm finished.")
        terminate
      }
      if( pushsumStratedy == "mostfinish" && actorDone >= 0.9*actorpool.size){
        println("pushsum algm finished.")
        terminate
      }
        
    case StartReportActorStatus =>
      reportActorStatusRepeatly
    case ReportActorStatus =>
      reportActorStatus
    case StartWork =>
      algm match {
        case "gossip"=> startGossip
        case "pushsum"=> startPushsum
        case default => println("I dont understand this algm")
 
    }
    case default =>
      println("Received a message I cant understand")
  }
  def startPushsum = {
    println("now begin pushing sum...")
    starttime = System.currentTimeMillis()
    //actorpool(0) ! AssignPushSumAlgm(0, 1, MaxSimilarNumToCal)
    actorpool(0) ! PushSum(0,1)
  }
  def startGossip = {
    println("now begin gossiping...")
    starttime = System.currentTimeMillis()
    actorpool(0) ! Gossip
  }
  def reportActorStatusRepeatly = {
    val system = context.system
    import system.dispatcher
    timer = context.system.scheduler.schedule(0 milliseconds, interval, self, ReportActorStatus)

  }
  def reportActorStatus = {
    println(actorDone + "/" + actorNum + " actors has done ")
  }

  def terminate = {
    println("It took " + (System.currentTimeMillis - starttime) + "ms to converge")
 
   // println("worker monitor " + context.system)
    context stop self
    context.system.shutdown
  }

  def startActors = {
    for (i <- 0 until actorNum) {
      var actor = context.system.actorOf(Props(classOf[Worker],actorInterval,failProbability, showlog,self), name = "actor" + i)
      actorpool += actor;
    }
    println("actors started")
  }
  def buildTopo(topo: String): Unit = {
    println("building " + topo + " network topo......")
    topo match {
      case "full" => buildFullNetworkTopo
      case "2d" => build2dNetworkTopo
      case "line" => buildLineNetworkTopo
      case "imperfect2d" => buildImperfect2dNetworkTopo
    }
    self ! StartWork
  }
  
  def buildLineNetworkTopo: Unit = {
    val size = actorpool.size
    println("topo size:" + size)
    if (size < 2) {
      println("error:no enough actors")
      return
    }
    actorpool(0) ! Register(actorpool(1))
    actorpool(size - 1) ! Register(actorpool(size - 2))

    for (i <- 1 until size - 1) {
      actorpool(i) ! Register(actorpool(i + 1))
      actorpool(i) ! Register(actorpool(i - 1))
    }

  }
  def buildImperfect2dNetworkTopo = {
    build2dNetworkTopo
    for(actor <- actorpool){
      var num = random.nextInt(actorpool.size)
      while(actor == actorpool(num))
        num = random.nextInt(actorpool.size)
      actor ! Register(actorpool(num))
    }
  }
  def build2dNetworkTopo = {
    val n = sqrt(actorNum).toInt
    for (i <- 0 until actorpool.size) {
      var neighbor:Int = i - n
      if (neighbor >= 0 && neighbor < actorpool.size)
        actorpool(i) ! Register(actorpool(neighbor))
      neighbor = i + n
      if (neighbor >= 0 && neighbor < actorpool.size)
        actorpool(i) ! Register(actorpool(neighbor))
      neighbor = i + 1
      if (neighbor >= 0 && neighbor < actorpool.size && (i+1)/n == i/n)
        actorpool(i) ! Register(actorpool(neighbor))
      neighbor = i - 1
      if (neighbor >= 0 && neighbor < actorpool.size && (i-1)/n == i/n)
        actorpool(i) ! Register(actorpool(neighbor))
    }

  }

  def buildFullNetworkTopo = {
    val actorNumbers = actorpool.size
    for (actor <- actorpool) {
      for (neighbour <- actorpool)
        if (actor != neighbour)
          actor ! Register(neighbour)
    }
  }

  def initAlgm(algm: String) = {
    if (algm equals "gossip") {
      for (actor <- actorpool)
        actor ! AssignGossipAlgm(MaxGossipToHear)
    } else if(algm equals "pushsum"){
      var i:Double = 0.0
      
      for (actor <- actorpool){
        i += 1
        actor ! AssignPushSumAlgm(i, 0, MaxSimilarNumToCal)
      }
    }else{
      println("fatal error")
    }

    println(algm+" algm assigned")
  }

}
