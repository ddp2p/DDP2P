/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
package net.ddp2p.common.data;
import java.math.BigInteger;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
/**
 WB_FieldExtra :: SEQUENCE {
 can_be_provided_later BOOLEAN,
 certificated BOOLEAN,
 default_val UTF8String,
 entry_size INTEGER,
 label UTF8String,
 list_of_values SEQUENCE OF UTF8String,
 partNeigh INTEGER,
 global_field_extra_ID PrintableString,
 required BOOLEAN,
 tip UTF8String,
 tip_lang PrintableString,
 label_lang PrintableString,
 list_of_values_lang SEQUENCE OF PrintableString,
 default_value_lang PrintableString,
 oid OID
}
 */
