package org.nlpcn.jcoder.controller;

import com.alibaba.fastjson.JSONObject;
import org.nlpcn.jcoder.constant.UserConstants;
import org.nlpcn.jcoder.domain.Token;
import org.nlpcn.jcoder.domain.User;
import org.nlpcn.jcoder.filter.IpErrorCountFilter;
import org.nlpcn.jcoder.run.mvc.view.JsonView;
import org.nlpcn.jcoder.service.TokenService;
import org.nlpcn.jcoder.util.ApiException;
import org.nlpcn.jcoder.util.Restful;
import org.nlpcn.jcoder.util.StaticValue;
import org.nlpcn.jcoder.util.StringUtil;
import org.nlpcn.jcoder.util.dao.BasicDao;
import org.nutz.dao.Cnd;
import org.nutz.dao.Condition;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@IocBean
@Filters(@By(type = IpErrorCountFilter.class, args = {"20"}))
@Ok("json")
public class LoginAction {

	private static final Logger LOG = LoggerFactory.getLogger(LoginAction.class);
	private static final String origin = "*";
	private static final String methods = "get, post, put, delete, options";
	private static final String headers = "origin, content-type, accept, authorization";
	private static final String credentials = "true";
	public BasicDao basicDao = StaticValue.systemDao;

	@At("/admin/login")
	public Restful login(HttpServletRequest req, HttpServletResponse resp, @Param("name") String name, @Param("password") String password) throws Throwable {
		JSONObject restful = new JSONObject();
		Condition con = Cnd.where("name", "=", name);
		User user = basicDao.findByCondition(User.class, con);

		if (user != null && user.getPassword().equals(StaticValue.passwordEncoding(password))) {
			restful.put(UserConstants.USER, name);
			restful.put(UserConstants.USER_ID, user.getId());
			restful.put(UserConstants.USER_TYPE, user.getType());

			HttpSession session = Mvcs.getHttpSession();
			session.setAttribute("user", user);

			LOG.info("user " + name + "login ok");

			if (!StaticValue.IS_LOCAL) { //集群模式相互访问使用token
				session.setAttribute("userToken", TokenService.regToken(user));
			}

			return Restful.ok().obj(restful);
		} else {
			int err = IpErrorCountFilter.err();
			LOG.info("user " + name + "login err ,times : " + err);
			return Restful.fail();
		}
	}

	@At("/login/api")
	@Ok("json")
	public Restful loginApi(HttpServletRequest req, HttpServletResponse resp, @Param("name") String name, @Param("password") String password) throws Exception {
		resp.addHeader("Access-Control-Allow-Origin", origin);
		resp.addHeader("Access-Control-Allow-Methods", methods);
		resp.addHeader("Access-Control-Allow-Headers", headers);
		resp.addHeader("Access-Control-Allow-Credentials", credentials);

		int err = IpErrorCountFilter.err();// for client login to many times , 
		Condition con = Cnd.where("name", "=", name);
		User user = basicDao.findByCondition(User.class, con);
		if (user != null && user.getPassword().equals(StaticValue.passwordEncoding(password))) {
			return Restful.instance().obj(TokenService.regToken(user));
		} else {
			LOG.info("user " + name + "login err , times : " + err);
			return Restful.instance(false, "login fail please validate your name or password ,times : " + err, null, ApiException.Unauthorized);
		}
	}

	@At("/loginOut/api")
	@Ok("json")
	public Restful loginOutApi(HttpServletRequest req) throws Exception {
		String token = req.getHeader(UserConstants.USER_TOKEN_HEAD);
		if (StringUtil.isBlank(token)) {
			return Restful.instance(false, "token 'authorization' not in header ");
		} else {
			Token removeToken = TokenService.removeToken(token);
			if (removeToken == null) {
				return Restful.instance(false, "token not in server ");
			} else {
				return Restful.instance(true, removeToken.getUser().getName() + " login out ok");
			}
		}
	}

	@At("/validation/token")
	public void validation(String token) throws Exception {
		Token t = TokenService.getToken(token);
		if (t == null) {
			new JsonView(ApiException.NotFound);
		} else {
			new JsonView();
		}
	}

	@At("/admin/loginOut")
	@Ok("redirect:/login.html")
	public void loginOut() {
		HttpSession session = Mvcs.getHttpSession();
		session.removeAttribute(UserConstants.USER);
		session.removeAttribute(UserConstants.USER_ID);
		session.removeAttribute(UserConstants.USER_TYPE);
		try {
			TokenService.removeToken(String.valueOf(session.getAttribute(UserConstants.USER_TOKEN)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		session.removeAttribute(UserConstants.USER_TOKEN);
	}

}
