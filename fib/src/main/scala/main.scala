import java.io.File
import java.util.UUID.randomUUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import akkahttp_router._
import org.apache.commons.io.FilenameUtils
import spray.json.DefaultJsonProtocol

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatchers.lookup("two-threads-dispatcher")

    def tempDestination(fileInfo: FileInfo): File = {
      val extension = FilenameUtils.getExtension(fileInfo.getFileName)
      File.createTempFile(randomUUID().toString, "." + extension)
    }


    class MyJsonService extends Directives with SprayJsonSupport with DefaultJsonProtocol {


      val fibHandler: Long => Route = (num: Long) => {
        complete(fib(num.toInt).toString)
      }

      def fib(n: Int): Int = n match {
        case 0 | 1 => n
        case _ => fib(n - 1) + fib(n - 2)
      }




      val router = Router(
        route(GET, "fib" / LongNumber, fibHandler),
      )

      val api = router.route

    }

    val bindingFuture = Http().bindAndHandle(new MyJsonService().api, "0.0.0.0", 9093)

  }

}