package cz.jiripinkas.jba.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import cz.jiripinkas.jba.entity.User;
import cz.jiripinkas.jba.service.UserService;

@Controller
@RequestMapping("/register")
public class RegisterController {

	@Autowired
	private UserService userService;

	@ModelAttribute("user")
	public User constructUser() {
		return new User();
	}

	@GetMapping
	public String showRegister(Model model) {
		model.addAttribute("current", "register");
		return "register";
	}

	@PostMapping
	public ModelAndView doRegister(@Valid @ModelAttribute("user") User user, BindingResult result, @RequestParam(required = false) String passCode) {
		if(!"fuckSpam".equals(passCode)) { // simple spam protection
			return new ModelAndView("register");
		}
		if (result.hasErrors()) {
			return new ModelAndView("register");
		}
		userService.save(user);
		RedirectView redirectView = new RedirectView("/register?success=true");
		redirectView.setExposeModelAttributes(false);
		return new ModelAndView(redirectView);
	}
	
	@GetMapping("/available")
	@ResponseBody
	public String available(@RequestParam String username) {
		Boolean available = userService.findOne(username) == null;
		return available.toString();
	}

}
