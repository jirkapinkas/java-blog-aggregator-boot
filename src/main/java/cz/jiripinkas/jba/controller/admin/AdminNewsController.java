package cz.jiripinkas.jba.controller.admin;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import cz.jiripinkas.jba.entity.NewsItem;
import cz.jiripinkas.jba.service.NewsService;

@Controller
@RequestMapping("/admin/news")
public class AdminNewsController {

	@Autowired
	private NewsService newsService;

	@ModelAttribute
	public NewsItem construct() {
		return new NewsItem();
	}
	
	@RequestMapping("/add")
	public String showAdd(Model model) {
		NewsItem newsItem = new NewsItem();
		newsItem.setDescription("<div class='jumbotron'>\n\n</div>\n");
		model.addAttribute("newsItem", newsItem);
		model.addAttribute("current", "news-form");
		return "news-form";
	}

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public View insert(@Valid @ModelAttribute NewsItem newsItem) {
		newsService.save(newsItem);
		RedirectView redirectView = new RedirectView("../news/add?success=true");
		redirectView.setExposeModelAttributes(false);
		return redirectView;
	}

	@RequestMapping("/edit/{shortName}")
	public String showEdit(Model model, @PathVariable String shortName) {
		model.addAttribute("newsItem", newsService.findOne(shortName));
		return "news-form";
	}

	@RequestMapping(value = "/edit/{shortName}", method = RequestMethod.POST)
	public View edit(@Valid @ModelAttribute NewsItem newsItem) {
		newsService.save(newsItem);
		RedirectView redirectView = new RedirectView(newsItem.getShortName() + "?success=true");
		redirectView.setExposeModelAttributes(false);
		return redirectView;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
	public void delete(@PathVariable int id) {
		newsService.delete(id);
	}

}
