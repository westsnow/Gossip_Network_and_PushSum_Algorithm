package main.scala

import akka.actor._
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.duration._

class Worker(actorInterval:Int,pushsumStratedy:String,showlog:Boolean, monitor:ActorRef) extends Actor{
  
  var neighbours = ArrayBuffer[ActorRef]()
  var value:Double = 1.0
  var weight:Double = 0.0
  var ratio:Double = 0
  val MIN = 0.0000000001
  var curRepeatNum = 0
  var maxSimilarNumToCal = 3
  var algm = ""
  var MaxGossipToHear = 0
  var gossipHeard = 0  
  val random = Random
  var pushsumStarted = false
  var timer:Cancellable = null
  val interval = actorInterval.microseconds
//  val interval = 200.milliseconds  
  
  
  case object SendGossipRandomly
  case object SendPushSumRandomly
  

  def addneighbour(actor:ActorRef) = {
    neighbours += actor
  }
  
  def printNeighbours = {
    print(neighbours.size)
    for(tmp <- neighbours){
      print(tmp)
    }
  }
  def receiveGossip = {
    gossipHeard += 1
    if(gossipHeard == 1){
      monitor ! GossipACK
      sendGossipRepeatly
    }
    if( gossipHeard >= MaxGossipToHear){
      terminate      
    }
    //sendGossipRandomly

  }

  def sendGossipRepeatly = {
    val system = context.system
    import system.dispatcher
    timer = context.system.scheduler.schedule(0 milliseconds, interval, self, SendGossipRandomly)
  }
  
  def sendGossipRandomly = {
    val num = random.nextInt(neighbours.size)
 //   println("--------------------------")
    neighbours(num) ! Gossip
//    sendGossipOnScheduel()
  }
  def terminate = {
//    println("I " + self +" have heard " + MaxGossipToHear +" gossips")
    timer.cancel
    context.stop(self)
    //println(context.system)
  }
  def sendPushSumRepeatly={
    val system = context.system
    import system.dispatcher
    timer = context.system.scheduler.schedule(0 milliseconds, interval, self, SendPushSumRandomly)
  }
  
  def sendPushsumRandomly:Unit={
    val num = random.nextInt(neighbours.size)
    value /= 2
    weight /= 2
    ratio = value / weight
    neighbours(num) ! PushSum(value,weight)
  }
  
  def receivePushSum(v:Double,w:Double)={
// println("received pushsum " + v+" "+w)
    value += v
    weight += w
    val tmp = value / weight
//    println(this+ "pre ration:" + ratio + "   --- cur ration " + tmp)
    if( Math.abs(ratio - tmp) < MIN){
      curRepeatNum += 1
      if( curRepeatNum >= maxSimilarNumToCal){
        if(showlog){
        	println("---------------------from " + self + " ratio is " + tmp)
        }
        monitor ! PushSumDone
        if( pushsumStratedy != "nonstop")
          terminate
      }
    }else{
      curRepeatNum = 0
    }

    if(!pushsumStarted){
      sendPushSumRepeatly
      pushsumStarted = true
    }
  }
  
  def receive = {
  	case s:String=>
  	  printNeighbours
  	case SendGossipRandomly=>
  	  sendGossipRandomly
  	case Register(actor:ActorRef)=>
  	  neighbours += actor
  	case AssignGossipAlgm(max:Int)=>
  	  MaxGossipToHear = max
  	case AssignPushSumAlgm(v:Double, w:Double, m:Int)=>
  	  value = v
  	  weight = w
  	  maxSimilarNumToCal = m
  	case Gossip=>
  	  receiveGossip
  	case PushSum(v:Double, w:Double)=>
  	  receivePushSum(v,w)
  	case SendPushSumRandomly=>
  	  sendPushsumRandomly
    }
  
}