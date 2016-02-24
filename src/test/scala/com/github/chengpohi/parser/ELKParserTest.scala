package com.github.chengpohi.parser

import java.io.ByteArrayOutputStream

import com.github.chengpohi.ELKRunEngine
import org.scalatest.{BeforeAndAfter, FlatSpec}

/**
 * elasticservice
 * Created by chengpohi on 1/19/16.
 */
class ELKParserTest extends FlatSpec with BeforeAndAfter {
  val outContent = new ByteArrayOutputStream()
  val errContent = new ByteArrayOutputStream()

  before {
    ELKRunEngine.run( """ createIndex "test-parser-name" """)
    outContent.reset()
  }

  "ELKParser" should "get health of elasticsearch" in {
    Console.withOut(outContent) {
      ELKRunEngine.run("health")
    }
    assert(outContent.toString.contains("cluster_name"))
  }

  "ELKParser" should "index doc by indexName, indexType, fields" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """index "test-parser-name" "test-parser-type" { "name" : "hello", "ppp":"fff" }""")

      Thread.sleep(1000)
      //then
      ELKRunEngine.run( """ count "test-parser-name" """)
    }
    assert(outContent.toString.contains("1"))
  }

  "ELKParser" should "reindex by sourceIndex targetIndex sourceType fields" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """index "test-parser-name" "test-parser-type" {"name":"hello"}""")
      Thread.sleep(3000)
      ELKRunEngine.run( """reindex "test-parser-name" "test-parser-name-reindex" "test-parser-type" ["name"]""")

      Thread.sleep(3000)

      ELKRunEngine.run( """ query "test-parser-name-reindex" """)
      ELKRunEngine.run( """ delete "test-parser-name-reindex" """)
      //then
    }
    assert(outContent.toString.contains(
      """
        |"_source":{"name":"hello"}
      """.stripMargin.trim))
  }

  "ELKParser" should "update doc by indexName indexType tuple" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """index "test-parser-name" "test-parser-type" {"name":"hello"} """)
      Thread.sleep(1000)
      ELKRunEngine.run( """update "test-parser-name" "test-parser-type" {"name":"elasticservice"} """)
      Thread.sleep(3000)
      ELKRunEngine.run( """ query "test-parser-name" """)
    }
    assert(outContent.toString.contains(
      """
        |"_source":{"name":"elasticservice"}
      """.stripMargin.trim))
  }

  "ELKParser" should "analysis doc by specific analyzer" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """analysis "standard" "foo,bar"""")
    }
    assert(outContent.toString.contains(
      """"token":"foo","start_offset":0,""".stripMargin
    ))
  }

  "ELKParser" should "delete index type by index type" in {
    ELKRunEngine.run( """index "test-parser-name" "test-parser-type" {"name":"hello"} """)
    ELKRunEngine.run( """delete "test-parser-name" "test-parser-type" """)
    Console.withOut(outContent) {
      ELKRunEngine.run( """"test-parser-name" mapping""")
    }
    assert(!outContent.toString.contains("test-parser-type"))
  }

  "ELKParser" should "index and get doc by id" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """index "test-parser-name" "test-parser-type" {"name":"hello"} "HJJJJJJH" """)
      ELKRunEngine.run( """get "test-parser-name" "test-parser-type" "hJJJJJJH"""")
    }
    assert(outContent.toString.contains( """"_id":"hJJJJJJH""""))
  }

  "ELKParser" should "extract json data" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """index "test-parser-name" "test-parser-type" {"name":"hello"} "HJJJJJJH" """)
      Thread.sleep(1000)
      ELKRunEngine.run( """query "test-parser-name" \\ "name" """)
    }
    assert(outContent.toString.contains( """[{"name":"hello"}]"""))
  }

  "ELKParser" should "set mapping for indexname indextype" in {
    Console.withOut(outContent) {
      ELKRunEngine.run( """mapping "test-mapping" {
                          |  "mappings": {
                          |    "bookmark": {
                          |      "properties": {
                          |        "created_at": {
                          |          "type": "date"
                          |        },
                          |        "name": {
                          |          "type": "string",
                          |          "index": "not_analyzed"
                          |        }
                          |      }
                          |    }
                          |  }
                          |}""".stripMargin('|'))
      Thread.sleep(1000)
      ELKRunEngine.run( """ "test-mapping" mapping """)
    }
    println(outContent.toString)
    assert(outContent.toString.contains( """"created_at":{"type":"date","format":"dateOptionalTime"}"""))
    assert(outContent.toString.contains( """"not_analyzed"""))
    ELKRunEngine.run( """ delete "test-mapping"""")
  }

  "ELKParser" should "bulk index docs" in {
    Console.withOut(outContent) {
      ELKRunEngine.run(
        """bulkIndex "test-parser-name" "test-parser-type" [
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 23}
          |] """.stripMargin)
      Thread.sleep(2000)
      ELKRunEngine.run( """ count "test-parser-name" """)
    }
    assert(outContent.toString.contains("7"))
    assert(outContent.toString.contains("false"))
  }

  "ELKParser" should "index newline" in {
    Console.withOut(outContent) {
      ELKRunEngine.run(
        """bulkIndex "test-parser-name" "test-parser-type" [{"price": 10000, "color": "red"}]""".stripMargin)
      Thread.sleep(2000)
      ELKRunEngine.run( """ count "test-parser-name" """)
    }
    assert(outContent.toString.contains("1"))
  }
  "ELKParser" should "aggs data" in {
    Console.withOut(outContent) {
      ELKRunEngine.run(
        """bulkIndex "test-parser-name" "test-parser-type" [
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 24},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 25},
          |{"name": "hello","age": 25},
          |{"name": "hello","age": 22},
          |{"name": "hello","age": 22}
          |] """.stripMargin)
      Thread.sleep(2000)
      ELKRunEngine.run( """aggsCount "test-parser-name" "test-parser-type" {"ages":{"terms": {"field": "age"}}}""")
    }
    assert(outContent.toString.contains("\"key\":23.0,\"doc_count\":2}"))
  }

    "ELKParser" should "alias index" in {
    Console.withOut(outContent) {
      ELKRunEngine.run(
        """bulkIndex "test-parser-name" "test-parser-type" [
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 24},
          |{"name": "hello","age": 23},
          |{"name": "hello","age": 25},
          |{"name": "hello","age": 25},
          |{"name": "hello","age": 22},
          |{"name": "hello","age": 22}
          |] """.stripMargin)
      Thread.sleep(2000)
      ELKRunEngine.run("""alias "alias-index" "test-parser-name"""")
      Thread.sleep(2000)
      ELKRunEngine.run( """aggsCount "alias-index" "test-parser-type" {"ages":{"terms": {"field": "age"}}}""")
    }
    assert(outContent.toString.contains("\"key\":23.0,\"doc_count\":2}"))
  }

  after {
    ELKRunEngine.run( """ delete "test-parser-name"""")
  }
}
