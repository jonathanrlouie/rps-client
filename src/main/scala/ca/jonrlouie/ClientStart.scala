package ca.jonrlouie

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.ByteString
import java.net.InetSocketAddress
import akka.io.{ IO, Tcp }
import Tcp._

object ClientStart extends App {
  val system = ActorSystem("ClientSystem")
  val listener = system.actorOf(Props[Listener], "listener")
  val client = system.actorOf(RPSClient.props(new InetSocketAddress("localhost", 9001), listener), "client")
  
  while(true){
    val msg = io.StdIn.readLine()
    listener ! msg
  }
  
  class Listener extends Actor {
    def receive = {
      case Connected(remote, local) => 
        println(s"Connection established: ${remote}")
        context become {
          case message: String => client ! ByteString(message)
          case data: ByteString => println(data.decodeString("UTF-8"))
        }
    }
  } 
}