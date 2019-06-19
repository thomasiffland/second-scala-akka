import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import akkahttp_router._
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.http.client.methods.HttpGet
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




    class MyJsonService extends Directives with SprayJsonSupport with DefaultJsonProtocol {

      val sumHandler:Route =  {
        post {
          entity(as[Array[Int]]) { ints =>
            complete(ints.sum.toString)
          }
        }
      }

      val sumWithNumHandler: Long => Route = (num: Long) => {
        val ints = Seq.fill(num.toInt)(Random.nextInt(100000))
        complete(generateList("http://generate:9090/generate",num.toInt).sum.toString)

      }


      val router = Router(
        route(POST, "sum", sumHandler),
        route(GET, "sum" / LongNumber, sumWithNumHandler),
      )

      val api = router.route

    }

    val bindingFuture = Http().bindAndHandle(new MyJsonService().api, "0.0.0.0", 9092)

  }

  def generateList(url: String, num: Int): List[Int] = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val client = HttpClients.createDefault
    val httpGet = new HttpGet(url+ "/" + num)
    val response = client.execute(httpGet)
    val responseBody = EntityUtils.toString(response.getEntity)
    return mapper.readValue[List[Int]](responseBody)
  }


}





