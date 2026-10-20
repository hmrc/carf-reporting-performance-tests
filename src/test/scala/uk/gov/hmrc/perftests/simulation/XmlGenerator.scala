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

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.time.LocalDate
import javax.xml.stream.{XMLOutputFactory, XMLStreamWriter}

/** Streams schema-valid CARF XML to disk until a target file size is reached. Uses StAX so we never hold the whole document in memory — the generator itself is a demonstration of
  * the streaming property we care about on the parsing side.
  *
  * The generated file always contains:
  *   - a fixed MessageSpec header
  *   - one CARFBody with an Entity RCASP
  *   - as many CryptoUsers (each with 2 RelevantTransactions) as it takes to reach the requested size
  *
  * The CARF schema can be found at: https://www.oecd.org/en/publications/crypto-asset-reporting-framework-xml-schema-july-2025_6e60235b-en/support-materials.html
  */
object XmlGenerator {

  private val CarfNs = "urn:oecd:ties:carf:v1"
  private val StfNs  = "urn:oecd:ties:carfstf:v5"

  def generateXmlFile(targetPath: String, size: String): Unit =
    generate(new File(targetPath), parseSize(size))

  private def parseSize(s: String): Long = {
    val lower       = s.trim.toLowerCase
    val (num, unit) = lower match {
      case x if x.endsWith("gb") => (x.dropRight(2), 1024L * 1024L * 1024L)
      case x if x.endsWith("mb") => (x.dropRight(2), 1024L * 1024L)
      case x if x.endsWith("kb") => (x.dropRight(2), 1024L)
      case x                     => (x, 1L)
    }
    (num.toDouble * unit).toLong
  }

  /** How the generated file is tagged, message-level and doc-level. Defaults produce a plain new-data submission (`CARF701` message with `OECD1` docs everywhere); override to
    * build corrections / deletions / test-data files, e.g. for worst-case perf testing.
    */
  final case class Profile(
    messageTypeIndic: String = "CARF701",
    rcaspDocTypeIndic: String = "OECD1",
    cryptoUserDocTypeIndic: String = "OECD1"
  )
  object Profile {

    /** Every CryptoUsers block deletes an existing entry. */
    val AllDeletions: Profile = Profile(
      messageTypeIndic = "CARF702",
      rcaspDocTypeIndic = "OECD2",
      cryptoUserDocTypeIndic = "OECD3"
    )
  }

  def generate(target: File, targetBytes: Long, profile: Profile = Profile()): Unit = {
    val start = System.nanoTime()
    val out   = new BufferedOutputStream(new FileOutputStream(target), 64 * 1024)
    val w     = XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8")

    try {
      w.writeStartDocument("UTF-8", "1.0")
      nl(w)

      w.setDefaultNamespace(CarfNs)
      w.setPrefix("stf", StfNs)

      w.writeStartElement(CarfNs, "CARF_OECD")
      w.writeDefaultNamespace(CarfNs)
      w.writeNamespace("stf", StfNs)
      w.writeAttribute("version", "1.5")

      writeMessageSpec(w, profile.messageTypeIndic)
      writeBodyOpen(w, profile.rcaspDocTypeIndic)

      w.flush()
      out.flush()

      var counter = 0L
      while (target.length() < targetBytes) {
        counter += 1
        writeCryptoUser(w, counter, profile.cryptoUserDocTypeIndic)
        if ((counter % 200L) == 0L) {
          w.flush()
          out.flush()
        }
      }

      writeBodyClose(w)
      w.writeEndElement() // </CARF_OECD>
      w.writeEndDocument()
      w.flush()
      out.flush()

      val elapsedMs = (System.nanoTime() - start) / 1e6
      val actualMb  = target.length().toDouble / (1024 * 1024)
      println(f"Wrote ${target.getPath} — $actualMb%.2f MB, $counter users, $elapsedMs%.0f ms")
    } finally {
      w.close()
      out.close()
    }
  }

  // --- section writers ---------------------------------------------------

  private def writeMessageSpec(w: XMLStreamWriter, messageTypeIndic: String): Unit = {
    nl(w, "  ")
    w.writeStartElement(CarfNs, "MessageSpec")
    child(w, CarfNs, "SendingEntityIN", "ZMCAR0123456787", "    ")
    child(w, CarfNs, "TransmittingCountry", "GB", "    ")
    child(w, CarfNs, "ReceivingCountry", "US", "    ")
    child(w, CarfNs, "MessageType", "CARF", "    ")
    child(w, CarfNs, "MessageRefId", s"MSG-${LocalDate.now}-gen", "    ")
    child(w, CarfNs, "MessageTypeIndic", messageTypeIndic, "    ")
    child(w, CarfNs, "ReportingPeriod", LocalDate.of(2024, 12, 31).toString, "    ")
    child(w, CarfNs, "Timestamp", "2024-12-31T23:59:59", "    ")
    nl(w, "  ")
    w.writeEndElement()
  }

