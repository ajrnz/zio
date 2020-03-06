package zio.test

import zio._
import zio.test.Assertion.equalTo

object ManagedSpec extends ZIOBaseSpec {

  type Counter = Has[Counter.Service]

  object Counter {

    trait Service {
      def incrementAndGet: UIO[Int]
    }

    val live: ZLayer.NoDeps[Nothing, Counter] =
      ZLayer.fromManaged {
        Ref.make(1).toManaged(_.set(-10)).map { ref =>
          new Counter.Service {
            val incrementAndGet: UIO[Int] = ref.updateAndGet(_ + 1)
          }
        }
      }

    val incrementAndGet: ZIO[Counter, Nothing, Int] =
      ZIO.accessM[Counter](_.get[Counter.Service].incrementAndGet)
  }

  def spec = suite("ManagedSpec")(
    suite("managed shared")(
      testM("first test") {
        assertM(Counter.incrementAndGet)(equalTo(2))
      },
      testM("second test") {
        assertM(Counter.incrementAndGet)(equalTo(3))
      },
      testM("third test") {
        assertM(Counter.incrementAndGet)(equalTo(4))
      }
    ).provideLayerShared(Counter.live),
    suite("managed per test")(
      testM("first test") {
        assertM(Counter.incrementAndGet)(equalTo(2))
      },
      testM("second test") {
        assertM(Counter.incrementAndGet)(equalTo(2))
      }
    ).provideLayer(Counter.live)
  )
}