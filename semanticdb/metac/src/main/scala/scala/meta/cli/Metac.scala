package scala.meta.cli

import java.io._
import java.net._
import java.nio.channels._
import java.nio.file._
import scala.meta.internal.semanticdb.scalac._
import scala.tools.nsc.{Main => ScalacMain}

object Metac {
  def process(args: Array[String]): Boolean = {
    val manifestDir = Files.createTempDirectory("semanticdb-scalac_")
    val resourceUrl = classOf[SemanticdbPlugin].getResource("/scalac-plugin.xml")
    val resourceChannel = Channels.newChannel(resourceUrl.openStream())
    val manifestStream = new FileOutputStream(manifestDir.resolve("scalac-plugin.xml").toFile)
    manifestStream.getChannel().transferFrom(resourceChannel, 0, Long.MaxValue)
    manifestStream.close()
    val pluginClasspath = classOf[SemanticdbPlugin].getClassLoader match {
      case null => manifestDir.toString
      case cl: URLClassLoader => cl.getURLs.map(_.getFile).mkString(File.pathSeparator)
      case cl => sys.error(s"unsupported classloader: $cl")
    }
    val semanticdbArgs = Array("-Xplugin:" + pluginClasspath, "-Xplugin-require:semanticdb", "-Yrangepos")
    val stopAfterSemanticdb = Array("-Ystop-after:semanticdb-typer")
    val scalacArgs = args ++ semanticdbArgs ++ stopAfterSemanticdb
    ScalacMain.process(scalacArgs)
    !ScalacMain.reporter.hasErrors
  }

  def main(args: Array[String]): Unit = {
    sys.exit(if (process(args)) 0 else 1)
  }
}
