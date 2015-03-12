package util;

interface DD_EmailableAttachment{
	String get_To();
	String get_From();
	String get_Subject();
	String get_FileName();
	byte[] get_ByteContent();
	String get_Greetings();
}