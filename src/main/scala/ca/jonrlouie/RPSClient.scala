package ca.jonrlouie

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

object RPSClient {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[RPSClient], remote, replies)
}

class RPSClient(remote: InetSocketAddress, listener: ActorRef) extends Actor {
  import Tcp._
  import context.system // implicitly used by IO(Tcp)
 
  val manager = IO(Tcp)
  
  manager ! Connect(remote)
  
  def receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self
 
    case c @ Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) =>
          listener ! data
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! "connection closed"
          context stop self
      }
  }
}