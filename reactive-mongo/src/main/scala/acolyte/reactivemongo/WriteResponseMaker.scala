package acolyte.reactivemongo

import scala.util.Try

import reactivemongo.bson.BSONDocument
import reactivemongo.core.protocol.Response

/**
 * Creates a write response for given channel ID and result.
 * @tparam T Result type
 */
trait WriteResponseMaker[T] extends ((Int, T) ⇒ Option[Try[Response]]) {
  /**
   * @param channelId ID of Mongo channel
   * @param result Optional result to be wrapped into response
   */
  override def apply(channelId: Int, result: T): Option[Try[Response]]
}

/** Response maker companion. */
object WriteResponseMaker {
  /**
   * {{{
   * import acolyte.reactivemongo.WriteResponseMaker
   *
   * val maker = implicitly[WriteResponseMaker[Boolean]]
   * }}}
   */
  implicit def SuccessWriteResponseMaker = new WriteResponseMaker[Boolean] {
    override def apply(channelId: Int, updatedExisting: Boolean): Option[Try[Response]] = Some(MongoDB.WriteSuccess(channelId, updatedExisting))
  }

  /**
   * Provides response maker for an error.
   *
   * {{{
   * import acolyte.reactivemongo.WriteResponseMaker
   *
   * val maker = implicitly[WriteResponseMaker[(String, Option[Int])]]
   * }}}
   */
  implicit def ErrorWriteResponseMaker = new WriteResponseMaker[(String, Option[Int])] {
    override def apply(channelId: Int, error: (String, Option[Int])): Option[Try[Response]] = Some(MongoDB.WriteError(channelId, error._1, error._2))
  }

  /**
   * Provides response maker for handler not supporting
   * specific write operation.
   *
   * {{{
   * import acolyte.reactivemongo.WriteResponseMaker
   *
   * val maker = implicitly[WriteResponseMaker[None.type]]
   * val response = maker(1, None)
   * }}}
   */
  implicit def UndefinedWriteResponseMaker = new WriteResponseMaker[None.type] {
    /** @return None */
    override def apply(channelId: Int, undefined: None.type): Option[Try[Response]] = None
  }
}
