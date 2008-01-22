/*
 * Copyright 2007-2008 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package net.liftweb.mapper

import net.liftweb.util.{Helpers, Log, Can, Empty, Full, Failure}
import Helpers._
import scala.testing.SUnit
import net.liftweb.mapper._
import java.sql.{Connection, DriverManager}
import java.io.File

object DBProviders {
    def asList = MySqlProvider :: DerbyProvider :: PostgreSqlProvider :: H2Provider :: H2MemoryProvider :: Nil

    trait Provider {
      def name: String
      def setupDB: unit
    }

    trait FileDbSetup {
      def filePath : String
      def vendor : Vendor

      def setupDB {
        val f = new File(filePath)

        def deleteIt(file: File) {
          if (file.exists) {
            if (file.isDirectory) file.listFiles.foreach{f => deleteIt(f)}
            file.delete
          }
        }

        deleteIt(f)

        DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
      }
    }

    trait DbSetup {
      def vendor : Vendor

      def setupDB {
        DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)

        def deleteAllTables {
          DB.use(DefaultConnectionIdentifier) {
            conn =>
              val md = conn.getMetaData
              val rs = md.getTables(null, Schemifier.getDefaultSchemaName(conn), null, null)
              var toDelete: List[String] = Nil
              while (rs.next) {
                val tableName = rs.getString(3)
                if (rs.getString(4).toLowerCase == "table") toDelete = tableName :: toDelete
              }
              rs.close

              toDelete.foreach {
                table =>
                  try {
                    val ct = "DROP TABLE "+table
                    val st = conn.createStatement
                    st.execute(ct)
                    st.close
                  } catch {
                    case e => e.printStackTrace
                  }
              }

              if (toDelete.length > 0) deleteAllTables
          }
        }
        deleteAllTables
      }
    }

    abstract class Vendor(driverClass : String) extends ConnectionManager {
      def newConnection(name: ConnectionIdentifier): Can[Connection] = {
        Class.forName(driverClass)
        Full(mkConn)
      }

      def releaseConnection(conn: Connection) {
          try {
              conn.close
          } catch {
              case e => Empty //ignore
          }
      }

      def mkConn : Connection
    }


    object MySqlProvider extends Provider with DbSetup {
      def name = "MySql"
      def vendor = new Vendor("com.mysql.jdbc.Driver") {
        def mkConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/lift_test?autoReconnect=true", "dpp", "")
      }
    }

    object PostgreSqlProvider extends Provider with DbSetup {
      def name = "PostgreSql"
      def vendor = new Vendor("org.postgresql.Driver") {
        def mkConn = DriverManager.getConnection("jdbc:postgresql://localhost/lift", "lift", "lift")
      }
    }

    object DerbyProvider extends Provider with FileDbSetup {
      def name = "Derby"
      def filePath = "target/tests_derby_lift"
      def vendor = new Vendor("org.apache.derby.jdbc.EmbeddedDriver") {
        def mkConn = DriverManager.getConnection("jdbc:derby:" + filePath + ";create=true")
      }
    }

    object H2Provider extends Provider with FileDbSetup {
      def name = "H2"
      def filePath = "target/tests_h2_lift"
      def vendor = new Vendor("org.h2.Driver") {
        def mkConn = DriverManager.getConnection("jdbc:h2:" + filePath + "/test.db")
      }
    }

    object H2MemoryProvider extends Provider with DbSetup {
      def name = "H2 in memory"
      def vendor = new Vendor("org.h2.Driver") {
        def mkConn = DriverManager.getConnection("jdbc:h2:mem:lift;DB_CLOSE_DELAY=-1")
      }
    }
}