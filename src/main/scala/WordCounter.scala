import Actors.Main
import Messages._
import akka.actor.typed.ActorSystem

import java.io.File

object Messages {
  sealed trait Message
  case class Parameters(directory: File, ignoredWordsPath: String, nWords: Int) extends Message
  case class Directory(directory: File) extends Message
  case class Document(file: File) extends Message
  case class WordList(words: List[String]) extends Message
}

object WordCounter extends App {
  val parameters: Parameters = Parameters(new File("./res/pdfs"), "./res/ignored.txt", 10)
  val system = ActorSystem[Message](Main(), "system")
  system ! parameters
}
