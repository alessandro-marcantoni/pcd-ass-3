package wordcounter

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import wordcounter.GUI.Renderer
import wordcounter.Messages._
import wordcounter.Utils.CorrectParameters

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.ListHasAsScala

object Actors {

  object Discoverer {
    def apply(stripper: ActorRef[Message]): Behavior[Message] = Behaviors.receiveMessage {
      case Directory(d) =>
        d.listFiles() foreach (f => stripper ! Document(f))
        Behaviors.stopped
    }
  }

  object Stripper {
    def apply(counter: ActorRef[Message]): Behavior[Message] = Behaviors.receive((ctx, msg) => msg match {
      case Document(d) =>
        ctx.spawnAnonymous(StripperChild(counter)) ! Document(d)
        Behaviors.same
    }
    )
  }

  object StripperChild {
    def apply(counter: ActorRef[Message]): Behavior[Message] = Behaviors.receiveMessage {
      case Document(d) =>
        counter ! WordList(stripDocument(d))
        Behaviors.stopped
    }

    def stripDocument(document: File): List[String] = {
      val pdDocument: PDDocument = PDDocument.load(document)
      val stripper: PDFTextStripper = new PDFTextStripper()
      stripper.setSortByPosition(true)
      stripper.setStartPage(1)
      stripper.setEndPage(pdDocument.getNumberOfPages)
      List.from(stripper
        .getText(pdDocument)
        .toLowerCase()
        .replaceAll("\\p{Punct}|\\d", "")
        .split("\\s+")
      )
    }
  }

  object Counter {
    def apply(ignoredWordsPath: String, nWords: Int, renderer: ActorRef[Message]): Behavior[Message] = Behaviors.setup(_ => {
      val ignoredWords: List[String] =
        Files.readAllLines(Path.of(ignoredWordsPath), Charset.defaultCharset()).asScala.toList
      var occurrences: Map[String, Int] = Map()
      Behaviors.receiveMessage {
        case WordList(l) => val (alreadyThere, notYet) = l.filter(!ignoredWords.contains(_)).partition(occurrences contains _)
          alreadyThere foreach (w => occurrences = occurrences + (w -> (occurrences(w) + 1)))
          notYet foreach (w => occurrences = occurrences + (w -> 1))
          renderer ! Occurrences(occurrences.toList.sortWith((a, b) => a._2 > b._2).take(nWords))
          Behaviors.same
      }
    })
  }

  /*object Renderer {
    def apply(): Behavior[Message] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case Occurrences(occurrences) =>
          ctx.log.info(occurrences.toString())
          Behaviors.same
      }
    }
  }*/

  object Main {
    def apply(): Behavior[Message] = Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case p: Parameters => p match {
          case CorrectParameters(p) => mainBehavior(ctx, p)
          case _ => Behaviors.stopped
        }
      }
    }

    def mainBehavior(ctx: ActorContext[Message], parameters: Parameters): Behavior[Message] = {
      val renderer: ActorRef[Message] = ctx.spawn(Renderer(), "renderer")
      val counter: ActorRef[Message] = ctx.spawn(Counter(parameters.ignoredWordsPath, parameters.nWords, renderer), "counter")
      val stripper: ActorRef[Message] = ctx.spawn(Stripper(counter), "stripper")
      val discoverer: ActorRef[Message] = ctx.spawn(Discoverer(stripper), "discoverer")
      discoverer ! Directory(parameters.directory)
      Behaviors.empty
    }
  }
}
