#!/bin/bash
for a in config/ util/ data/ hds/ hds/identities wireless/ widgets/ ASN1/ ciphersuits/ widgets/directories/ widgets/peers/ widgets/org/ table/
do ./remove_DEBUG.sh $a;
done

