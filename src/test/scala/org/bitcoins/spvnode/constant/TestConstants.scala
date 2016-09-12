package org.bitcoins.spvnode.constant

import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

/**
  * Created by chris on 9/11/16.
  */
trait TestConstants {

  /** Reads the configuration for the databse specified inside of application.conf */
  private def dbConfig: DatabaseConfig[PostgresDriver] = DatabaseConfig.forConfig("testDatabaseUrl")

  /** The test database used for our spv node */
  def database: Database = dbConfig.db
}

object TestConstants extends TestConstants
