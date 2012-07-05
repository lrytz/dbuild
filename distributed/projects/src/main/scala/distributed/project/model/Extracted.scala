package distributed
package project
package model

import config.ConfigPrint
import ConfigPrint.makeMember

/** A project dep is an extracted *external* build dependency.  I.e. this is a
 * maven/ivy artifact that exists and is built external to a local build.
 */
case class ProjectRef(
    name: String, 
    organization: String, 
    extension: String = "jar", 
    classifier: Option[String] = None)
object ProjectRef {
  implicit object ProjectDepPrettyPrint extends ConfigPrint[ProjectRef] {
    def apply(t: ProjectRef): String = {
      import t._
      val sb = new StringBuilder("{")
      sb append makeMember("name", name)
      sb append ","
      sb append makeMember("organization", organization)
      sb append ","
      sb append makeMember("ext", extension)
      classifier foreach { o => 
        sb append ","
        sb append makeMember("classifier", o)
      }
      sb append ("}")
      sb.toString
    }   
  }  
  
  object Configured {
    import config._
    def unapply(c: ConfigValue): Option[ProjectRef] = c match {
      // TODO - Handle optional classifier...
      case c: ConfigObject =>
        (c get "name", c get "organization", c get "ext", c get "classifier") match {
          case (ConfigString(name), ConfigString(org), ConfigString(ext), ConfigString(classifier)) =>
            Some(ProjectRef(name, org, ext, Some(classifier)))
          case (ConfigString(name), ConfigString(org), ConfigString(ext), _) => 
            Some(ProjectRef(name, org, ext))
          case _ => None
        }
    case _ => None
  }
  }
}

/** Represents extracted Project information in a build.  A project is akin to a
 * deployed artifact for a given build, and may have dependencies.
 */
case class Project(
    name: String,
    organization: String,
    artifacts: Seq[ProjectRef],
    dependencies: Seq[ProjectRef])
object Project {
  implicit object ProjectPrettyPrint extends ConfigPrint[Project] {
    def apply(t: Project): String = {
      import t._
      val sb = new StringBuilder("{")
      sb append  makeMember("name", name)
      sb append ","
      sb append makeMember("organization", organization)
      sb append ","
      sb append makeMember("artifacts", artifacts)
      sb append ","
      sb append makeMember("dependencies", dependencies)
      sb append ("}")
      sb.toString
    }    
  }
}

/** Represents the *Extracted* metadata of a build.
 */
case class ExtractedBuildMeta(uri: String, projects: Seq[Project]) {
  override def toString = "Build(%s, %s)" format (uri, projects.mkString("\n\t", "\n\t", "\n"))
}
object ExtractedBuildMeta {
  implicit object  BuildPretty extends ConfigPrint[ExtractedBuildMeta] {
    def apply(b: ExtractedBuildMeta): String = {
      import b._
      val sb = new StringBuilder("{")
      sb append makeMember("scm", uri)
      sb append ","
      sb append makeMember("projects", projects)
      sb append ("}")
      sb.toString
    }
  }
}


