package ccc.controller;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommonController {

	@GetMapping("/main")
	public String dashboard1(Model model, HttpSession session) {
        model.addAttribute("sessionId", session.getId());
		return "main";
	}
}
