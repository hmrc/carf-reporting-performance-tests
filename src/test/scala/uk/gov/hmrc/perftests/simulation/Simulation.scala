/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.perftests.simulation

import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.requests.ReportingRequests._
import uk.gov.hmrc.perftests.requests._

import java.nio.file.{Files, Paths}

class Simulation extends PerformanceTestRunner {

  private val mediumFileSize: String = "150mb"
  private val largeFileSize: String  = "249mb"

  private val mediumFilePath: String = s"target/scala-2.13/test-classes/data/accepted-carf-$mediumFileSize.xml"
  private val largeFilePath: String  = s"target/scala-2.13/test-classes/data/accepted-carf-$largeFileSize.xml"

  XmlGenerator.generateXmlFile(mediumFilePath, mediumFileSize)
  XmlGenerator.generateXmlFile(largeFilePath, largeFileSize)

  after {
    Files.deleteIfExists(Paths.get(mediumFilePath))
    Files.deleteIfExists(Paths.get(largeFilePath))
  }

  setup("SmallFile", "Small file upload").withChainedActions(
    getAuthLoginPage,
    postAuthLoginPage,
    getUploadFilePage,
    postUploadFilePage("accepted-valid-carf-50mb.xml"),
    getUploadIdStatusUrl,
    pollUntilValidated,
    getValidateFileUrl,
    getSaveRcaspAndSubscriptionDetails,
    getCheckYourFileDetailsPage,
    getSendYourFilePage,
    postSendYourFilePage,
    getSendYourFileStatusUrl,
    getStillCheckingYourFileUrl,
    getRefreshStillCheckingYourFileUrl,
    getFilePassedChecksPage,
    getFileConfirmationPage
  )

  setup("MediumFile", "Medium file upload").withChainedActions(
    getAuthLoginPage,
    postAuthLoginPage,
    getUploadFilePage,
    postUploadFilePage(s"accepted-carf-$mediumFileSize.xml"),
    getUploadIdStatusUrl,
    pollUntilValidated,
    getValidateFileUrl,
    getSaveRcaspAndSubscriptionDetails,
    getCheckYourFileDetailsPage,
    getSendYourFilePage,
    postSendYourFilePage,
    getSendYourFileStatusUrl,
    getStillCheckingYourFileUrl,
    getRefreshStillCheckingYourFileUrl,
    getFilePassedChecksPage,
    getFileConfirmationPage
  )

  setup("LargeFile", "Large file upload").withChainedActions(
    getAuthLoginPage,
    postAuthLoginPage,
    getUploadFilePage,
    postUploadFilePage(s"accepted-carf-$largeFileSize.xml"),
    getUploadIdStatusUrl,
    pollUntilValidated,
    getValidateFileUrl,
    getSaveRcaspAndSubscriptionDetails,
    getCheckYourFileDetailsPage,
    getSendYourFilePage,
    postSendYourFilePage,
    getSendYourFileStatusUrl,
    getStillCheckingYourFileUrl,
    getRefreshStillCheckingYourFileUrl,
    getFilePassedChecksPage,
    getFileConfirmationPage
  )

  runSimulation()
}
