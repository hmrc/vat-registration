package services.submission

import models.BuildFailure
import models.api.VatScheme
import play.api.libs.json.JsObject
import utils.JsonUtils.jsonObject

object PeriodsBlockBuilder {

  def buildPeriodsBlock(vatScheme: VatScheme): Either[BuildFailure, JsObject] =
    vatScheme.vatApplication match {
      case Some(vatApplication) =>
        Right(jsonObject("customerPreferredPeriodicity" -> vatApplication.staggerStart))
      case None =>
        Left(BuildFailure("Unable to build submission model due to missing 'vatApplication'"))
    }

}
