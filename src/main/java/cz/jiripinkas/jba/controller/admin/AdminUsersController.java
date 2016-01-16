package cz.jiripinkas.jba.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.UserService;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

	private static final Logger log = LoggerFactory.getLogger(AdminUsersController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private BlogService blogService;

	@RequestMapping
	public String users(Model model) {
		model.addAttribute("users", userService.findAll());
		model.addAttribute("current", "users");
		return "users";
	}

	@RequestMapping("/{id}")
	public String detail(Model model, @PathVariable int id) {
		model.addAttribute("user", userService.findOneWithBlogs(id));
		return "user-detail";
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/remove/{id}", method = RequestMethod.POST)
	public void removeUser(@PathVariable int id) {
		userService.delete(id);
	}

	@RequestMapping(value = "/upload-icon/{id}", method = RequestMethod.POST)
	public View uploadIcon(@RequestParam MultipartFile icon, @PathVariable("id") int blogId) {
		if (!icon.isEmpty()) {
			try {
				blogService.saveIcon(blogId, icon.getBytes());
			} catch (Exception e) {
				log.error("could not upload icon", e);
			}
		} else {
			log.error("could not upload icon");
		}
		RedirectView redirectView = new RedirectView("/blog-form?blogId=" + blogId + "&success=true");
		redirectView.setExposeModelAttributes(false);
		return redirectView;
	}

}
