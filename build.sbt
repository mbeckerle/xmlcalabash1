version := "0.0.1"

organization := "com.xmlcalabash"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "commons-logging" % "commons-logging" % "1.1.+"
    , "commons-codec" % "commons-codec" % "1.6"
    , "commons-io" % "commons-io" % "2.4"
    , "net.sf.saxon" % "Saxon-HE" % "9.6.0-7"
    , "org.apache.httpcomponents" % "httpclient" % "4.5.1"
    , "org.apache.httpcomponents" % "httpcore" % "4.4.4"
    , "org.restlet.jee" % "org.restlet" % "2.3.4"
    , "org.apache.ant" % "ant" % "1.9.6"
    , "org.slf4j" % "slf4j-api" % "1.7.12"
    , "com.thaiopensource" % "jing" % "20091111"
    , "com.nwalsh" % "nwalsh-annotations" % "1.0.0"
    , "com.nwalsh" % "xmlresolver" % "0.12.0"
    , "com.liferay" % "org.apache.commons.fileupload" % "1.2.2.LIFERAY-PATCHED-1"
    , "nu.validator" % "validator" % "15.6.29"
    , "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"
    , "org.restlet.osgi" % "org.restlet.ext.fileupload" % "2.2.2"
    , "com.sun.xml.bind" % "jaxb1-impl" % "2.2.4-1"
    , "org.opengis.cite.saxon" % "saxon9" % "9.0.0.8"
    , "xmlunit" % "xmlunit" % "1.6"
    ,  "com.tunnelvisionlabs" % "xmlcalabash-extension-stubs" % "1.0.0"
    , "org.apache.xmlgraphics" % "fop" % "2.0"
    
)

// retrieve dependencies to lib_managed for convenience when running
// from the command-line
retrieveManaged := true


