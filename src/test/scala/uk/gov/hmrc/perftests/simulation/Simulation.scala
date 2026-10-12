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

package uk.gov.hmrc.perftests.simulation

import uk.gov.hmrc.performance.simulation.PerformanceTestRunner
import uk.gov.hmrc.perftests.requests.ReportingRequests._
import uk.gov.hmrc.perftests.requests._
class Simulation extends PerformanceTestRunner {

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
    postUploadFilePage("accepted-nil-report.xml"), // TODO: 150mb file not accepted in local, 250mb not accepted in local & staging, large file not accepted in git
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
    postUploadFilePage("accepted-nil-report.xml"), // TODO: 150mb file not accepted in local, 250mb not accepted in local & staging, large file not accepted in git
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
