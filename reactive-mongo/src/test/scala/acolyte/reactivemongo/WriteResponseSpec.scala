package acolyte.reactivemongo

import reactivemongo.bson.{ BSONBoolean, BSONInteger, BSONString }

object WriteResponseSpec
    extends org.specs2.mutable.Specification with ResponseMatchers {

  "Write response" title

  "Reponse" should {
    "be made for error message" >> {
      "using generic factory" in {
        WriteResponse("error message #1") aka "prepared" must beLike {
          case prepared ⇒ prepared(1) aka "applied" must beSome.which(
            _ aka "write response" must beWriteError("error message #1"))
        }
      }

      "using named factory" in {
        WriteResponse.failed("error message #2") aka "prepared" must beLike {
          case prepared ⇒ prepared(2) aka "applied" must beSome.which(
            _ aka "write response" must beWriteError("error message #2"))
        }
      }
    }

    "be made for error with code" >> {
      "using generic factory" in {
        WriteResponse("error message #3" -> 9) aka "prepared" must beLike {
          case prepared ⇒ prepared(1) aka "applied" must beSome.which(
            _ aka "write response" must beWriteError(
              "error message #3", Some(9)))
        }
      }

      "using named factory" in {
        WriteResponse.failed("error message #4", 7) aka "prepared" must beLike {
          case prepared ⇒ prepared(2) aka "applied" must beSome.which(
            _ aka "write response" must beWriteError(
              "error message #4", Some(7)))
        }
      }
    }

    "be made for successful result" >> {
      "with a boolean updatedExisting flag" in {
        WriteResponse(true) aka "prepared" must beLike {
          case prepared ⇒ prepared(3) aka "applied" must beSome.which(
            _ aka "write response" must beResponse {
              case ValueDocument(("ok", BSONInteger(1)) ::
                ("updatedExisting", BSONBoolean(true)) :: Nil) :: Nil ⇒ ok
            })
        }
      }

      "with a boolean updatedExisting flag using named factory" in {
        WriteResponse.successful(false) aka "prepared" must beLike {
          case prepared ⇒ prepared(3) aka "applied" must beSome.which(
            _ aka "write response" must beResponse {
              case ValueDocument(("ok", BSONInteger(1)) ::
                ("updatedExisting", BSONBoolean(false)) :: Nil) :: Nil ⇒ ok
            })
        }
      }

      "with a unit (effect)" in {
        WriteResponse() aka "prepared" must beLike {
          case prepared ⇒ prepared(4) aka "applied" must beSome.which(
            _ aka "write response" must beResponse {
              case ValueDocument(("ok", BSONInteger(1)) ::
                ("updatedExisting", BSONBoolean(false)) :: Nil) :: Nil ⇒ ok
            })
        }
      }

      "with a unit (effect) using named factory" in {
        WriteResponse.successful() aka "prepared" must beLike {
          case prepared ⇒ prepared(4) aka "applied" must beSome.which(
            _ aka "write response" must beResponse {
              case ValueDocument(("ok", BSONInteger(1)) ::
                ("updatedExisting", BSONBoolean(false)) :: Nil) :: Nil ⇒ ok
            })
        }
      }
    }

    "be undefined" >> {
      "using generic factory" in {
        WriteResponse(None) aka "prepared" must beLike {
          case prepared ⇒ prepared(1) aka "applied" must beNone
        }
      }

      "using named factory" in {
        WriteResponse.empty aka "prepared" must beLike {
          case prepared ⇒ prepared(2) aka "applied" must beNone
        }
      }
    }
  }
}
