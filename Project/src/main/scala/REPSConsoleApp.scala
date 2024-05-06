import org.json4s.DefaultFormats
import scala.io.StdIn
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._
import java.net.{HttpURLConnection, URL}
// Yipeng Cui, 000826527
// Yuhang Li, 000981323
// Tongyu Li, 000824613
// Main object to handle Renewable Energy Production System (REPS) application
object REPSConsoleApp {

  // Function to validate the format of a given time string using the ISO_ZONED_DATE_TIME formatter
  def checkTimeFormat(time: String): Boolean = {
    try {
      ZonedDateTime.parse(time, DateTimeFormatter.ISO_ZONED_DATE_TIME)
      true
    } catch {
      case _: Exception => false
    }
  }

  // Function to fetch data from a given dataset using an API call
  def fetchData(datasetId: String, startTime: String, endTime: String): List[Map[String, Any]] = {
    val url = s"https://data.fingrid.fi/api/datasets/$datasetId/data?startTime=$startTime&endTime=$endTime&pageSize=10000"

    // Setting up an HTTP connection and adding the API key in the request header
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.setRequestProperty("x-api-key", "8432cb863d604580a2c6d20f3073940e")

    // Reading the response from the API
    val inputStream = connection.getInputStream
    val result = Source.fromInputStream(inputStream).mkString
    inputStream.close()
    connection.disconnect()

    // Parsing the JSON response
    implicit val formats = DefaultFormats
    val json = parse(result)
    val data = (json \ "data").extract[List[Map[String, Any]]]

    // Checking for invalid data
    if (data.isEmpty) {
      println("Warning: The API returned invalid data.")
      Nil
    } else {
      data
    }
  }

  // Function to calculate and print statistics for the provided data
  def calculateStatistics(data: List[Map[String, Any]], stats: List[String]): Unit = {
    val productionNumbers = data.map(_("value") match {
      case bigInt: BigInt => bigInt.toDouble
      case double: Double => double
      case _ => throw new Exception("Unexpected data type for 'value'")
    }).toList

    // Defining the available statistics and corresponding functions
    val statistics = Map[String, List[Double] => Unit](
      "total" -> (numbers => println(s"Total Energy Production: ${numbers.sum}")),
      "mean" -> (numbers => println(s"Mean Energy Production: ${numbers.sum / numbers.length}")),
      "median" -> (numbers => {
        val sorted = numbers.sorted
        val median = if (sorted.length % 2 == 0) (sorted(sorted.length / 2 - 1) + sorted(sorted.length / 2)) / 2.0
        else sorted(sorted.length / 2)
        println(s"Median Energy Production: $median")
      }),
      "mode" -> (numbers => println(s"Mode Energy Production: ${numbers.groupBy(identity).mapValues(_.size).maxBy(_._2)._1}")),
      "range" -> (numbers => println(s"Range of Energy Production: ${numbers.max - numbers.min}"))
    )

    // Calling the corresponding functions for each requested statistic
    stats.foreach(stat => statistics.get(stat).foreach(_(productionNumbers)))
  }

  // Function to write data to a CSV file
  def writeToCSV(data: List[Map[String, Any]], filename: String): Unit = {
    val writer = new java.io.PrintWriter(filename)

    // Writing the header row
    val header = data.head.keys.mkString(",")
    writer.println(header)

    // Writing the data rows
    data.foreach { row =>
      val values = row.values.mkString(",")
      writer.println(values)
    }

    writer.close()
    println(s"Data has been written to $filename")
  }

  // Function to detect and handle any issues in the renewable energy data
  def detectAndHandleIssues(data: List[Map[String, Any]]): Unit = {
    val lowProductionThreshold = 500.0 // Threshold for low energy production
    var issueDetected = false

    // Checking each data row for low production
    data.foreach { row =>
      val value = row("value") match {
        case bigInt: BigInt => bigInt.toDouble
        case double: Double => double
        case _ => throw new Exception("Unexpected data type for 'value'")
      }
      if (value < lowProductionThreshold) {
        println(s"Warning: Low energy output detected at ${row("startTime")}.")
        issueDetected = true
      }
    }

    if (!issueDetected) {
      println("No issues detected in the energy output. Good job!")
    }
  }

  // Function to read data from a file and print its contents
  def readFile(filename: String): Unit = {
    val source = Source.fromFile(filename)
    val lines = source.getLines()

    println(s"Contents of $filename:")
    lines.foreach(println)

    source.close()
  }

  // Main function to handle user interactions
  def main(args: Array[String]): Unit = {
    println("Welcome to the REPS Console Application!")

    // User input for dataset ID and start/end times
    println("Enter the dataset ID (191 for Hydro power production, 246 for Wind power generation, 247 for Solar power generation):")
    val datasetId = StdIn.readLine()

    println("Enter the start time (e.g., 2023-01-01T00:00:00Z):")
    var startTime = StdIn.readLine()
    while (!checkTimeFormat(startTime)) {
      println("Invalid time format. Please enter the start time again (e.g., 2023-01-01T00:00:00Z):")
      startTime = StdIn.readLine()
    }

    println("Enter the end time (e.g., 2023-12-31T23:59:59Z):")
    var endTime = StdIn.readLine()
    while (!checkTimeFormat(endTime)) {
      println("Invalid time format. Please enter the end time again (e.g., 2023-12-31T23:59:59Z):")
      endTime = StdIn.readLine()
    }

    // Fetch data from the API
    val data = fetchData(datasetId, startTime, endTime)

    // User interaction loop
    var continue = true
    while (continue) {
      println("Please select an option:")
      println("1. Perform data analysis")
      println("2. Save data analysis to a file")
      println("3. Detect and handle issues")
      println("4. View displaying data")
      println("5. Exit")

      val choice = StdIn.readInt()

      choice match {
        case 1 =>
          println("Please select the statistics you want to calculate (separated by comma):")
          println("Options: total, mean, median, mode, range")
          val stats = StdIn.readLine().split(",").map(_.trim).toList
          calculateStatistics(data, stats)
        case 2 =>
          println("Enter the filename to save the data analysis (e.g., energy_analysis.csv):")
          val filename = StdIn.readLine()
          writeToCSV(data, filename)
        case 3 =>
          detectAndHandleIssues(data)
        case 4 =>
          println("Enter the filename to read (e.g., energy_analysis.csv):")
          val filename = StdIn.readLine()
          readFile(filename)
        case 5 =>
          println("Exiting the application.")
          continue = false
        case _ =>
          println("Invalid option. Please try again.")
      }
    }
  }
}

