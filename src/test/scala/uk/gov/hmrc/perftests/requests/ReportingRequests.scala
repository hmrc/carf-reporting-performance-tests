/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.perftests.requests

import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import uk.gov.hmrc.performance.conf.ServicesConfiguration

import scala.concurrent.duration.DurationInt

object ReportingRequests extends ServicesConfiguration {

  val baseUrl: String     = baseUrlFor("carf-reporting-frontend")
  val route: String       = "/send-a-cryptoasset-report"
  val baseUrlAuth: String = baseUrlFor("auth-frontend")
  val targetUrlPattern    = """action="(.*?)""""
  val upScanTimer: Int    = 60

  def inputSelectorByName(name: String): Expression[String] = s"input[name='$name']"

  val getAuthLoginPage: HttpRequestBuilder =
    http("Get Auth login page")
      .get(baseUrlAuth + "/auth-login-stub/gg-sign-in")
      .check(status.is(200))

  val postAuthLoginPage: HttpRequestBuilder =
    http("Post Auth login page")
      .post(baseUrlAuth + "/auth-login-stub/gg-sign-in")
      .formParam("authorityId", "")
      .formParam("credentialStrength", "strong")
      .formParam("excludeGnapToken", "false")
      .formParam("confidenceLevel", "50")
      .formParam("credentialRole", "User")
      .formParam("additionalInfo.emailVerified", "N/A")
      .formParam("email", "user@test.com")
      .formParam("affinityGroup", "Organisation")
      .formParam("redirectionUrl", baseUrl + route)
      .formParam("enrolment[0].name", "HMRC-CARF-ORG")
      .formParam("enrolment[0].taxIdentifier[0].name", "CARFID")
      .formParam("enrolment[0].taxIdentifier[0].value", "HG111113")
      .formParam("enrolment[0].state", "Activated")
      .check(status.is(303))
      .check(header("Location").is(baseUrl + route).saveAs("AuthLoginForCarfReporting"))

  val getUploadFilePage: HttpRequestBuilder =
    http("Get Upload file page")
      .get(baseUrl + route + "/upload-file")
      .check(form("#uploadForm").saveAs("CarfUploadForm"))
      .check(saveFileUploadUrl)
      .check(status.is(200))

  def postUploadFilePage(fileName: String): HttpRequestBuilder =
    http("Post Upload file page")
      .post("#{upscanTargetUrl}")
      .asMultipartForm
      .form("#{CarfUploadForm}")
      .bodyPart(RawFileBodyPart("file", s"data/$fileName"))
      .check(status.is(303))
      .check(header("location").saveAs("Status"))

  val pollUntilValidated: List[ActionBuilder] =
    asLongAsDuring(
      session => session("ValidateFile").as[String].contains("status"),
      upScanTimer.seconds
    ) {
      exec(
        http("Poll Status")
          .get("#{Status}")
          .check(status.is(303))
          .check(header("Location").saveAs("ValidateFile"))
          .silent
      ).pause(3.seconds)
    }.actionBuilders

  val getUploadIdStatusUrl: HttpRequestBuilder =
    http("Get Status uploadId url")
      .get("#{Status}")
      .check(status.is(303))
      .check(header("Location").saveAs("ValidateFile"))

  val getValidateFileUrl: HttpRequestBuilder =
    http("Get Validate file url")
      .get(baseUrl + "#{ValidateFile}")
      .check(status.is(303))
      .check(
        header("Location").is(route + "/save-rcasp-and-subscription-details").saveAs("SaveRcaspAndSubscriptionDetails")
      )

  val getSaveRcaspAndSubscriptionDetails: HttpRequestBuilder =
    http("Get Save Rcasp and subscription details url")
      .get(baseUrl + "#{SaveRcaspAndSubscriptionDetails}")
      .check(status.is(303))
      .check(header("Location").is(route + "/check-your-file-details").saveAs("CheckYourFileDetails"))

  val getCheckYourFileDetailsPage: HttpRequestBuilder =
    http("Get Check your file details page")
      .get(baseUrl + "#{CheckYourFileDetails}")
      .check(status.is(200))

  val getSendYourFilePage: HttpRequestBuilder =
    http("Get Send your file page")
      .get(baseUrl + route + "/send-your-file")
      .check(status.is(200))
      .check(css(inputSelectorByName("csrfToken"), "value").saveAs("csrfToken"))

  val postSendYourFilePage: HttpRequestBuilder =
    http("Post Send your file page")
      .post(baseUrl + route + "/send-your-file")
      .formParam("csrfToken", "#{csrfToken}")
      .check(status.is(303))
      .check(header("Location").is(route + "/still-checking-your-file").saveAs("StillCheckingYourFile"))

  val getSendYourFileStatusUrl: HttpRequestBuilder =
    http("Get Send your file status url")
      .get(baseUrl + route + "/send-your-file/status")
      .check(status.in(204, 200))
      .check()

  val getStillCheckingYourFileUrl: HttpRequestBuilder =
    http("Get Still checking your file url")
      .get(baseUrl + "#{StillCheckingYourFile}")
      .check(status.is(200))

  val getRefreshStillCheckingYourFileUrl: ChainBuilder =
    exec(
      http("Get Refresh Still checking your file url")
        .get(baseUrl + "#{StillCheckingYourFile}")
        .check(status.is(303))
    ).pause(8)

  val getFilePassedChecksPage: HttpRequestBuilder =
    http("Get file passed checks page")
      .get(baseUrl + route + "/file-passed-checks")
      .check(
        css("a#continue", "href")
          .transform(url => url.replaceAll("/send-a-cryptoasset-report/file-confirmation/", ""))
          .saveAs("uuid")
      )
      .check(status.is(200))

  val getFileConfirmationPage: HttpRequestBuilder =
    http("Get file confirmation page")
      .get(baseUrl + route + "/file-confirmation/" + "#{uuid}")
      .check(status.is(200))

  def saveFileUploadUrl: CheckBuilder[RegexCheckType, String] =
    regex(_ => targetUrlPattern).saveAs("upscanTargetUrl")

}
