package org.bitcoins.spvnode.constant

import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

/**
  * Created by chris on 9/11/16.
  */
trait TestConstants extends DbConfig {

  /** Reads the configuration for the database specified inside of application.conf */
  def dbConfig: DatabaseConfig[PostgresDriver] = DatabaseConfig.forConfig("unitTestDatabaseUrl")

}

object TestConstants extends TestConstants
