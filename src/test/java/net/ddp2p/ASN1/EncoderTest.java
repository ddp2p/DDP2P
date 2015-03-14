package net.ddp2p.ASN1;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Test;

public class EncoderTest {

	@Test
	public void testEncoderCalendar() {
		Calendar c = ASN1_Util.CalendargetInstance();
		Encoder e = new Encoder(c);
		Decoder d = new Decoder(e.getBytes());
		Calendar _c = null;
		try {
			_c = d.getGeneralizedTimeCalender_();
		} catch (ASN1DecoderFail e1) {
			e1.printStackTrace();
		}
		assertEquals(c, _c);
	}

	@Test
	public void testSetNull() {
		Encoder e = new Encoder();
		e.setNull();
		Decoder d = new Decoder(e.getBytes());
		Object o = d.getString();
		assertEquals(null, o);
	}

}
