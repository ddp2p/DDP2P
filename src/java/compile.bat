del MANIFEST.MF DD.jar jars\DD.jar ^
net\ddp2p\ciphersuits\*.class net\ddp2p\ASN1\*.class ^
net\ddp2p\common\updates\*.class net\ddp2p\common\config\*.class net\ddp2p\common\plugin_data\*.class net\ddp2p\common\streaming\*.class ^
net\ddp2p\common\hds\*.class net\ddp2p\common\util\*.class net\ddp2p\common\wireless\*.class ^
net\ddp2p\common\simulator\*.class net\ddp2p\common\handling_wb\*.class net\ddp2p\common\recommendationTesters\*.class ^
net\ddp2p\common\table\*.class net\ddp2p\common\data\*.class net\ddp2p\common\WSupdate\*.class ^
net\ddp2p\widgets\private_org\*.class net\ddp2p\widgets\updates\*.class ^
net\ddp2p\widgets\dir_management\*.class net\ddp2p\widgets\dir_fw_terms\*.class net\ddp2p\widgets\updatesKeys\*.class ^
net\ddp2p\widgets\constituent\*.class net\ddp2p\widgets\wireless\*.class net\ddp2p\widgets\components\*.class net\ddp2p\widgets\org\*.class ^
net\ddp2p\widgets\motions\*.class net\ddp2p\widgets\justifications\*.class net\ddp2p\widgets\news\*.class net\ddp2p\widgets\directories\*.class ^
net\ddp2p\widgets\*.class net\ddp2p\widgets\keys\*.class net\ddp2p\widgets\threads\*.class net\ddp2p\widgets\instance\*.class ^
net\ddp2p\widgets\peers\*.class net\ddp2p\widgets\identities\*.class net\ddp2p\widgets\app\*.class  net\ddp2p\widgets\census\*.class ^
net\ddp2p\java\db\*.class net\ddp2p\java\email\*.class util\tools\*.class tools\*.class widgets\app\*.class

javac -cp "./jars/javax.mail.jar;./jars/MultiSplit.jar;./jars/sqlite4java.jar;./jars/sqlite-jdbc-3.7.2.jar;jars/icepdf-viewer.jar;jars/icepdf-core.jar;jars/MetaphaseEditor-1.0.0.jar;./" ^
net\ddp2p\ASN1\*.java net\ddp2p\ciphersuits\*.java ^
net\ddp2p\common\WSupdate\*.java net\ddp2p\common\config\*.java net\ddp2p\common\plugin_data\*.java ^
net\ddp2p\common\streaming\*.java net\ddp2p\common\simulator\*.java net\ddp2p\common\handling_wb\*.java net\ddp2p\common\hds\*.java ^
net\ddp2p\common\util\*.java net\ddp2p\common\wireless\*.java net\ddp2p\common\recommendationTesters\*.java net\ddp2p\common\updates\*.java ^
net\ddp2p\common\table\*.java net\ddp2p\common\data\*.java net\ddp2p\common\WSupdate\*.java ^
net\ddp2p\widgets\private_org\*.java net\ddp2p\widgets\dir_management\*.java net\ddp2p\widgets\dir_fw_terms\*.java ^
net\ddp2p\widgets\updates\*.java net\ddp2p\widgets\updatesKeys\*.java ^
net\ddp2p\widgets\directories\*.java net\ddp2p\widgets\census\*.java ^
net\ddp2p\widgets\peers\*.java net\ddp2p\widgets\identities\*.java net\ddp2p\widgets\constituent\*.java net\ddp2p\widgets\wireless\*.java ^
net\ddp2p\widgets\components\*.java net\ddp2p\widgets\org\*.java net\ddp2p\widgets\motions\*.java net\ddp2p\widgets\justifications\*.java ^
net\ddp2p\widgets\news\*.java net\ddp2p\widgets\keys\*.java net\ddp2p\widgets\threads\*.java net\ddp2p\widgets\instance\*.java ^
net\ddp2p\widgets\app\*.java ^
net\ddp2p\java\db\*.java net\ddp2p\java\email\*.java util\tools\*.java tools\*.java widgets\app\*.java


