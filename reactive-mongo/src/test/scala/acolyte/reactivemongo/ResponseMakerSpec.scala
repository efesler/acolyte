package acolyte.reactivemongo

import scala.util.Try
import reactivemongo.bson.BSONDocument

object ResponseMakerSpec
    extends org.specs2.mutable.Specification with ResponseMakerFixtures {

  "Response maker" title

  "Response maker" should {
    "be working for Traversable[BSONDocument]" in {
      val makr = implicitly[ResponseMaker[Traversable[BSONDocument]]]

      makr(2, documents) aka "response" must beSome.which { prepared ⇒
        zip(prepared, MongoDB.Success(2, documents)).
          aka("maker") must beSuccessfulTry.like {
            case (a, b) ⇒ a.documents aka "response" must_== b.documents
          }
      }
    }

    "be working for an error message (String)" in {
      val makr = implicitly[ResponseMaker[String]]

      makr(3, "Custom error") aka "response" must beSome.which { prepared ⇒
        zip(prepared, MongoDB.Error(3, "Custom error")).
          aka("maker") must beSuccessfulTry.like {
            case (a, b) ⇒ a.documents aka "response" must_== b.documents
          }
      }
    }

    shapeless.test.illTyped("implicitly[ResponseMaker[Any]]")
  }

  @inline def zip[A, B](a: Try[A], b: Try[B]): Try[(A, B)] =
    for { x ← a; y ← b } yield (x -> y)
}

sealed trait ResponseMakerFixtures {
  val documents = Seq(
    BSONDocument("prop1" -> "str", "propB" -> 1),
    BSONDocument("propB" -> 3, "prop1" -> "text"))
}
