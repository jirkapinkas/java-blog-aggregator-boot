package cz.jiripinkas.jba.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jiripinkas.jba.atom.Entry;
import cz.jiripinkas.jba.atom.Feed;
import cz.jiripinkas.jba.atom.Link;
import cz.jiripinkas.jba.entity.Blog;
import cz.jiripinkas.jba.entity.Item;
import cz.jiripinkas.jba.exception.RssException;
import cz.jiripinkas.jba.exception.UrlException;
import cz.jiripinkas.jba.rss.TRss;
import cz.jiripinkas.jba.rss.TRssChannel;
import cz.jiripinkas.jba.rss.TRssItem;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RssService {

	private static final Logger log = LoggerFactory.getLogger(RssService.class);

	private static Unmarshaller unmarshallerRss;
	private static Unmarshaller unmarshallerAtom;
	private static DocumentBuilder db;

	@Autowired
	private CloseableHttpClient httpClient;

	static {
		try {
			System.setProperty("com.sun.net.ssl.checkRevocation", "false");
			JAXBContext jaxbContextRss = JAXBContext.newInstance(TRss.class);
			unmarshallerRss = jaxbContextRss.createUnmarshaller();
			JAXBContext jaxbContextAtom = JAXBContext.newInstance(Feed.class);
			unmarshallerAtom = jaxbContextAtom.createUnmarshaller();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Item> getItems(String location, Blog blog, Map<String, Object> allLinksMap) throws RssException {
		return getItems(location, false, blog, allLinksMap);
	}

	/**
	 * This method ensures that the output String has only valid XML unicode
	 * characters as specified by the XML 1.0 standard. For reference, please
	 * see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
	 * standard</a>. This method will return an empty String if the input is
	 * null or empty.
	 *
	 * @param in
	 *            The String whose non-valid characters we want to remove.
	 * @return The in String, stripped of non-valid characters.
	 */
	private String stripNonValidCharacters(String in) {
		StringBuilder out = new StringBuilder(); // Used to hold the output.
		char current; // Used to reference the current character.

		if (in == null || ("".equals(in)))
			return ""; // vacancy test.
		for (int i = 0; i < in.length(); i++) {
			current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught
									// here; it should not happen.
			if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF)) || ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF)))
				out.append(current);
		}
		String outString = fixDate(out.toString());
		return outString.replaceAll("\\s", " ").replace("‘", "'").replace("’", "'").replace("“", "\"").replace("”", "\"").replace("–", "-").replace("‼", "-").replace("&ndash;", "-");
	}

	/**
	 * fix for jsfcentral atom feed, which contains white space in pubDate
	 */
	protected String fixDate(String page) {
		String result = page.replaceAll("<pubDate>(\\s*)(.*)</pubDate>", "<pubDate>$2</pubDate>");
		result = result.replaceAll("\\s*</pubDate>", "</pubDate>");
		return result;
	}

	private HttpGet constructGet(String location) {
		HttpGet get = new HttpGet(location);
		Builder requestConfigBuilder = RequestConfig.custom().setSocketTimeout(100000).setConnectTimeout(100000).setCookieSpec(CookieSpecs.IGNORE_COOKIES);
		get.setConfig(requestConfigBuilder.build());
		get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		get.setHeader("Accept-Encoding", "gzip, deflate, sdch");
		get.setHeader("Accept-Language", "cs-CZ,cs;q=0.8,en;q=0.6,sk;q=0.4,und;q=0.2,pl;q=0.2");
		get.setHeader("Cache-Control", "no-cache");
		get.setHeader("Connection", "keep-alive");
		get.setHeader("Pragma", "no-cache");
		get.setHeader("Upgrade-Insecure-Requests", "1");
		get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
		return get;
	}

	public List<Item> getItems(String location, boolean localFile, Blog blog, Map<String, Object> allLinksMap) throws RssException {
		Node node = null;
		String page = null;

		try {
			if (localFile) {
				page = Files.lines(Paths.get(location))
						.collect(Collectors.joining("\n"))
						.trim();
			} else {
				HttpGet get = constructGet(location);
				CloseableHttpResponse response = null;
				try {
					response = httpClient.execute(get);
					HttpEntity entity = response.getEntity();
					page = EntityUtils.toString(entity, "UTF-8").trim();
				} finally {
					if (response != null) {
						response.close();
					}
				}
			}
			page = stripNonValidCharacters(page);
			if (location.contains("reddit.com")) {
				return getRedditItems(page, blog, allLinksMap);
			}
			Document document = db.parse(new ByteArrayInputStream(page.getBytes(Charset.forName("UTF-8"))));
			node = document.getDocumentElement();
		} catch (Exception ex) {
			log.error("Error parsing XML file: {}", location);
			throw new RssException(ex.getMessage());
		}

		if ("rss".equals(node.getNodeName())) {
			return getRssItems(new StringReader(page), blog, allLinksMap);
		} else if ("feed".equals(node.getNodeName())) {
			return getAtomItems(new StringReader(page), blog, allLinksMap);
		} else {
			throw new RssException("unknown RSS type: " + location);
		}
	}

	protected String getRealLink(String link, HttpClientContext context) throws UrlException {
		link = link.trim();
		link = link.replace("&amp;", "&");
		String realLink = null;
		try {
			HttpGet get = constructGet(link);
			CloseableHttpResponse response = null;
			try {
				response = httpClient.execute(get, context);
				HttpEntity entity = response.getEntity();
				EntityUtils.toString(entity); // consume page
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new UrlException("Link: " + link + " returned: " + response.getStatusLine().getStatusCode());
				}
				if (context.getRedirectLocations() == null) {
					// no redirections performed
					realLink = link;
				} else {
					realLink = context.getRedirectLocations().get(context.getRedirectLocations().size() - 1).toString();
				}
			} finally {
				if (response != null) {
					response.close();
				}
			}
		} catch (Exception e) {
			log.debug("Stacktrace", e);
			log.error("Error downloading real link: {}", link);
			throw new UrlException("Exception during downloading: " + link);
		}
		if (realLink != null) {
			realLink = fixRealLink(realLink);
		}
		return realLink;
	}

	protected String fixRealLink(String realLink) {
		// fixes for stupid blogs
		if (realLink.contains("?utm_campaign=")) {
			realLink = realLink.split("\\?utm_campaign=")[0];
		}
		if (realLink.contains("?utm_medium=")) {
			realLink = realLink.split("\\?utm_medium=")[0];
		}
		if (realLink.contains("?utm_content=")) {
			realLink = realLink.split("\\?utm_content=")[0];
		}
		if (realLink.contains("?utm_source=rss")) {
			realLink = realLink.split("\\?utm_source=rss")[0];
		}
		if (realLink.contains("#tk.rss_all")) {
			realLink = realLink.split("#tk.rss_all")[0];
		}
		realLink = realLink.trim();
		return realLink;
	}

	private List<Item> getRedditItems(String page, Blog blog, Map<String, Object> allLinksMap) throws RssException {
		ArrayList<Item> list = new ArrayList<>();
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readValue(page, JsonNode.class);
			JsonNode items = rootNode.get("data").get("children");
			for(JsonNode item : items) {
					JsonNode data = item.get("data");
					if(data.get("ups").asInt() >= blog.getMinRedditUps()) {
						Item i = new Item();
						i.setLink(getRealLink(data.get("url").asText(), HttpClientContext.create()));
						i.setTitle(cleanTitle(data.get("title").asText()));
						i.setDescription(cleanDescription(data.get("selftext").asText()) + "<a href='https://www.reddit.com" + data.get("permalink").asText() + "'>[comments]</a>");
						i.setPublishedDate(new Date(data.get("created").asLong() * 1000));
						if (allLinksMap.containsKey(i.getLink())) {
							// skip this item, it's already in the database
						} else {
							list.add(i);
						}
					}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RssException(e);
		}
		return list;
	}

	private List<Item> getRssItems(Reader reader, Blog blog, Map<String, Object> allLinksMap) throws RssException {
		ArrayList<Item> list = new ArrayList<>();
		try {
			TRss rss = (TRss) unmarshallerRss.unmarshal(reader);

			List<TRssChannel> channels = rss.getChannels();

			for (TRssChannel channel : channels) {
				List<TRssItem> items = channel.getItems();
				if (items != null) {
					for (TRssItem rssItem : items) {
						Item item = new Item();
						item.setTitle(cleanTitle(rssItem.getTitle()));
						if (rssItem.getDescription() != null) {
							item.setDescription(cleanDescription(rssItem.getDescription().trim()));
						} else if (rssItem.getEncoded() != null) {
							item.setDescription(cleanDescription(rssItem.getEncoded().trim()));
						} else {
							throw new UnsupportedOperationException("unknown description");
						}
						Date pubDate = getRssDate(rssItem.getPubDate());
						item.setPublishedDate(pubDate);
						String link = null;
						if (rssItem.getOrigLink() != null) {
							link = rssItem.getOrigLink();
						} else {
							link = rssItem.getLink();
						}
						if (allLinksMap.containsKey(link)) {
							// skip this item, it's already in the database
							continue;
						}
						try {
							item.setLink(getRealLink(link, HttpClientContext.create()));
						} catch (UrlException e) {
							item.setError(e.getMessage());
						}
						list.add(item);
					}
				}
			}
		} catch (JAXBException | ParseException e) {
			throw new RssException(e);
		}
		return list;
	}

	private List<Item> getAtomItems(Reader reader, Blog blog, Map<String, Object> allLinksMap) throws RssException {
		ArrayList<Item> list = new ArrayList<>();
		try {
			Feed atom = (Feed) unmarshallerAtom.unmarshal(reader);
			List<Entry> entries = atom.getEntries();
			for (Entry entry : entries) {
				Item item = new Item();
				item.setTitle(cleanTitle(entry.getTitle()));
				String summary = entry.getSummary();
				String description = null;
				if (summary != null && !summary.trim().isEmpty()) {
					description = summary;
				} else {
					description = entry.getContent();
				}
				if (description == null) {
					throw new UnsupportedOperationException("unknown description");
				}
				item.setDescription(cleanDescription(description));
				Date pubDate = null;
				if (entry.getPublished() != null) {
					pubDate = entry.getPublished().toGregorianCalendar().getTime();
				} else {
					pubDate = entry.getUpdated().toGregorianCalendar().getTime();
				}
				item.setPublishedDate(pubDate);
				String link = null;
				if (entry.getOrigLink() != null) {
					link = entry.getOrigLink();
				} else {
					if (entry.getLinks().size() == 1) {
						link = entry.getLinks().get(0).getHref();
					} else {
						for (Link atomLink : entry.getLinks()) {
							if ("alternate".equals(atomLink.getRel())) {
								link = atomLink.getHref();
								break;
							}
						}
					}
				}
				if (allLinksMap.containsKey(link)) {
					// skip this item, it's already in the database
					continue;
				}
				try {
					item.setLink(getRealLink(link, HttpClientContext.create()));
				} catch (UrlException e) {
					item.setError(e.getMessage());
				}
				list.add(item);
			}
		} catch (JAXBException e) {
			throw new RssException(e);
		}
		return list;
	}

	public Date getRssDate(String stringDate) throws ParseException {
		stringDate = stringDate.trim();
		try {
			return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(stringDate);
		} catch (ParseException e) {
			try {
				return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).parse(stringDate);
			} catch (ParseException e2) {
				try {
					return new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(stringDate);
				} catch (ParseException e3) {
					try {
						return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(stringDate);
					} catch (ParseException e4) {
						try {
							return new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).parse(stringDate);
						} catch (ParseException e5) {
							try {
								return new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH).parse(stringDate);
							} catch (ParseException e6) {
								try {
									stringDate = stringDate.replace("Sept", "Sep");
									return new SimpleDateFormat("MMM dd, yyyy HH:mm:ss a Z", Locale.ENGLISH).parse(stringDate);
								} catch (ParseException e7) {
									try {
										return new SimpleDateFormat("EEE, MMM dd, yyyy HH:mm:ss a Z", Locale.ENGLISH).parse(stringDate);
									} catch (ParseException e8) {
										try {
											return new SimpleDateFormat("EEE, MMM dd, yyyy HH:mm:ss Z", Locale.ENGLISH).parse(stringDate);
										} catch (ParseException e9) {
											return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(stringDate);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private String cleanXml10(String xml) {
		String xml10pattern = "[^" + "\u0009\r\n" + "\u0020-\uD7FF" + "\uE000-\uFFFD" + "\ud800\udc00-\udbff\udfff" + "]";
		return xml.replaceAll(xml10pattern, "");
	}

	public String cleanTitle(String title) {
		String textTitle = Jsoup.parse(title).text();
		textTitle = textTitle.replace("[OmniFaces utilities 2.0]", "").trim();
		if(textTitle.startsWith("Blog Post: ") && textTitle.length() > 11) {
			textTitle = textTitle.substring(11, textTitle.length());
		}
		textTitle = textTitle.replace("’", "'");
		textTitle = textTitle.replace("“", "\"");
		textTitle = textTitle.replace("”", "\"");
		return cleanXml10(textTitle);
	}

	public String cleanDescription(String description) {
		if(description.contains("Please visit Dilbert.com")) {
			description = "";
		}
		String unescapedDescription = StringEscapeUtils.unescapeHtml(description);
		unescapedDescription = unescapedDescription.replace("<![CDATA[", "").replace("]]>", "");
		unescapedDescription = unescapedDescription.replace("<br />", "BREAK_HERE").replace("<br/>", "BREAK_HERE").replace("<br>", "BREAK_HERE").replace("&lt;br /&gt;", "BREAK_HERE")
				.replace("&lt;br/&gt;", "BREAK_HERE").replace("&lt;br&gt;", "BREAK_HERE");
		String cleanDescription = Jsoup.parse(Jsoup.clean(unescapedDescription, Whitelist.none())).text();
		cleanDescription = cleanDescription.replace("BREAK_HERE", " ");
		
		// fix for Tomcat blog
		cleanDescription = cleanDescription.replace("~", "");
		
		cleanDescription = cleanDescription.replace("... Continue reading", "");
		cleanDescription = cleanDescription.replace("[OmniFaces utilities]", "");
		cleanDescription = cleanDescription.replace("[Note from Pinal]:", "");
		cleanDescription = cleanDescription.replace("[Notes from Pinal]:", "");
		cleanDescription = cleanDescription.replace("[Additional]", "");
		cleanDescription = cleanDescription.replace("RSS from Javabeginnerstutorial.com", "");
		
		// fix for Venkat Subramaniam
		if(cleanDescription.startsWith("Tweet ") && cleanDescription.length() > 6 && Character.isUpperCase(cleanDescription.charAt(6))) {
			cleanDescription = cleanDescription.substring(6, cleanDescription.length());
		}
		if(cleanDescription.startsWith("Tweet") && cleanDescription.length() > 5 && Character.isUpperCase(cleanDescription.charAt(5))) {
			cleanDescription = cleanDescription.substring(5, cleanDescription.length());
		}
		
		if(cleanDescription.startsWith("Preface ") && cleanDescription.length() > 9 && Character.isUpperCase(cleanDescription.charAt(8))) {
			cleanDescription = cleanDescription.substring(8, cleanDescription.length());
		}
		
		if(cleanDescription.startsWith("Share this post: ") && cleanDescription.length() > 18 && Character.isUpperCase(cleanDescription.charAt(17))) {
			cleanDescription = cleanDescription.substring(17, cleanDescription.length());
		}
		if(cleanDescription.startsWith("RSS content ") && cleanDescription.length() > 12 && Character.isUpperCase(cleanDescription.charAt(12))) {
			cleanDescription = cleanDescription.substring(12, cleanDescription.length());
		}
		
		if(cleanDescription.endsWith("...Read More")) {
			cleanDescription = cleanDescription.substring(0, cleanDescription.indexOf("...Read More"));
		}
		if(cleanDescription.endsWith("Read More")) {
			cleanDescription = cleanDescription.substring(0, cleanDescription.indexOf("Read More"));
		}
		
		
		// fix for TL;DR
		if(cleanDescription.startsWith("TLDR ") && cleanDescription.length() > 5) {
			cleanDescription = cleanDescription.substring(5, cleanDescription.length());
		}
		if(cleanDescription.startsWith("TLDR; ") && cleanDescription.length() > 6) {
			cleanDescription = cleanDescription.substring(6, cleanDescription.length());
		}
		if(cleanDescription.startsWith("TLDR: ") && cleanDescription.length() > 6) {
			cleanDescription = cleanDescription.substring(6, cleanDescription.length());
		}
		if(cleanDescription.startsWith("TL;DR ") && cleanDescription.length() > 6) {
			cleanDescription = cleanDescription.substring(6, cleanDescription.length());
		}
		if(cleanDescription.startsWith("TL;DR: ") && cleanDescription.length() > 7) {
			cleanDescription = cleanDescription.substring(7, cleanDescription.length());
		}

		ArrayList<String> links = pullLinks(cleanDescription);
		for (String link : links) {
			cleanDescription = cleanDescription.replace(link, "");
		}

		// split words which are more than 25 characters long
		StringBuilder finalDescription = new StringBuilder(cleanDescription.length());
		int lastSpace = 0;
		for (int i = 0; i < cleanDescription.length(); i++) {
			finalDescription.append(cleanDescription.charAt(i));
			if (cleanDescription.charAt(i) == ' ') {
				lastSpace = 0;
			}
			if (lastSpace == 25) {
				lastSpace = 0;
				finalDescription.append(" ");
			}
			lastSpace++;
		}

		// return only first 140 characters (plus '...')
		String returnDescription = finalDescription.toString();
		returnDescription = returnDescription.replace("’", "'");
		returnDescription = returnDescription.replace("“", "\"");
		returnDescription = returnDescription.replace("”", "\"");
		// this will replace all multiple whitespaces with just single
		// whitespace
		// fix for http://www.tutorial4soft.com/feeds/posts/default?alt=rss
		returnDescription = returnDescription.replaceAll("[^\\x00-\\x7F]", " ").trim();
		returnDescription = returnDescription.trim().replaceAll("\\s+", " ").trim();
		if (returnDescription.length() >= 140) {
			returnDescription = returnDescription.substring(0, 140);
			returnDescription += "...";
		}
		return returnDescription.trim();
	}

	public ArrayList<String> pullLinks(String text) {
		ArrayList<String> links = new ArrayList<>();

		String regex = "\\(?\\b(mailto:|ftp://|http://|https://|www[.])[-A-Za-z0-9+&amp;@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&amp;@#/%=~_()|]";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);
		while (m.find()) {
			String urlStr = m.group();
			if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
				urlStr = urlStr.substring(1, urlStr.length() - 1);
			}
			links.add(urlStr);
		}
		return links;
	}

	public void setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

}
