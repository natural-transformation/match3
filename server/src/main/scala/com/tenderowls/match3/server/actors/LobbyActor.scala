package com.tenderowls.match3.server.actors

import akka.typed._
import akka.typed.scaladsl._
import com.tenderowls.match3.{BoardGenerator, Rules}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object LobbyActor {

  def mkId: String = Random.alphanumeric.take(6).mkString

  def apply(timeout: FiniteDuration, animationDuration: FiniteDuration, rules: Rules, maxScore: Int): Behavior[Event] = {
    def matchMaking(pendingPlayers: List[Player]): Behavior[Event] = {
      Actor.immutable[Event] {
        case (ctx, Event.Enter(player)) =>
          pendingPlayers match {
            case Nil =>
              matchMaking(List(player))
            case pendingPlayer :: restPendingPlayers =>
              val board = BoardGenerator.square()(rules)
              val gameBehavior = GameActor(pendingPlayer, player, board, timeout, animationDuration, rules, maxScore)
              ctx.spawn(gameBehavior, s"game-$mkId")
              println(s"restPendingPlayers = $restPendingPlayers")
              matchMaking(restPendingPlayers)
          }
        case (_, Event.Leave(player)) =>
          matchMaking(pendingPlayers.filter(_ != player))
        case _ =>
          Actor.same
      }
    }
    matchMaking(Nil)
  }

  sealed trait Event

  object Event {
    final case class Enter(player: Player) extends Event
    final case class Leave(player: Player) extends Event
  }
}