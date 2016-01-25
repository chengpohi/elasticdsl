package com.github.chengpohi.parser

/**
 * elasticservice
 * Created by chengpohi on 1/18/16.
 */
class ELKInstrumentParser {

  import fastparse.all._

  val StringChars = NamedFunction(!"\"\\ ".contains(_: Char), "StringChars")

  val strChars = P(CharsWhile(StringChars))
  val space = P(CharsWhile(" \r\n".contains(_)).?)
  val strName = P(CharIn('a' to 'z', 'A' to 'Z'))
  val strParameter: P[String] = P("\"" ~ strChars.rep(1).! ~ "\"")
  val variable: P[String] = P(strName.rep(1)).!.map(s => "$" + s)
  val parameter: P[String] = P(space ~ strParameter ~ ",".? ~ space)
  val strOrVar: P[String] = P(strParameter | variable)

  val status = P("health").map(s => ("health", Some(ELKCommand.h), Seq()))
  val count = P("count" ~ space ~ strOrVar).map(c =>
    ("count", Some(ELKCommand.c), Seq(c)))
  val delete = P("delete" ~ space ~ strOrVar ~ space ~ strOrVar.?).map(c =>
    ("delete", Some(ELKCommand.d), Seq(c._1, c._2.getOrElse("*"))))
  val query = P("query" ~ space ~ strOrVar).map(c =>
    ("query", Some(ELKCommand.q), Seq(c)))
  val reindex = P("reindex" ~ space ~ strOrVar.rep(4, sep = " ")).map(
    c => ("reindex", Some(ELKCommand.r), c))
  val index = P("index" ~ space ~ strOrVar.rep(3, sep = " ")).map(
    c => ("index", Some(ELKCommand.i), c)
  )
  val createIndex = P("createIndex" ~ space ~ strOrVar).map(
    c => ("createIndex", Some(ELKCommand.ci), Seq(c))
  )
  val update = P("update" ~ space ~ strOrVar.rep(3, sep = " ")).map(c =>
    ("query", Some(ELKCommand.u), c))
  val analysis = P("analysis" ~ space ~ strOrVar.rep(2, sep = " ")).map(c =>
    ("analysis", Some(ELKCommand.a), c))
  val functionInstrument = P(strName.rep(1).! ~ "(" ~/ parameter.rep ~ ")").map(f => (f._1, None, f._2))
}