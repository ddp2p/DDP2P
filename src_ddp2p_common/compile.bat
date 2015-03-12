del MANIFEST.MF DD.jar jars\DD.jar tools\*.class widgets\private_org\*.class widgets\updates\*.class ^
widgets\dir_management\*.class widgets\dir_fw_terms\*.class widgets\updatesKeys\*.class WSupdate\*.class ^
updates\*.class config\*.class plugin_data\*.class streaming\*.class simulator\*.class handling_wb\*.class ^
hds\*.class util\*.class ASN1\*.class wireless\*.class widgets\peers\*.class widgets\identities\*.class ^
widgets\constituent\*.class widgets\wireless\*.class widgets\components\*.class widgets\org\*.class ^
widgets\motions\*.class widgets\justifications\*.class widgets\news\*.class widgets\directories\*.class ^
table\*.class data\*.class ciphersuits\*.class widgets\census\*.class ^
registration\*.class widgets\*.class widgets\keys\*.class widgets\threads\*.class widgets\instance\*.class ^
widgets\app\*.class util\db\*.class util\email\*.class util\tools\*.class

javac -cp "./jars/javax.mail.jar;./jars/MultiSplit.jar;./jars/sqlite4java.jar;./jars/sqlite-jdbc-3.7.2.jar;jars/icepdf-viewer.jar;jars/icepdf-core.jar;jars/MetaphaseEditor-1.0.0.jar;./" ^
tools\*.java updates\*.java widgets\private_org\*.java widgets\dir_management\*.java widgets\dir_fw_terms\*.java ^
widgets\updates\*.java widgets\updatesKeys\*.java WSupdate\*.java config\*.java plugin_data\*.java ^
streaming\*.java simulator\*.java handling_wb\*.java hds\*.java util\*.java ASN1\*.java wireless\*.java ^
widgets\*.java ^
widgets\peers\*.java widgets\identities\*.java widgets\constituent\*.java widgets\wireless\*.java ^
widgets\components\*.java widgets\org\*.java widgets\motions\*.java widgets\justifications\*.java widgets\news\*.java ^
widgets\directories\*.java table\*.java data\*.java ciphersuits\*.java WSupdate\*.java  updates\*.java widgets\census\*.java ^
widgets\*.java Dos\*.java widgets\keys\*.java widgets\threads\*.java widgets\instance\*.java ^
widgets\app\*.java util\db\*.java util\email\*.java util\tools\*.java


echo Main-Class: config.DD> MANIFEST.MF
@del DD.jar
@del DD_Android.jar

jar cmf MANIFEST.MF DD.jar *.properties ^
p2pdd_resources ^
ASN1\*.class ciphersuits\*.class config\*.class data\*.class handling_wb\*.class hds\*.class MCMC\*.class ^
plugin_data\*.class simulator\*.class streaming\*.class table\*.class  tools\*.class updates\*.class ^
util\*.class wireless\*.class WSupdate\*.class ^
widgets\app\*.class widgets\census\*.class widgets\components\*.class widgets\constituent\*.class ^
widgets\directories\*.class widgets\dir_fw_terms\*.class widgets\dir_management\*.class ^
widgets\identities\*.class widgets\instance\*.class widgets\justifications\*.class ^
widgets\keys\*.class widgets\motions\*.class widgets\news\*.class ^
widgets\org\*.class widgets\peers\*.class widgets\private_org\*.class widgets\threads\*.class widgets\updates\*.class ^
widgets\updatesKeys\*.class widgets\wireless\*.class ^
util\db\*.class util\email\*.class util\tools\*.class

jar cmf MANIFEST.MF DD_Android.jar *.properties ^
p2pdd_resources ^
ASN1\*.class ciphersuits\*.class config\*.class data\*.class handling_wb\*.class hds\*.class MCMC\*.class ^
plugin_data\*.class simulator\*.class streaming\*.class table\*.class  tools\*.class updates\*.class ^
util\*.class wireless\*.class WSupdate\*.class  ^
ASN1\*.java  ciphersuits\*.java  config\*.java  data\*.java  handling_wb\*.java  hds\*.java  MCMC\*.java  ^
plugin_data\*.java  simulator\*.java  streaming\*.java  table\*.java   tools\*.java  updates\*.java  ^
util\*.java  wireless\*.java  WSupdate\*.java

mkdir jars
copy DD.jar jars\
copy DD_Android.jar jars\
copy /b dd_run_stub.bat + DD.jar dd_DD.bat

