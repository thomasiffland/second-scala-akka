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
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.commons.io.FilenameUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import spray.json.DefaultJsonProtocol

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

      val generate: Long => Route = (num: Long) => {
        val ints = Seq.fill(num.toInt)(Random.nextInt(100000))
        complete(ints)
      }

      val generateSorted: Long => Route = (num: Long) => {
        val ints = Seq.fill(num.toInt)(Random.nextInt(100000))
        complete(sortList("http://sort:9091/sort",ints.toArray))

      }
      val generateSortedReverse: Long => Route = (num: Long) => {
        val ints = Seq.fill(num.toInt)(Random.nextInt(100000))
        complete(sortList("http://sort:9091/sort/reverse",ints.toArray))
      }


      val router = Router(
        route(GET, "generate" / LongNumber, generate),
        route(GET, "generate" / LongNumber / "sorted", generateSorted),
        route(GET, "generate" / LongNumber / "sorted" / "reverse", generateSortedReverse)

      )

      val api = router.route

    }

    val bindingFuture = Http().bindAndHandle(new MyJsonService().api, "0.0.0.0", 9090)

  }

  def sortList(url: String, list: Array[Int]): List[Int] = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val client = HttpClients.createDefault
    val httpPost = new HttpPost(url)
    httpPost.setHeader("content-type", "application/json")
    httpPost.setEntity(new StringEntity(mapper.writeValueAsString(list)))
    val response = client.execute(httpPost)
    val responseBody = EntityUtils.toString(response.getEntity)
    return mapper.readValue[List[Int]](responseBody)
  }



}