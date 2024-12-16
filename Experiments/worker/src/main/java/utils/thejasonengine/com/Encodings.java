package utils.thejasonengine.com;

import org.apache.commons.text.StringEscapeUtils;

public class Encodings 
{

	public String EscapeString(String toEscape)
	{
		String escaped = StringEscapeUtils.escapeHtml4(toEscape);
		return escaped;
	}
	public String UnescapeString(String toUnescape)
	{
		String unescaped = StringEscapeUtils.unescapeHtml4(toUnescape);
		return unescaped;
	}
}
