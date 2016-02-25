package cz.jiripinkas.jba.controller;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import cz.jiripinkas.jba.entity.Blog;
import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.UserService;

@Controller
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private BlogService blogService;

	@ModelAttribute("blog")
	public Blog constructBlog() {
		Blog blog = new Blog();
		blog.setMinRedditUps(0);
		return blog;
	}

	@RequestMapping("/blog-form")
	public String showForm(@RequestParam int blogId, Model model) {
		model.addAttribute("blog", blogService.findOne(blogId));
		return "blog-form";
	}

	@RequestMapping(value = "/blog-form", method = RequestMethod.POST)
	public View editBlog(@RequestParam int blogId, @ModelAttribute Blog blog, Model model, Principal principal, HttpServletRequest request) {
		blog.setId(blogId);
		blogService.update(blog, principal.getName(), request.isUserInRole("ADMIN"));
		RedirectView redirectView = new RedirectView("blog-form?blogId=" + blogId + "&success=true");
		redirectView.setExposeModelAttributes(false);
		return redirectView;
	}

	@RequestMapping("/account")
	public String account(Model model, Principal principal) {
		String name = principal.getName();
		model.addAttribute("user", userService.findOneWithBlogs(name));
		model.addAttribute("current", "account");
		return "account";
	}

	@RequestMapping(value = "/account", method = RequestMethod.POST)
	public ModelAndView doAddBlog(Model model, @Valid @ModelAttribute("blog") Blog blog, BindingResult result, Principal principal) {
		if (result.hasErrors()) {
			return new ModelAndView(account(model, principal));
		}
		String name = principal.getName();
		blogService.save(blog, name);
		RedirectView redirectView = new RedirectView("/account?success=true");
		redirectView.setExposeModelAttributes(false);
		return new ModelAndView(redirectView);
	}

	@RequestMapping(value = "/blog/remove/{id}", method = RequestMethod.POST)
	public View removeBlog(@PathVariable int id) {
		Blog blog = blogService.findOneFetchUser(id);
		blogService.delete(blog);
		RedirectView redirectView = new RedirectView("/account?success=true");
		redirectView.setExposeModelAttributes(false);
		return redirectView;
	}

	@RequestMapping("/blog/available")
	@ResponseBody
	public String available(@RequestParam String url) {
		Boolean available = blogService.findOne(url) == null;
		return available.toString();
	}

}
