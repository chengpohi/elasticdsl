package com.github.chengpohi.api

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.RichSearchResponse
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse
import org.elasticsearch.cluster.health.ClusterHealthStatus

import scala.concurrent.Future

/**
  * elasticshell
  * Created by chengpohi on 3/12/16.
  */
trait ElasticManagement {
  this: ElasticBase =>
  def nodeStats = {
    buildFuture(client.admin.cluster.prepareNodesStats().all().execute)
  }

  def indicesStats = {
    buildFuture(client.admin.indices().prepareStats().all().execute)
  }

  def clusterStats = client.execute {
    get cluster stats
  }

  def createRepository(repositoryName: String, repositoryType: String, st: Map[String, String]) = client.execute {
    create repository repositoryName `type` repositoryType settings st
  }

  def createSnapshot(snapshotName: String, repositoryName: String) = client.execute {
    create snapshot snapshotName in repositoryName
  }

  def getSnapshotBySnapshotNameAndRepositoryName(snapshotName: String, repositoryName: String) = client.execute {
    get snapshot snapshotName from repositoryName
  }

  def getAllSnapshotByRepositoryName(repositoryName: String) = client.execute {
    get snapshot Seq() from repositoryName
  }

  def deleteSnapshotBySnapshotNameAndRepositoryName(snapshotName: String, repositoryName: String) = client.execute {
    delete snapshot snapshotName in repositoryName
  }

  def mappings(indexName: String, mapping: String) = {
    client.execute {
      create index indexName source mapping
    }
  }

  def getMapping(indexName: String) = client.execute {
    get mapping indexName
  }

  def getIndices: Future[ClusterStateResponse] = {
    buildFuture(client.admin.cluster().prepareState().execute)
  }

  def clusterHealth(): String = {
    val resp = client.execute {
      get cluster health
    }.await
    resp.toString
  }

  def alias(targetIndex: String, sourceIndex: String) = {
    client.execute {
      add alias targetIndex on sourceIndex
    }
  }


  def restoreSnapshot(snapshotName: String, repositoryName: String) = {
    client.execute {
      restore snapshot snapshotName from repositoryName
    }
  }

  def closeIndex(indexName: String) = {
    client.execute {
      close index indexName
    }
  }

  def openIndex(indexName: String) = {
    client.execute {
      open index indexName
    }
  }

  def countCommand(indexName: String): Future[RichSearchResponse] = client.execute {
    search in indexName size 0
  }

  def clusterSettings() = buildFuture(client.admin.cluster().prepareUpdateSettings().execute)

  def nodesSettings() = buildFuture(client.admin.cluster().prepareNodesInfo().execute)

  def indexSettings(indexName: String) = client.execute {
    get settings indexName
  }

  def pendingTasks(): Future[PendingClusterTasksResponse] = buildFuture(client.admin.cluster().preparePendingClusterTasks().execute)

  def waitForStatus(indexName: Option[String] = Some("*"), status: Option[String] = Some("GREEN"), timeOut: Option[String] = Some("100s")): Future[ClusterHealthResponse] = {
    val clusterHealthStatus: ClusterHealthStatus = status match {
      case Some("GREEN") => ClusterHealthStatus.GREEN
      case Some("RED") => ClusterHealthStatus.RED
      case Some("YELLOW") => ClusterHealthStatus.YELLOW
      case _ => ClusterHealthStatus.GREEN
    }
    buildFuture(client.admin.cluster()
      .prepareHealth(indexName.get)
      .setTimeout(timeOut.get)
      .setWaitForStatus(clusterHealthStatus).execute)
  }
}