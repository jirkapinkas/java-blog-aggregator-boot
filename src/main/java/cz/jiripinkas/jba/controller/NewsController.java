package cz.jiripinkas.jba.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.jiripinkas.jba.rss.TRss;
import cz.jiripinkas.jba.service.NewsService;

@Controller
@RequestMapping("/news")
public class NewsController {
	
	@Autowired
	private NewsService newsService;

	@RequestMapping
	public String showBlogs(Model model, @RequestParam(defaultValue = "0") int page) {
		model.addAttribute("newsPage", newsService.findBlogs(page));
		model.addAttribute("currPage", page);
		model.addAttribute("current", "news");
		return "news";
	}

	@RequestMapping("/{shortName}")
	public String showDetail(Model model, @PathVariable String shortName) {
		model.addAttribute("news", newsService.findOne(shortName));
		model.addAttribute("current", "news");
		return "news-detail";
	}

	@ResponseBody
	@RequestMapping("/feed")
	public TRss rss() {
		return newsService.getFeed();
	}

}
