all: script_linux_suid_connect_adhoc script_linux_suid_disconnect_adhoc

script_linux_suid_connect_adhoc: script_linux_suid_connect_adhoc.cpp
	-g++ script_linux_suid_connect_adhoc.cpp -o script_linux_suid_connect_adhoc
	-sudo chown root script_linux_suid_connect_adhoc
	-sudo chown root:root script_linux_suid_connect_adhoc
	-sudo chmod a+s script_linux_suid_connect_adhoc
#	./script_linux_suid_connect_adhoc wlan1 DirectDemocracy 0.0.10

script_linux_suid_disconnect_adhoc: script_linux_suid_disconnect_adhoc.cpp
	g++ script_linux_suid_disconnect_adhoc.cpp -o script_linux_suid_disconnect_adhoc
	-sudo chown root script_linux_suid_disconnect_adhoc
	-sudo chown root:root script_linux_suid_disconnect_adhoc
	-sudo chmod a+s script_linux_suid_disconnect_adhoc
