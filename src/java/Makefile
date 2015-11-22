DIRS = scripts
FOLDERS= net/ddp2p/common/population/*.java net/ddp2p/tools/*.java net/ddp2p/common/network/*.java net/ddp2p/common/network/natpmp/*.java net/ddp2p/common/network/pcp/*.java net/ddp2p/common/network/stun/stun/*.java net/ddp2p/common/network/stun/keepalive/*.java  net/ddp2p/common/network/upnp/*.java net/ddp2p/common/network/sim/*.java 
FOLDERC= net/ddp2p/common/population/*.class net/ddp2p/tools/*.class net/ddp2p/common/network/*.class net/ddp2p/common/network/natpmp/*.class net/ddp2p/common/network/pcp/*.class net/ddp2p/common/network/stun/stun/*.class net/ddp2p/common/network/stun/keepalive/*.class  net/ddp2p/common/network/upnp/*.class net/ddp2p/common/network/sim/*.class


all:
	-rm net/ddp2p/*/*.class
	-rm net/ddp2p/common/*/*.class
	-rm net/ddp2p/java/*/*.class
	-rm net/ddp2p/widgets/*/*.class
	-rm */*.class
	-rm util/tools/*.class
	-rm widgets/app/*.class
	javac -source 1.7 -target 1.7 -Xlint:unchecked -cp "jars/MultiSplit.jar:jars/javax.mail.jar:jars/sqlite4java.jar:jars/icepdf-viewer.jar:jars/icepdf-core.jar:jars/MetaphaseEditor-1.0.0.jar:./ " ${FOLDERS}  net/ddp2p/ASN1/*.java  net/ddp2p/ciphersuits/*.java net/ddp2p/common/population/*.java net/ddp2p/tools/*.java net/ddp2p/common/MCMC/*.java net/ddp2p/common/handling_wb/*.java net/ddp2p/common/updates/*.java net/ddp2p/common/plugin_data/*.java net/ddp2p/common/streaming/*.java net/ddp2p/common/data/*.java net/ddp2p/common/table/*.java net/ddp2p/common/util/*.java net/ddp2p/common/hds/*.java net/ddp2p/common/config/*.java net/ddp2p/common/wireless/*.java net/ddp2p/common/simulator/*.java net/ddp2p/java/WSupdate/*.java net/ddp2p/common/recommendationTesters/*.java net/ddp2p/common/examplePlugin/*.java  net/ddp2p/widgets/*/*.java  net/ddp2p/java/*/*.java tools/*.java util/tools/*.java util/db/*.java widgets/app/*.java net/ddp2p/common/network/sim/*.java
	-xgettext -k__ tools/*.java util/tools/*.java net/ddp2p/*/*.java widgets/app/*.java net/ddp2p/widgets/*/*.java net/ddp2p/java/*/*.java net/ddp2p/common/*/*.java net/ddp2p/ciphersuits/*.java net/ddp2p/tools/*.java 
	-rm DD.jar
	echo "Main-Class: net.ddp2p.widgets.app.MainFrame" > MANIFEST.MF

	sudo find ./ -not -name '*.java' -exec touch -t 200012121212.12 \{\} \;

	jar cmf MANIFEST.MF DD.jar ${FOLDERC}  widgets/app/*.class util/tools/*.class tools/*.class net/ddp2p/*/*.class net/ddp2p/common/*/*.class */*.class net/ddp2p/widgets/*/*.class net/ddp2p/java/*/*.class *.properties p2pdd_resources/*.png p2pdd_resources/*.gif p2pdd_resources/*.ico p2pdd_resources/*.jpg p2pdd_resources/steag/* p2pdd_resources/census/*.jpg 

	jar cmf MANIFEST.MF DD_Android.jar ${FOLDERC} ${FOLDERS} net/ddp2p/ASN1/*.class net/ddp2p/ciphersuits/*.class net/ddp2p/common/*/*.class  *.properties \
net/ddp2p/ASN1/*.java  net/ddp2p/ciphersuits/*.java  net/ddp2p/common/config/*.java  net/ddp2p/common/data/*.java net/ddp2p/common/population/*.java net/ddp2p/tools/*.java net/ddp2p/common/handling_wb/*.java  net/ddp2p/common/hds/*.java net/ddp2p/common/MCMC/*.java  net/ddp2p/common/plugin_data/*.java  net/ddp2p/common/simulator/*.java  net/ddp2p/common/streaming/*.java  net/ddp2p/common/table/*.java net/ddp2p/common/updates/*.java  net/ddp2p/common/util/*.java net/ddp2p/common/wireless/*.java  net/ddp2p/common/recommendationTesters/*.java  net/ddp2p/common/examplePlugin/*.java

	jar cmf MANIFEST.MF DD_Android_classes.jar ${FOLDERC} net/ddp2p/ASN1/*.class net/ddp2p/ciphersuits/*.class net/ddp2p/tools/*.class net/ddp2p/common/*/*.class  *.properties

	jar cmf MANIFEST.MF DD_Android_srcs.jar ${FOLDERS}  *.properties \
net/ddp2p/ASN1/*.java  net/ddp2p/ciphersuits/*.java  net/ddp2p/common/config/*.java  net/ddp2p/common/data/*.java  net/ddp2p/common/handling_wb/*.java net/ddp2p/tools/*.java net/ddp2p/common/hds/*.java net/ddp2p/common/population/*.java net/ddp2p/common/MCMC/*.java  net/ddp2p/common/plugin_data/*.java  net/ddp2p/common/simulator/*.java  net/ddp2p/common/streaming/*.java  net/ddp2p/common/table/*.java net/ddp2p/common/updates/*.java  net/ddp2p/common/util/*.java net/ddp2p/common/wireless/*.java  net/ddp2p/common/recommendationTesters/*.java  net/ddp2p/common/examplePlugin/*.java


	#-./unit_test.sh util.updateJARfile DD.jar
	-cat dd_run_stub.sh DD.jar >dd_DD.sh
	-mkdir -p jars
	-cp DD.jar DD_Android.jar jars/
	-chmod +x dd_DD.sh
	-cd scripts; $(MAKE) $(MFLAGS)

translations:
	#msgcat --properties-output messages.po >DebateDecide.properties
	msgcat --properties-output ./translations_path/ro_RO/LC_MESSAGES/DebateDecide.po >DebateDecide_ro.properties
	msgcat --properties-output ./translations_path/ar_SA/LC_MESSAGES/DebateDecide.po >DebateDecide_ar.properties

force_look :
	true