echo Main-Class: net.ddp2p.widgets.app.MainFrame> MANIFEST.MF
@del DD.jar
@del DD_Android.jar

jar cmf MANIFEST.MF DD.jar *.properties ^
p2pdd_resources ^
net\ddp2p\ASN1\*.class net\ddp2p\ciphersuits\*.class net\ddp2p\common\config\*.class net\ddp2p\common\data\*.class ^
net\ddp2p\common\handling_wb\*.class net\ddp2p\common\hds\*.class net\ddp2p\common\MCMC\*.class net\ddp2p\common\recommendationTesters\*.class ^
net\ddp2p\common\plugin_data\*.class net\ddp2p\common\simulator\*.class net\ddp2p\common\streaming\*.class net\ddp2p\common\table\*.class  ^
net\ddp2p\common\updates\*.class ^
net\ddp2p\common\util\*.class net\ddp2p\common\wireless\*.class net\ddp2p\common\WSupdate\*.class ^
net\ddp2p\widgets\app\*.class net\ddp2p\widgets\census\*.class net\ddp2p\widgets\components\*.class net\ddp2p\widgets\constituent\*.class ^
net\ddp2p\widgets\directories\*.class net\ddp2p\widgets\dir_fw_terms\*.class net\ddp2p\widgets\dir_management\*.class ^
net\ddp2p\widgets\identities\*.class net\ddp2p\widgets\instance\*.class net\ddp2p\widgets\justifications\*.class ^
net\ddp2p\widgets\keys\*.class net\ddp2p\widgets\motions\*.class net\ddp2p\widgets\news\*.class ^
net\ddp2p\widgets\org\*.class net\ddp2p\widgets\peers\*.class net\ddp2p\widgets\private_org\*.class net\ddp2p\widgets\threads\*.class net\ddp2p\widgets\updates\*.class ^
net\ddp2p\widgets\updatesKeys\*.class net\ddp2p\widgets\wireless\*.class ^
net\ddp2p\java\db\*.class net\ddp2p\java\email\*.class util\tools\*.class tools\*.class widgets\app\*.java

jar cmf MANIFEST.MF DD_Android.jar *.properties ^
p2pdd_resources ^
net\ddp2p\ASN1\*.class net\ddp2p\ciphersuits\*.class ^
net\ddp2p\common\config\*.class net\ddp2p\common\data\*.class net\ddp2p\common\handling_wb\*.class net\ddp2p\common\hds\*.class net\ddp2p\common\MCMC\*.class ^
net\ddp2p\common\plugin_data\*.class net\ddp2p\common\simulator\*.class net\ddp2p\common\streaming\*.class net\ddp2p\common\table\*.class net\ddp2p\common\updates\*.class ^
net\ddp2p\common\util\*.class net\ddp2p\common\wireless\*.class net\ddp2p\common\WSupdate\*.class  net\ddp2p\common\recommendationTesters\*.class ^
net\ddp2p\ASN1\*.java  net\ddp2p\ciphersuits\*.java  ^
net\ddp2p\common\config\*.java  net\ddp2p\common\data\*.java  net\ddp2p\common\handling_wb\*.java  net\ddp2p\common\hds\*.java  net\ddp2p\common\MCMC\*.java  ^
net\ddp2p\common\plugin_data\*.java  net\ddp2p\common\simulator\*.java net\ddp2p\common\streaming\*.java  net\ddp2p\common\table\*.java net\ddp2p\common\updates\*.java ^
net\ddp2p\common\util\*.java  net\ddp2p\common\wireless\*.java  net\ddp2p\common\WSupdate\*.java  net\ddp2p\common\recommendationTesters\*.java

mkdir jars
copy DD.jar jars\
copy DD_Android.jar jars\
copy /b dd_run_stub.bat + DD.jar dd_DD.bat