  private def writeBodyOpen(w: XMLStreamWriter, rcaspDocTypeIndic: String): Unit = {
    nl(w, "  ")
    w.writeStartElement(CarfNs, "CARFBody")
    nl(w, "    ")
    w.writeStartElement(CarfNs, "RCASP")
    nl(w, "      ")
    w.writeStartElement(CarfNs, "RCASP_ID")
    nl(w, "        ")
    w.writeStartElement(CarfNs, "Entity")

    child(w, CarfNs, "ResCountryCode", "GB", "          ")

    // <TIN issuedBy="GB">RCASP-GEN-TIN-001</TIN>
    nl(w, "          ")
    w.writeStartElement(CarfNs, "TIN")
    w.writeAttribute("issuedBy", "GB")
    w.writeCharacters("RCASP-GEN-TIN-001")
    w.writeEndElement()

    child(w, CarfNs, "Name", "Generated Crypto Exchange Ltd", "          ")

    // Address
    nl(w, "          ")
    w.writeStartElement(CarfNs, "Address")
    child(w, CarfNs, "CountryCode", "GB", "            ")
    nl(w, "            ")
    w.writeStartElement(CarfNs, "AddressFix")
    child(w, CarfNs, "City", "London", "              ")
    nl(w, "            ")
    w.writeEndElement() // </AddressFix>
    nl(w, "          ")
    w.writeEndElement() // </Address>

    nl(w, "        ")
    w.writeEndElement() // </Entity>
    nl(w, "      ")
    w.writeEndElement() // </RCASP_ID>

    // DocSpec
    nl(w, "      ")
    w.writeStartElement(CarfNs, "DocSpec")
    child(w, StfNs, "DocTypeIndic", rcaspDocTypeIndic, "        ")
    child(w, StfNs, "DocRefId", "DOC-RCASP-GEN", "        ")
    nl(w, "      ")
    w.writeEndElement() // </DocSpec>

    nl(w, "    ")
    w.writeEndElement() // </RCASP>
  }

  private def writeBodyClose(w: XMLStreamWriter): Unit = {
    nl(w, "  ")
    w.writeEndElement() // </CARFBody>
    nl(w)
  }

  private def writeCryptoUser(w: XMLStreamWriter, n: Long, docTypeIndic: String): Unit = {
    nl(w, "    ")
    w.writeStartElement(CarfNs, "CryptoUsers")

    // UserID > Individual
    nl(w, "      ")
    w.writeStartElement(CarfNs, "UserID")
    nl(w, "        ")
    w.writeStartElement(CarfNs, "Individual")

    child(w, CarfNs, "ResCountryCode", "GB", "          ")

    nl(w, "          ")
    w.writeStartElement(CarfNs, "TIN")
    w.writeAttribute("issuedBy", "GB")
    w.writeCharacters(f"USER-TIN-$n%010d")
    w.writeEndElement()

    // Name (complex NamePerson_Type)
    nl(w, "          ")
    w.writeStartElement(CarfNs, "Name")
    child(w, CarfNs, "FirstName", s"Generated$n", "            ")
    child(w, CarfNs, "LastName", s"Person$n", "            ")
    nl(w, "          ")
    w.writeEndElement() // </Name>

    // Address
    nl(w, "          ")
    w.writeStartElement(CarfNs, "Address")
    child(w, CarfNs, "CountryCode", "GB", "            ")
    nl(w, "            ")
    w.writeStartElement(CarfNs, "AddressFix")
    child(w, CarfNs, "City", "London", "              ")
    nl(w, "            ")
    w.writeEndElement() // </AddressFix>
    nl(w, "          ")
    w.writeEndElement() // </Address>

    // BirthInfo
    nl(w, "          ")
    w.writeStartElement(CarfNs, "BirthInfo")
    child(w, CarfNs, "BirthDate", "1990-01-01", "            ")
    nl(w, "          ")
    w.writeEndElement() // </BirthInfo>

    nl(w, "        ")
    w.writeEndElement() // </Individual>
    nl(w, "      ")
    w.writeEndElement() // </UserID>

    // Two RelevantTransactions per user
    writeRelevantTransaction(w, asset = "BTC", branch = "CryptoToCryptoIn", txCount = 5)
    writeRelevantTransaction(w, asset = "ETH", branch = "CryptoFiatOut", txCount = 3)

    // DocSpec
    nl(w, "      ")
    w.writeStartElement(CarfNs, "DocSpec")
    child(w, StfNs, "DocTypeIndic", docTypeIndic, "        ")
    child(w, StfNs, "DocRefId", f"DOC-USER-$n%010d", "        ")
    nl(w, "      ")
    w.writeEndElement() // </DocSpec>

    nl(w, "    ")
    w.writeEndElement() // </CryptoUsers>
  }

  private def writeRelevantTransaction(
    w: XMLStreamWriter,
    asset: String,
    branch: String,
    txCount: Int
  ): Unit = {
    nl(w, "      ")
    w.writeStartElement(CarfNs, "RelevantTransactions")

    child(w, CarfNs, "CryptoAsset", asset, "        ")

    nl(w, "        ")
    w.writeStartElement(CarfNs, branch)

    child(w, CarfNs, "NumberofTransactions", txCount.toString, "          ")
    nl(w, "          ")
    w.writeStartElement(CarfNs, "Amount")
    w.writeAttribute("currCode", "USD")
    w.writeCharacters("1234.56")
    w.writeEndElement()
    child(w, CarfNs, "NumberofUnits", "0.500000", "          ")

    nl(w, "        ")
    w.writeEndElement() // </branch>

    nl(w, "      ")
    w.writeEndElement() // </RelevantTransactions>
  }

  // --- small XML helpers -------------------------------------------------

  private def child(w: XMLStreamWriter, ns: String, name: String, text: String, indent: String): Unit = {
    nl(w, indent)
    w.writeStartElement(ns, name)
    w.writeCharacters(text)
    w.writeEndElement()
  }

  private def nl(w: XMLStreamWriter, indent: String = ""): Unit =
    w.writeCharacters("\n" + indent)

}
