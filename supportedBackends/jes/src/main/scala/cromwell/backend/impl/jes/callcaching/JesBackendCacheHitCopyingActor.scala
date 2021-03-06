package cromwell.backend.impl.jes.callcaching

import com.google.cloud.storage.contrib.nio.CloudStorageOptions
import cromwell.backend.BackendInitializationData
import cromwell.backend.impl.jes.JesBackendInitializationData
import cromwell.backend.io.JobPaths
import cromwell.backend.standard.callcaching.{StandardCacheHitCopyingActor, StandardCacheHitCopyingActorParams}
import cromwell.core.CallOutputs
import cromwell.core.io.{IoCommand, IoTouchCommand}
import cromwell.core.path.Path
import cromwell.core.simpleton.{WdlValueBuilder, WdlValueSimpleton}
import cromwell.filesystems.gcs.batch.GcsBatchCommandBuilder
import lenthall.util.TryUtil
import wdl4s.wdl.values.WdlFile

import scala.language.postfixOps
import scala.util.Try

class JesBackendCacheHitCopyingActor(standardParams: StandardCacheHitCopyingActorParams) extends StandardCacheHitCopyingActor(standardParams) with GcsBatchCommandBuilder {
  
  private val cachingStrategy = BackendInitializationData
    .as[JesBackendInitializationData](standardParams.backendInitializationDataOption)
    .jesConfiguration.jesAttributes.duplicationStrategy
  
  override def processSimpletons(wdlValueSimpletons: Seq[WdlValueSimpleton], sourceCallRootPath: Path) = cachingStrategy match {
    case CopyCachedOutputs => super.processSimpletons(wdlValueSimpletons, sourceCallRootPath)
    case UseOriginalCachedOutputs =>
      val touchCommands: Seq[Try[IoTouchCommand]] = wdlValueSimpletons collect {
        case WdlValueSimpleton(_, wdlFile: WdlFile) => getPath(wdlFile.value) map touchCommand
      }
      
      TryUtil.sequence(touchCommands) map {
        WdlValueBuilder.toJobOutputs(jobDescriptor.call.task.outputs, wdlValueSimpletons) -> _.toSet
      }
  }
  
  override def processDetritus(sourceJobDetritusFiles: Map[String, String]) = cachingStrategy match {
    case CopyCachedOutputs => super.processDetritus(sourceJobDetritusFiles)
    case UseOriginalCachedOutputs =>
      // apply getPath on each detritus string file
      val detritusAsPaths = detritusFileKeys(sourceJobDetritusFiles).toSeq map { key =>
        key -> getPath(sourceJobDetritusFiles(key))
      } toMap

      // Don't forget to re-add the CallRootPathKey that has been filtered out by detritusFileKeys
      TryUtil.sequenceMap(detritusAsPaths, "Failed to make paths out of job detritus") map { newDetritus =>
        (newDetritus + (JobPaths.CallRootPathKey -> destinationCallRootPath)) -> newDetritus.values.map(touchCommand).toSet
      }
  }

  override protected def additionalIoCommands(sourceCallRootPath: Path,
                                              originalSimpletons: Seq[WdlValueSimpleton],
                                              newOutputs: CallOutputs,
                                              originalDetritus:  Map[String, String],
                                              newDetritus: Map[String, Path]): List[Set[IoCommand[_]]] = {
    val content =
      s"""
         |This directory does not contain any output files because this job matched an identical job that was previously run, thus it was a cache-hit.
         |Cromwell is configured to not copy outputs during call caching. To change this, edit the filesystems.gcs.caching.duplication-strategy field in your backend configuration.
         |The original outputs can be found at this location: ${sourceCallRootPath.pathAsString}
      """.stripMargin

    List(Set(writeCommand(jobPaths.callExecutionRoot / "call_caching_placeholder.txt", content, Seq(CloudStorageOptions.withMimeType("text/plain")))))
  }
}
