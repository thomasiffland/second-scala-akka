import java.io.{File, IOException}
import java.util.UUID.randomUUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.ActorMaterializer
import org.apache.commons.io.{FilenameUtils, IOUtils}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.{HttpMultipartMode, MultipartEntityBuilder}
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.http.impl.client.HttpClientBuilder
import spray.json.DefaultJsonProtocol
import akkahttp_router._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives._

import scala.util.Random

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


      val sort: Route = {
        post {
          entity(as[Array[Int]]) { ints =>
            complete(ints.sorted)
          }
        }
      }


      val sortReverse: Route = {
        post {
          entity(as[Array[Int]]) { ints =>
            complete(ints.sorted.reverse)
          }
        }
      }


      val router = Router(
          route(POST, "sort", sort),
          route(POST, "sort" / "reverse", sortReverse)
        )

        val api = router.route

    }

      val bindingFuture = Http().bindAndHandle(new MyJsonService().api, "0.0.0.0", 9091)

    }

    def generateSizeString(jpg: File, percent: String): String = {
      val client = HttpClientBuilder.create.build
      val post = new HttpPost("http://exifdata:8082/exifdata/filtered")
      val fileBody = new FileBody(jpg, ContentType.DEFAULT_BINARY)
      val filterBody = new StringBody("Image Height", ContentType.DEFAULT_BINARY)
      val builder = MultipartEntityBuilder.create
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
      builder.addPart("file", fileBody)
      builder.addPart("filter", filterBody)
      val entity = builder.build
      post.setEntity(entity)
      try {
        val response = client.execute(post)
        val returnValue = new String(IOUtils.toByteArray(response.getEntity.getContent))
        val imageHeight = returnValue.split(":")(1).trim.toFloat
        val newImageHeight = imageHeight * (percent.toFloat / 100f)
        println(newImageHeight)
        return newImageHeight + "x" + newImageHeight
      } catch {
        case e: IOException =>
          e.printStackTrace()
      }
      new String()
    }
  }