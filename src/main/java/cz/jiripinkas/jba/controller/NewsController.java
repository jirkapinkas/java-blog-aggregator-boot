package cz.jiripinkas.jba.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.jiripinkas.jba.entity.NewsItem;
import cz.jiripinkas.jba.exception.PageNotFoundException;
import cz.jiripinkas.jba.service.NewsService;

@Controller
@RequestMapping("/news")
public class NewsController {
	
	private static final Logger log = LoggerFactory.getLogger(NewsController.class);
	
	@Autowired
	private NewsService newsService;

	@ExceptionHandler(PageNotFoundException.class)
	public void pageNotFound(HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	@RequestMapping
	public String showNews(Model model, @RequestParam(defaultValue = "0") int page, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to news list");
		model.addAttribute("newsPage", newsService.findNews(page));
		model.addAttribute("currPage", page);
		model.addAttribute("current", "news");
		return "news";
	}

	@RequestMapping("/{shortName}")
	public String showDetail(Model model, @PathVariable String shortName, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to news: {}", shortName);
		NewsItem newsItem = newsService.findOne(shortName);
		if(newsItem == null) {
			log.error("News not found: {}", shortName);
			throw new PageNotFoundException();
		}
		model.addAttribute("news", newsItem);
		model.addAttribute("current", "news");
		return "news-detail";
	}

	@ResponseBody
	@RequestMapping("/feed.xml")
	public String rss(@RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to rss feed");
		return newsService.getFeed();
	}

}
