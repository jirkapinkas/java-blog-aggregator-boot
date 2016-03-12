package cz.jiripinkas.jba.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.jiripinkas.jba.entity.Blog;
import cz.jiripinkas.jba.entity.Configuration;
import cz.jiripinkas.jba.entity.NewsItem;
import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.ConfigurationService;
import cz.jiripinkas.jba.service.NewsService;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;

@Controller
public class SitemapController {
	
	@Autowired
	private ConfigurationService configurationService;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private NewsService newsService;
	
	@ResponseBody
	@RequestMapping("/robots.txt")
	public String getRobots() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Sitemap: ");
		stringBuilder.append(configurationService.find().getChannelLink());
		stringBuilder.append("/sitemap.xml");
		stringBuilder.append("\n");
		stringBuilder.append("User-agent: *");
		stringBuilder.append("\n");
		stringBuilder.append("Allow: /");
		stringBuilder.append("\n");
		return stringBuilder.toString();
	}

	@ResponseBody
	@RequestMapping("/sitemap")
	public String getSitemap() {
		Configuration configuration = configurationService.find();
		SitemapGenerator sitemapGenerator = new SitemapGenerator(configuration.getChannelLink());
		sitemapGenerator.addPage(new WebPage().setName(""));
		sitemapGenerator.addPage(new WebPage().setName("blogs"));
		sitemapGenerator.addPage(new WebPage().setName("news"));
		for (Blog blog : blogService.findAll(false)) {
			sitemapGenerator.addPage(new WebPage().setName("blog/" + blog.getShortName()));
		}
		for (NewsItem newsItem : newsService.findAll()) {
			sitemapGenerator.addPage(new WebPage().setName("news/" + newsItem.getShortName()));
		}
		return sitemapGenerator.constructSitemapString();
	}

}
