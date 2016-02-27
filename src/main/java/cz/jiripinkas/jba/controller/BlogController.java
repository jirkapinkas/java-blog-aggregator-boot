package cz.jiripinkas.jba.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
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

import cz.jiripinkas.jba.entity.Blog;
import cz.jiripinkas.jba.exception.PageNotFoundException;
import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.ItemService;
import cz.jiripinkas.jba.service.ItemService.MaxType;
import cz.jiripinkas.jba.service.ItemService.OrderType;
import cz.jiripinkas.jba.util.MyUtil;

@Controller
public class BlogController {
	
	private static final Logger log = LoggerFactory.getLogger(BlogController.class);

	@Autowired
	private ItemService itemService;

	@Autowired
	private BlogService blogService;

	@ExceptionHandler
	public void handleBlogNotFound(PageNotFoundException exception, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	private void findBlog(String shortName, Model model) {
		Blog blog = blogService.findByShortName(shortName);
		if (blog == null) {
			log.error("Blog not found {}", shortName);
			throw new PageNotFoundException();
		}
		model.addAttribute("title", "Blog: " + blog.getPublicName());
		model.addAttribute("blogDetail", true);
		model.addAttribute("blogShortName", blog.getShortName());
		model.addAttribute("blog", blog);
	}

	@RequestMapping(value = "/blog/{shortName}")
	public String blogDetail(Model model, HttpServletRequest request, @RequestParam(defaultValue = "0") Integer page, @PathVariable String shortName, @RequestParam(required = false) String orderBy, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to blog: {}, page: {}", shortName, page);
		findBlog(shortName, model);
		return showPage(model, page, shortName, request, orderBy);
	}

	private String showPage(Model model, int page, String shortName, HttpServletRequest request, String orderBy) {
		boolean showAll = false;
		if (request.isUserInRole("ADMIN")) {
			showAll = true;
		}
		OrderType orderType = OrderType.LATEST;
		if (orderBy != null && orderBy.contains("top")) {
			orderType = OrderType.MOST_VIEWED;
		}
		MaxType maxType = MaxType.UNDEFINED;
		if (orderBy != null && orderBy.toLowerCase().contains("month")) {
			maxType = MaxType.MONTH;
		} else if (orderBy != null && orderBy.toLowerCase().contains("week")) {
			maxType = MaxType.WEEK;
		}
		model.addAttribute("items", itemService.getDtoItems(page, showAll, orderType, maxType, null, null, shortName));
		model.addAttribute("nextPage", page + 1);
		return "index";
	}

	@RequestMapping("/blogs")
	public String showBlogs(Model model, HttpServletRequest request, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to blogs list");
		boolean showAll = false;
		if (request.isUserInRole("ADMIN")) {
			showAll = true;
		}
		model.addAttribute("blogs", blogService.findAll(showAll));
		model.addAttribute("current", "blogs");
		return "blogs";
	}
	
	@RequestMapping("/blog/shortname/available")
	@ResponseBody
	public String available(@RequestParam String shortName) {
		Boolean available = blogService.findByShortName(MyUtil.generatePermalink(shortName)) == null;
		return available.toString();
	}

}
