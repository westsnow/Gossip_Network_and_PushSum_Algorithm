package main.scala

import akka.actor.ActorRef

case class Register(client:ActorRef)
case class AssignGossipAlgm(maxGossipToHear:Int)
case object Gossip
case object StartPushSum

//a worker tell the monitor that it has heard of the gossip
case object GossipACK
case object PushSumDone
case object StartReportActorStatus
case object ReportActorStatus

case class PushSum(value:Double, weight:Double)
case class AssignPushSumAlgm(value:Double, weight:Double, maxSimilarNumToCal:Int)