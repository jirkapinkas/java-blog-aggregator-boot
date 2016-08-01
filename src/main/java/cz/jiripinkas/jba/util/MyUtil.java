package cz.jiripinkas.jba.util;

import java.text.Normalizer;

public final class MyUtil {

	private MyUtil() {
	}

	/**
	 * Generate permalink (without http://www.yourweb.com prefix)
	 * 
	 * @param input
	 *            Name (for example Java 101)
	 * @return Permalink part (for example java-101)
	 */
	public static String generatePermalink(String input) {
		String permalink = input.toLowerCase().trim();
		permalink = java.text.Normalizer.normalize(permalink, Normalizer.Form.NFD);
		permalink = permalink.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		permalink = permalink.replaceAll("[^\\p{Alpha}\\p{Digit}]+", "-");
		permalink = permalink.replaceAll("^-", "");
		permalink = permalink.replaceAll("-$", "");
		return permalink;
	}

	/**
	 * Returns public name in format "nick (name)". 
	 * If nick is empty, return "name".
	 * If nick is too long, trim it (if trimLongNick is true) 
	 * @param nick
	 * @param name
	 * @param trimLongNick
	 * @return
	 */
	public static String getPublicName(String nick, String name, boolean trimLongNick) {
		if (nick == null || nick.trim().isEmpty()) {
			return name;
		}
		if(trimLongNick) {
			if(nick.length() > 17) {
				nick = nick.substring(0, 17) + " ...";
			}
		}
		return nick + " (" + name + ")";
	}

}
