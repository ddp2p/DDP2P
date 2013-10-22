import ASN1.*;
public class testASN{
@Test
public void encodeDecodeCalendar() throws ASN1DecoderFail {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        
        Encoder enc = new Encoder(cal);
        Decoder dec = new Decoder(enc.getBytes());
        Calendar res = dec.getFirstObject(true).getGeneralizedTimeCalendar();

        int m1 = cal.get(Calendar.MONTH);
        int m2 = res.get(Calendar.MONTH);
        Assert.assertEquals(m1, m2);
}
    public void main(String[]args){
	encodeDecodeCalendar();
    }
}
